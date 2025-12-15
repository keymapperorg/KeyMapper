use crate::evdev_jni_observer::EvdevJniObserver;
use evdev::{Device, DeviceWrapper};
use evdev_manager_core::android::keylayout::key_layout_map_manager::KeyLayoutMapManager;
use evdev_manager_core::device_identifier::DeviceIdentifier;
use evdev_manager_core::event_loop::EventLoopManager;
use evdev_manager_core::grab_device_request::GrabDeviceRequest;
use jni::objects::{JClass, JIntArray, JObject, JObjectArray, JString, JValue};
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
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_initEvdevManager(
    env: JNIEnv,
    this: JObject,
) {
    android_log::init("KeyMapperSystemBridge").unwrap();
    // Set log level: Info for production builds, Debug for debug builds
    let log_level = if cfg!(debug_assertions) {
        LevelFilter::Debug
    } else {
        LevelFilter::Info
    };
    log::set_max_level(log_level);
    set_log_panic_hook();

    info!("Initializing evdev manager");

    // Get the JavaVM
    let jvm = env.get_java_vm().expect("Failed to get JavaVM");

    // Create a global reference to the SystemBridge instance
    let system_bridge = env
        .new_global_ref(this)
        .expect("Failed to create global reference to SystemBridge");

    // Initialize the JNI observer
    let key_layout_manager = KeyLayoutMapManager::get();
    let observer = EvdevJniObserver::new(Arc::new(jvm), system_bridge, key_layout_manager);

    if JNI_OBSERVER.set(observer).is_err() {
        panic!("JNI observer already initialized");
    }

    // Initialize and start the event loop with the observer
    EventLoopManager::init(|device_id, device_identifier, event| {
        get_jni_observer().on_event(device_id, device_identifier, event)
    });

    EventLoopManager::get()
        .start()
        .inspect_err(|e| error!("Failed to start event loop: {:?}", e))
        .unwrap();
}

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_destroyEvdevManager(
    _env: JNIEnv,
    _class: JClass,
) {
    info!("Destroying evdev manager");

    EventLoopManager::get()
        .stop()
        .inspect_err(|e| error!("Failed to stop event loop: {:?}", e))
        .unwrap();
}

/// Set the list of grabbed devices. Takes an array of GrabDeviceRequest and returns an array of GrabbedDeviceHandle.
#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_setGrabbedDevicesNative(
    mut env: JNIEnv,
    _class: JClass,
    j_devices: jobjectArray,
) -> jobjectArray {
    // Parse the input array of GrabDeviceRequest
    let mut requested_devices: Vec<GrabDeviceRequest> = Vec::new();

    // Convert raw jobjectArray to JObjectArray
    let devices_array: JObjectArray = unsafe { JObjectArray::from_raw(j_devices) };

    let array_length = match env.get_array_length(&devices_array) {
        Ok(len) => len,
        Err(e) => {
            error!("Failed to get array length: {:?}", e);
            return ptr::null_mut();
        }
    };

    for i in 0..array_length {
        let obj = match env.get_object_array_element(&devices_array, i) {
            Ok(o) => o,
            Err(e) => {
                error!("Failed to get array element {}: {:?}", i, e);
                continue;
            }
        };

        match parse_grab_device_request(&mut env, &obj) {
            Ok(grab_request) => requested_devices.push(grab_request),
            Err(e) => {
                error!("Failed to parse GrabDeviceRequest at index {}: {:?}", i, e);
            }
        }
    }

    let grabbed_devices = EventLoopManager::get().set_grabbed_devices(requested_devices);
    create_java_grabbed_device_handle_array(&mut env, grabbed_devices)
}

/// Write an event to a grabbed device using its device ID
#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_writeEvdevEventNative(
    _env: JNIEnv,
    _class: JClass,
    j_device_id: jint,
    j_type: jint,
    j_code: jint,
    j_value: jint,
) -> jboolean {
    EventLoopManager::get()
        .write_event(j_device_id as usize, j_type as u32, j_code as u32, j_value)
        .is_ok() as jboolean
}

/// Write an event to a grabbed device using its device ID
#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_writeEvdevEventKeyCodeNative(
    _env: JNIEnv,
    _class: JClass,
    j_device_id: jint,
    j_key_code: jint,
    j_value: jint,
) -> jboolean {
    EventLoopManager::get()
        .write_key_code_event(j_device_id as usize, j_key_code as u32, j_value)
        .is_ok() as jboolean
}

