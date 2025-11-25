use crate::evdev_callback_binder_observer::EvdevCallbackBinderObserver;
use evdev_manager_core::event_loop::EventLoopManager;
use jni::objects::{GlobalRef, JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jint, jobject, jobjectArray};
use jni::JNIEnv;
use std::ptr;
use std::sync::OnceLock;

static BINDER_OBSERVER: OnceLock<EvdevCallbackBinderObserver> = OnceLock::new();

fn get_binder_observer() -> &'static EvdevCallbackBinderObserver {
    BINDER_OBSERVER.get_or_init(EvdevCallbackBinderObserver::new)
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_initEvdevManager(
    _env: JNIEnv,
    _class: JClass,
    _j_callback_binder: JObject,
) {
    info!("Initializing evdev manager");
    android_log::init("KeyMapperSystemBridge").unwrap();
    set_log_panic_hook();

    EventLoopManager::get()
        .register_observer(|path, device| get_binder_observer().on_event(path, device));
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_destroyEvdevManager(
    _env: JNIEnv,
    _class: JClass,
    _j_callback_binder: JObject,
) {
    info!("Destroying evdev manager");

    EventLoopManager::get()
        .stop()
        .inspect_err(|e| error!("Failed to stop event loop: {:?}", e))
        .unwrap();
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_startEventLoop(
    _env: JNIEnv,
    _class: JClass,
    _j_callback_binder: JObject,
) {
    EventLoopManager::get()
        .start()
        .inspect_err(|e| error!("Failed to start event loop: {:?}", e))
        .unwrap();
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_stopEventLoop(
    _env: JNIEnv,
    _class: JClass,
) {
    EventLoopManager::get()
        .stop()
        .inspect_err(|e| error!("Failed to stop event loop: {:?}", e))
        .unwrap();

    info!("Stopped evdev manager");
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

    EventLoopManager::get()
        .grab_device(device_path.as_str())
        .is_ok() as jboolean
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
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_ungrabAllEvdevDevicesNative(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
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
