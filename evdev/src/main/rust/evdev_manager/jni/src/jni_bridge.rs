use crate::evdev_jni_observer::EvdevJniObserver;
use evdev::{Device, DeviceWrapper};
use evdev_manager_core::android::keylayout::key_layout_map_manager::KeyLayoutMapManager;
use evdev_manager_core::event_loop::EventLoopManager;
use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jint, jobject, jobjectArray};
use jni::JNIEnv;
use log::LevelFilter;
use std::path::PathBuf;
use std::ptr;
use std::sync::{Arc, OnceLock};

static JNI_OBSERVER: OnceLock<EvdevJniObserver> = OnceLock::new();

fn get_jni_observer() -> &'static EvdevJniObserver {
    JNI_OBSERVER.get().expect("JNI observer not initialized")
}

/// MUST only be called once in the lifetime of the process.
#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_initEvdevManager(
    env: JNIEnv,
    this: JObject,
) {
    android_log::init("KeyMapperSystemBridge").unwrap();
    // Set minimum log level to reduce verbosity from JNI
    log::set_max_level(LevelFilter::Debug);
    set_log_panic_hook();

    info!("Initializing evdev manager");

    // Get the JavaVM
    let jvm = env.get_java_vm().expect("Failed to get JavaVM");

    // Create a global reference to the BaseSystemBridge instance
    let system_bridge = env
        .new_global_ref(this)
        .expect("Failed to create global reference to BaseSystemBridge");

    // Initialize the JNI observer
    let key_layout_manager = KeyLayoutMapManager::get();
    let observer = EvdevJniObserver::new(Arc::new(jvm), system_bridge, key_layout_manager);

    if let Err(_) = JNI_OBSERVER.set(observer) {
        panic!("JNI observer already initialized");
    }

    // Register the observer with the event loop
    EventLoopManager::get().register_observer(|path, device_id, event| {
        get_jni_observer().on_event(path, device_id, event)
    });

    // Start the event loop
    EventLoopManager::get()
        .start()
        .inspect_err(|e| error!("Failed to start event loop: {:?}", e))
        .unwrap();

    // Notify that the event loop has started
    get_jni_observer().on_event_loop_started();
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_destroyEvdevManager(
    _env: JNIEnv,
    _class: JClass,
) {
    info!("Destroying evdev manager");

    EventLoopManager::get()
        .stop()
        .inspect_err(|e| error!("Failed to stop event loop: {:?}", e))
        .unwrap();
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_grabEvdevDeviceNative(
    mut env: JNIEnv,
    _class: JClass,
    j_name: JString,
    j_bus: jint,
    j_vendor: jint,
    j_product: jint
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
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_ungrabAllEvdevDevicesNative(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    EventLoopManager::get().ungrab_all_devices().is_ok() as jboolean
}

// TODO
#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_writeEvdevEventNative(
    mut _env: JNIEnv,
    _class: JClass,
    _j_device_path: JString,
    _j_type: jint,
    _j_code: jint,
    _j_value: jint,
) -> jboolean {
    false as jboolean
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_getEvdevDevicesNative(
    mut env: JNIEnv,
    _class: JClass,
) -> jobjectArray {
    let mut device_handles = Vec::new();
    let device_paths_result = EventLoopManager::get().get_all_real_devices();

    match device_paths_result {
        Ok(paths) => {
            for path in paths {
                match get_evdev_from_path(path.clone()) {
                    Some(device) => {
                        let name = device.name().unwrap();
                        let bus = device.bustype() as i32;
                        let vendor = device.vendor_id() as i32;
                        let product = device.product_id() as i32;

                        // Create EvdevDeviceHandle
                        match create_java_evdev_device_handle(
                            &mut env,
                            path.to_str().unwrap(),
                            name,
                            bus,
                            vendor,
                            product,
                        ) {
                            Ok(handle) => device_handles.push(handle),
                            Err(e) => {
                                error!("Failed to create EvdevDeviceHandle: {:?}", e);
                            }
                        }
                    }
                    None => continue,
                }
            }
        }
        Err(e) => {
            error!("Failed to get input device paths: {:?}", e);
        }
    }

    // Create Java array
    let class = match env.find_class("io/github/sds100/keymapper/common/models/EvdevDeviceHandle") {
        Ok(c) => c,
        Err(e) => {
            error!("Failed to find EvdevDeviceHandle class: {:?}", e);
            return ptr::null_mut();
        }
    };

    let array = match env.new_object_array(device_handles.len() as i32, class, JObject::null()) {
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

fn get_evdev_from_path(path: PathBuf) -> Option<Device> {
    Device::new_from_path(path.clone())
        .inspect_err(|e| warn!("Failed to open evdev device: {:?}", e))
        .ok()
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
