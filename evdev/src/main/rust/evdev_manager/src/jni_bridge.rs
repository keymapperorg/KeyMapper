use crate::bindings;
use crate::device_manager::DeviceContext;
use crate::evdev::{EvdevDevice, EvdevError};
use crate::evdevcallback_binder_observer::EvdevCallbackBinderObserver;
use crate::event_loop;
use crate::observer::EvdevEventNotifier;
use jni::objects::{GlobalRef, JClass, JObject, JString};
use jni::sys::{jboolean, jint, jobject, jobjectArray, jstring};
use jni::JNIEnv;
use nix::dir::Dir;
use nix::fcntl::OFlag;
use nix::sys::stat::Mode;
use std::ffi::CString;
use std::os::fd::AsRawFd;
use std::ptr;
use std::sync::{Arc, Mutex, OnceLock};

fn init_logger() {
    static LOGGER_INIT: OnceLock<()> = OnceLock::new();

    LOGGER_INIT.get_or_init(|| {
        android_log::init("KeyMapperSystemBridge").unwrap_or_else(|e| {
            eprintln!("Failed to initialize Android logger: {:?}", e);
        });
    });
}

/// Global event notifier
static EVENT_NOTIFIER: OnceLock<Arc<EvdevEventNotifier>> = OnceLock::new();

/// Global binder observer
static BINDER_OBSERVER: OnceLock<Arc<EvdevCallbackBinderObserver>> = OnceLock::new();

/// Get or create the event notifier
fn get_event_notifier() -> Arc<EvdevEventNotifier> {
    EVENT_NOTIFIER
        .get_or_init(|| Arc::new(EvdevEventNotifier::new()))
        .clone()
}

/// Get or create the binder observer
fn get_binder_observer() -> Arc<EvdevCallbackBinderObserver> {
    BINDER_OBSERVER
        .get_or_init(|| Arc::new(EvdevCallbackBinderObserver::new()))
        .clone()
}