/// Get all available evdev devices (returns EvdevDeviceInfo array)
#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_SystemBridge_getEvdevDevicesNative(
    mut env: JNIEnv,
    _class: JClass,
) -> jobjectArray {
    let mut device_infos = Vec::new();
    let device_paths_result = EventLoopManager::get().get_all_real_devices();

    match device_paths_result {
        Ok(paths) => {
            for path in paths {
                match get_evdev_from_path(path.clone()) {
                    Some(device) => {
                        let name = device.name().unwrap_or("");
                        let bus = device.bustype() as i32;
                        let vendor = device.vendor_id() as i32;
                        let product = device.product_id() as i32;

                        // Create EvdevDeviceInfo
                        match create_java_evdev_device_info(&mut env, name, bus, vendor, product) {
                            Ok(info) => device_infos.push(info),
                            Err(e) => {
                                error!("Failed to create EvdevDeviceInfo: {:?}", e);
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
    let class = match env.find_class("io/github/sds100/keymapper/common/models/EvdevDeviceInfo") {
        Ok(c) => c,
        Err(e) => {
            error!("Failed to find EvdevDeviceInfo class: {:?}", e);
            return ptr::null_mut();
        }
    };

    let array = match env.new_object_array(device_infos.len() as i32, class, JObject::null()) {
        Ok(a) => a,
        Err(e) => {
            error!("Failed to create EvdevDeviceInfo array: {:?}", e);
            return ptr::null_mut();
        }
    };

    // Fill array
    for (i, info) in device_infos.iter().enumerate() {
        if let Err(e) =
            env.set_object_array_element(&array, i as i32, unsafe { JObject::from_raw(*info) })
        {
            error!("Failed to set array element: {:?}", e);
        }
    }

    array.into_raw()
}

fn get_evdev_from_path(path: PathBuf) -> Option<Device> {
    Device::new_from_path(path.clone())
        .inspect_err(|e| warn!("Failed to open evdev device {:?}: {:?}", path, e))
        .ok()
}

/// Parse a Java EvdevDeviceInfo object into a Rust DeviceIdentifier
/// Note: version is set to 0 as it's not exposed in the Java API
fn parse_evdev_device_info(
    env: &mut JNIEnv,
    obj: &JObject,
) -> Result<DeviceIdentifier, jni::errors::Error> {
    // Get name field
    let name_field = env.get_field(obj, "name", "Ljava/lang/String;")?;
    let name_obj = name_field.l()?;
    let name_jstring = JString::from(name_obj);
    let name: String = env
        .get_string(&name_jstring)?
        .to_string_lossy()
        .into_owned();

    // Get bus field
    let bus = env.get_field(obj, "bus", "I")?.i()? as u16;

    // Get vendor field
    let vendor = env.get_field(obj, "vendor", "I")?.i()? as u16;

    // Get product field
    let product = env.get_field(obj, "product", "I")?.i()? as u16;

    Ok(DeviceIdentifier {
        name,
        bus,
        vendor,
        product,
        version: 0, // Version is not exposed in Java API, set to 0
    })
}

/// Parse a Java GrabDeviceRequest object into a Rust GrabDeviceRequest
fn parse_grab_device_request(
    env: &mut JNIEnv,
    obj: &JObject,
) -> Result<GrabDeviceRequest, jni::errors::Error> {
    // Get the device field (EvdevDeviceInfo)
    let device_field = env.get_field(
        obj,
        "device",
        "Lio/github/sds100/keymapper/common/models/EvdevDeviceInfo;",
    )?;
    let device_obj = device_field.l()?;
    let device_identifier = parse_evdev_device_info(env, &device_obj)?;

    // Get the extraEventCodes field (int[])
    let extra_codes_field = env.get_field(obj, "extraKeyCodes", "[I")?;
    let extra_codes_obj = extra_codes_field.l()?;
    let extra_codes_array = JIntArray::from(extra_codes_obj);

    // Convert Java int[] to Vec<EventCode>
    let array_length = env.get_array_length(&extra_codes_array)? as usize;
    let extra_key_codes: Vec<u32> = Vec::with_capacity(array_length);

    Ok(GrabDeviceRequest {
        device_identifier,
        extra_key_codes: extra_key_codes,
    })
}

fn create_java_grabbed_device_handle_array(
    mut env: &mut JNIEnv,
    grabbed_devices: Vec<(usize, DeviceIdentifier)>,
) -> jobjectArray {
    let handle_class =
        match env.find_class("io/github/sds100/keymapper/common/models/GrabbedDeviceHandle") {
            Ok(c) => c,
            Err(e) => {
                panic!("Failed to find GrabbedDeviceHandle class: {:?}", e);
            }
        };

    let array =
        match env.new_object_array(grabbed_devices.len() as i32, &handle_class, JObject::null()) {
            Ok(a) => a,
            Err(e) => {
                panic!("Failed to create GrabbedDeviceHandle array: {:?}", e);
            }
        };

    // grabbed_devices contains (slab_key, DeviceIdentifier) tuples
    // The slab_key is used as the device_id for O(1) lookup when writing events
    for (i, (slab_key, device_identifier)) in grabbed_devices.iter().enumerate() {
        match create_java_grabbed_device_handle(
            &mut env,
            *slab_key as i32, // slab key = device_id for O(1) lookup
            &device_identifier.name,
            device_identifier.bus as i32,
            device_identifier.vendor as i32,
            device_identifier.product as i32,
        ) {
            Ok(handle) => {
                if let Err(e) = env.set_object_array_element(&array, i as i32, unsafe {
                    JObject::from_raw(handle)
                }) {
                    error!("Failed to set array element: {:?}", e);
                }
            }
            Err(e) => {
                error!("Failed to create GrabbedDeviceHandle: {:?}", e);
            }
        }
    }

    array.into_raw()
}

/// Create a Java EvdevDeviceInfo object
fn create_java_evdev_device_info(
    env: &mut JNIEnv,
    name: &str,
    bus: i32,
    vendor: i32,
    product: i32,
) -> Result<jobject, jni::errors::Error> {
    let class = env.find_class("io/github/sds100/keymapper/common/models/EvdevDeviceInfo")?;
    let name_str = env.new_string(name)?;

    let obj = env.new_object(
        class,
        "(Ljava/lang/String;III)V",
        &[
            JValue::Object(&name_str.into()),
            JValue::Int(bus),
            JValue::Int(vendor),
            JValue::Int(product),
        ],
    )?;

    Ok(obj.into_raw())
}

/// Create a Java GrabbedDeviceHandle object
fn create_java_grabbed_device_handle(
    env: &mut JNIEnv,
    id: i32,
    name: &str,
    bus: i32,
    vendor: i32,
    product: i32,
) -> Result<jobject, jni::errors::Error> {
    let class = env.find_class("io/github/sds100/keymapper/common/models/GrabbedDeviceHandle")?;
    let name_str = env.new_string(name)?;

    let obj = env.new_object(
        class,
        "(ILjava/lang/String;III)V",
        &[
            JValue::Int(id),
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
