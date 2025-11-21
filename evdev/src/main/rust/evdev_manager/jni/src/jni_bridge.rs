use crate::evdev_callback_binder_observer::EvdevCallbackBinderObserver;
use evdev_manager_core::event_loop;
use evdev_manager_core::grabbed_device::GrabbedDevice;
use evdev_manager_core::observer::EvdevEventNotifier;
use evdev::{Device, DeviceWrapper};
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jint, jobject, jobjectArray};
use jni::JNIEnv;
use std::fs::File;
use std::ptr;
use std::ptr::null;
use std::sync::{Arc, OnceLock};

static EVENT_NOTIFIER: OnceLock<Arc<EvdevEventNotifier>> = OnceLock::new();
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

fn create_java_evdev_device_handle(
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
            JValue::Object(&path_str.into()),
            JValue::Object(&name_str.into()),
            JValue::Int(bus),
            JValue::Int(vendor),
            JValue::Int(product),
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
    let device_path: String = match env.get_string(&j_device_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(e) => {
            error!("Failed to get device path string: {:?}", e);
            return false as jboolean;
        }
    };

    false as jboolean

    // Check if device is already grabbed
    // if event_loop::is_device_grabbed(&device_path) {
    //     warn!("Device {} is already grabbed", device_path);
    //     return false as jboolean;
    // }
    //
    // // Never grab uinput devices
    // if device_path.contains("uinput") {
    //     warn!("Cannot grab uinput device: {}", device_path);
    //     return false as jboolean;
    // }
    //
    // // Grab the device
    // let device = match GrabbedDevice::new(&device_path) {
    //     Ok(d) => Arc::new(d),
    //     Err(e) => {
    //         error!("Failed to grab device {}: {}", device_path, e);
    //         return false as jboolean;
    //     }
    // };
    //
    // // Register device with binder observer for KeyLayoutMap lookup
    // let binder_observer = get_binder_observer();
    // binder_observer.register_device(device_path.as_str(), &device.evdev);
    //
    // // Add device to event loop
    // match event_loop::add_device(device_path.clone(), device.clone()) {
    //     Ok(()) => {
    //         info!("Grabbed device: {}", device_path);
    //         true as jboolean
    //     }
    //     Err(e) => {
    //         error!("Failed to add device to event loop: {}", e);
    //         binder_observer.unregister_device(&device_path);
    //         false as jboolean
    //     }
    // }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_ungrabEvdevDeviceNative(
    mut _env: JNIEnv,
    _class: JClass,
    j_device_path: JString,
) -> jboolean {
    let device_path = match _env.get_string(&j_device_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return false as jboolean,
    };

    false as jboolean

    // Unregister from binder observer
    // let binder_observer = get_binder_observer();
    // binder_observer.unregister_device(&device_path);
    //
    // // Remove from event loop
    // match event_loop::remove_device(&device_path) {
    //     Ok(()) => {
    //         info!("Ungrabbed device: {}", device_path);
    //         true as jboolean
    //     }
    //     Err(_) => {
    //         warn!(
    //             "Device {} was not found in grabbed devices list",
    //             device_path
    //         );
    //         false as jboolean
    //     }
    // }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_ungrabAllEvdevDevicesNative(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    let binder_observer = get_binder_observer();

    // Unregister all devices from binder observer
    // for path in &device_paths {
    //     binder_observer.unregister_device(path);
    // }

    // Remove all devices from event loop
    // event_loop::remove_all_devices();

    info!("Ungrabbed all devices");
    true as jboolean
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_writeEvdevEventNative(
    mut _env: JNIEnv,
    _class: JClass,
    j_device_path: JString,
    j_type: jint,
    j_code: jint,
    j_value: jint,
) -> jboolean {
    let device_path = match _env.get_string(&j_device_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return false as jboolean,
    };

    false as jboolean

    // Write event to device through event loop
    // match event_loop::write_event_to_device(&device_path, j_type as u32, j_code as u32, j_value) {
    //     Ok(()) => true as jboolean,
    //     Err(_) => false as jboolean,
    // }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_getEvdevDevicesNative(
    mut env: JNIEnv,
    _class: JClass,
) -> jobjectArray {
    // let dir = match Dir::open("/dev/input") {
    //     Ok(d) => d,
    //     Err(e) => {
    //         error!("Failed to open /dev/input directory: {}", e);
    //         return ptr::null_mut();
    //     }
    // };

    // let mut device_handles = Vec::new();
    // Get uinput device paths to exclude them
    // let uinput_devices = event_loop::get_uinput_device_paths();
    //
    // for entry in dir.iter() {
    //     let entry = match entry {
    //         Ok(e) => e,
    //         Err(_) => continue,
    //     };
    //
    //     let file_name = match entry.file_name().to_str() {
    //         Some(n) => n,
    //         None => continue,
    //     };
    //
    //     // Skip . and ..
    //     if file_name == "." || file_name == ".." {
    //         continue;
    //     }
    //
    //     let full_path = format!("/dev/input/{}", file_name);
    //
    //     // Never return uinput devices
    //     if uinput_devices.contains(&full_path) {
    //         continue;
    //     }
    //
    //     // Try to open the device
    //     let file = match File::open(&full_path) {
    //         Ok(f) => f,
    //         Err(_) => continue,
    //     };
    //
    //     // Create evdev device to get info
    //     let evdev = match Device::new_from_file(file) {
    //         Ok(d) => d,
    //         Err(_) => continue,
    //     };
    //
    //     let name = evdev.name();
    //     let bus = evdev.bustype() as i32;
    //     let vendor = evdev.vendor_id() as i32;
    //     let product = evdev.product_id() as i32;
    //
    //     // Create EvdevDeviceHandle
    //     match create_java_evdev_device_handle(&mut _env, &full_path, &name, bus, vendor, product) {
    //         Ok(handle) => device_handles.push(handle),
    //         Err(e) => {
    //             error!("Failed to create EvdevDeviceHandle: {:?}", e);
    //         }
    //     }
    // }
    //
    // // Create Java array
    let class = match env.find_class("io/github/sds100/keymapper/common/models/EvdevDeviceHandle") {
        Ok(c) => c,
        Err(e) => {
            error!("Failed to find EvdevDeviceHandle class: {:?}", e);
            return ptr::null_mut();
        }
    };

    let array = match env.new_object_array(0, class, JObject::null()) {
        Ok(a) => a,
        Err(e) => {
            error!("Failed to create EvdevDeviceHandle array: {:?}", e);
            return ptr::null_mut();
        }
    };
    //
    // // Fill array
    // for (i, handle) in device_handles.iter().enumerate() {
    //     if let Err(e) =
    //         _env.set_object_array_element(&array, i as i32, unsafe { JObject::from_raw(*handle) })
    //     {
    //         error!("Failed to set array element: {:?}", e);
    //     }
    // }

    array.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_startEvdevManager(
    _env: JNIEnv,
    _class: JClass,
    _j_callback_binder: JObject,
) {
    android_log::init("KeyMapperSystemBridge").unwrap();
    set_log_panic_hook();

    let notifier = get_event_notifier();
    let binder_observer = get_binder_observer();

    event_loop::start_event_loop(&notifier).unwrap();

    // notifier.register(Box::new(binder_observer.clone()));
    //
    // // Initialize device task manager
    // match event_loop::init_device_task_manager(notifier) {
    //     Ok(()) => {
    //         info!("Initialized evdev manager with Tokio");
    //
    //         // Notify callback that event loop started
    //         let _ = unsafe { bindings::evdev_callback_on_evdev_event_loop_started() };
    //     }
    //     Err(e) => {
    //         error!("Failed to initialize device task manager: {}", e);
    //     }
    // }
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_stopEvdevManager(
    _env: JNIEnv,
    _class: JClass,
) {
    // event_loop::remove_all_devices();
    info!("Stopped evdev manager");
}

fn set_log_panic_hook() {
    std::panic::set_hook(Box::new(|panic_info| {
        error!("PANIC in Rust code!");

        if let Some(location) = panic_info.location() {
            error!(
                "Panic at {}:{}:{}",
                location.file(),
                location.line(),
                location.column()
            );
        } else {
            error!("Panic at unknown location");
        }

        if let Some(payload) = panic_info.payload().downcast_ref::<&str>() {
            error!("Panic message: {}", payload);
        } else if let Some(payload) = panic_info.payload().downcast_ref::<String>() {
            error!("Panic message: {}", payload);
        } else {
            error!("Panic with unknown payload");
        }
    }));
}