/// Create a Java EvdevDeviceHandle object
fn create_evdev_device_handle(
    env: &mut JNIEnv,
    path: &str,
    name: &str,
    bus: i32,
    vendor: i32,
    product: i32,
) -> Result<jobject, jni::errors::Error> {
    let class = env.find_class("io/github/sds100/keymapper/common/models/EvdevDeviceHandle")?;
    let path_str = env.new_string(path)?;
    let name_str = env.new_string(name)?;

    let obj = env.new_object(
        class,
        "(Ljava/lang/String;Ljava/lang/String;III)V",
        &[
            jni::objects::JValue::Object(&path_str.into()),
            jni::objects::JValue::Object(&name_str.into()),
            jni::objects::JValue::Int(bus),
            jni::objects::JValue::Int(vendor),
            jni::objects::JValue::Int(product),
        ],
    )?;

    Ok(obj.into_raw())
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_grabEvdevDeviceNative(
    mut env: JNIEnv,
    _class: JClass,
    j_device_path: JString,
) -> jboolean {
    init_logger();

    let device_path = match env.get_string(&j_device_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(e) => {
            error!("Failed to get device path string: {:?}", e);
            return false as jboolean;
        }
    };

    // Check if device is already grabbed
    if event_loop::is_device_grabbed(&device_path) {
        warn!("Device {} is already grabbed", device_path);
        return false as jboolean;
    }

    // Never grab uinput devices
    if device_path.contains("uinput") {
        warn!("Cannot grab uinput device: {}", device_path);
        return false as jboolean;
    }

    // Grab the device
    let device = match DeviceContext::grab_device(&device_path) {
        Ok(d) => Arc::new(d),
        Err(e) => {
            error!("Failed to grab device {}: {}", device_path, e);
            return false as jboolean;
        }
    };

    // Register device with binder observer for KeyLayoutMap lookup
    let binder_observer = get_binder_observer();
    binder_observer.register_device(device_path.clone(), &device.evdev);

    // Add device to event loop
    match event_loop::add_device(device_path.clone(), device.clone()) {
        Ok(()) => {
            info!("Grabbed device: {}", device_path);
            true as jboolean
        }
        Err(e) => {
            error!("Failed to add device to event loop: {}", e);
            binder_observer.unregister_device(&device_path);
            false as jboolean
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_ungrabEvdevDeviceNative(
    _env: JNIEnv,
    _class: JClass,
    j_device_path: JString,
) -> jboolean {
    init_logger();

    let device_path = match _env.get_string(&j_device_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return false as jboolean,
    };

    // Unregister from binder observer
    let binder_observer = get_binder_observer();
    binder_observer.unregister_device(&device_path);

    // Remove from event loop
    match event_loop::remove_device(&device_path) {
        Ok(()) => {
            info!("Ungrabbed device: {}", device_path);
            true as jboolean
        }
        Err(_) => {
            warn!(
                "Device {} was not found in grabbed devices list",
                device_path
            );
            false as jboolean
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_ungrabAllEvdevDevicesNative(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    init_logger();

    let binder_observer = get_binder_observer();

    // Get all device paths before clearing
    let device_paths: Vec<String> = {
        let state = event_loop::EVENT_LOOP_STATE.lock().unwrap();
        state.devices.keys().cloned().collect()
    };

    // Unregister all devices from binder observer
    for path in &device_paths {
        binder_observer.unregister_device(path);
    }

    // Remove all devices from event loop
    event_loop::remove_all_devices();

    info!("Ungrabbed all devices");
    true as jboolean
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_writeEvdevEventNative(
    _env: JNIEnv,
    _class: JClass,
    j_device_path: JString,
    j_type: jint,
    j_code: jint,
    j_value: jint,
) -> jboolean {
    init_logger();

    let device_path = match _env.get_string(&j_device_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return false as jboolean,
    };

    // Write event to device through event loop
    match event_loop::write_event_to_device(&device_path, j_type as u32, j_code as u32, j_value) {
        Ok(()) => true as jboolean,
        Err(_) => false as jboolean,
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_getEvdevDevicesNative(
    mut env: JNIEnv,
    _class: JClass,
) -> jobjectArray {
    init_logger();

    let dir = match Dir::open("/dev/input") {
        Ok(d) => d,
        Err(e) => {
            error!("Failed to open /dev/input directory: {}", e);
            return ptr::null_mut();
        }
    };

    let mut device_handles = Vec::new();
    // Get uinput device paths to exclude them
    let uinput_devices = event_loop::get_uinput_device_paths();

    for entry in dir.iter() {
        let entry = match entry {
            Ok(e) => e,
            Err(_) => continue,
        };

        let file_name = match entry.file_name().to_str() {
            Some(n) => n,
            None => continue,
        };

        // Skip . and ..
        if file_name == "." || file_name == ".." {
            continue;
        }

        let full_path = format!("/dev/input/{}", file_name);

        // Never return uinput devices
        if uinput_devices.contains(&full_path) {
            continue;
        }

        // Try to open the device
        let fd = match nix::fcntl::open(&full_path, OFlag::O_RDONLY, Mode::empty()) {
            Ok(f) => f,
            Err(_) => continue,
        };

        // Create evdev device to get info
        let evdev = match EvdevDevice::new_from_fd(fd) {
            Ok(d) => d,
            Err(_) => {
                let _ = nix::unistd::close(fd);
                continue;
            }
        };

        let name = evdev.name();
        let bus = unsafe { bindings::libevdev_get_id_bustype(evdev.as_ptr()) } as i32;
        let vendor = unsafe { bindings::libevdev_get_id_vendor(evdev.as_ptr()) } as i32;
        let product = unsafe { bindings::libevdev_get_id_product(evdev.as_ptr()) } as i32;

        // Create EvdevDeviceHandle
        match create_evdev_device_handle(&mut env, &full_path, &name, bus, vendor, product) {
            Ok(handle) => device_handles.push(handle),
            Err(e) => {
                error!("Failed to create EvdevDeviceHandle: {:?}", e);
            }
        }

        // Close fd (evdev device will be dropped)
        let _ = nix::unistd::close(fd);
    }

    // Create Java array
    let class = match env.find_class("io/github/sds100/keymapper/common/models/EvdevDeviceHandle") {
        Ok(c) => c,
        Err(e) => {
            error!("Failed to find EvdevDeviceHandle class: {:?}", e);
            return ptr::null_mut();
        }
    };

    let array = match env.new_object_array(device_handles.len() as i32, class, ptr::null_mut()) {
        Ok(a) => a,
        Err(e) => {
            error!("Failed to create EvdevDeviceHandle array: {:?}", e);
            return ptr::null_mut();
        }
    };

    // Fill array
    for (i, handle) in device_handles.iter().enumerate() {
        if let Err(e) =
            env.set_object_array_element(&array, i as i32, unsafe { JObject::from_raw(*handle) })
        {
            error!("Failed to set array element: {:?}", e);
        }
    }

    array.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_startEvdevEventLoop(
    mut env: JNIEnv,
    _class: JClass,
    j_callback_binder: JObject,
) {
    init_logger();

    // Register the callback binder with the C++ callback manager
    // This needs to be done before starting the event loop
    // The C++ callback manager will handle the AIDL interface

    // Convert Java IBinder to AIBinder and register with C++ callback manager
    // We'll use the existing C++ callback manager registration function
    // For now, we'll need to call the JNI function from C++ side
    // Actually, the callback registration should happen in the Kotlin/Java side
    // and the C++ callback manager should already be set up

    // Get the event notifier and register the binder observer
    let notifier = get_event_notifier();
    let binder_observer = get_binder_observer();
    notifier.register(Box::new(binder_observer.clone()));

    // Start the event loop
    match event_loop::start_event_loop(notifier) {
        Ok(()) => {
            info!("Started evdev event loop");

            // Notify callback that event loop started
            let _ = unsafe { bindings::evdev_callback_on_evdev_event_loop_started() };
        }
        Err(e) => {
            error!("Failed to start evdev event loop: {}", e);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_stopEvdevEventLoop(
    _env: JNIEnv,
    _class: JClass,
) {
    init_logger();
    event_loop::stop_event_loop();
    info!("Stopped evdev event loop");
}
