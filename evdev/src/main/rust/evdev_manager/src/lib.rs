mod android;
mod bindings; // libevdev C bindings + KeyLayoutMap C interface bindings
mod device_manager;
mod device_manager_tokio;
mod device_task;
mod evdevcallback_binder_observer;
mod event_loop;
mod input_device_config;
mod input_event_lookup;
mod jni_bridge;
mod key_layout_map;
mod key_layout_map_manager;
mod evdev_error;
mod observer;
mod tokenizer;
mod tokio_runtime;

// Export public types
pub use key_layout_map::{KeyLayoutAxisInfo, KeyLayoutAxisMode, KeyLayoutKey, KeyLayoutMap};

#[macro_use]
extern crate log;
extern crate android_log;

use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_sysbridge_service_BaseSystemBridge_helloEvdevManager(
    mut env: JNIEnv,
    _class: JClass,
    input: JString,
) -> jstring {
    let input: String = env
        .get_string(&input)
        .expect("Couldn't get java string!")
        .into();

    let output = env
        .new_string(format!("Hello from evdev Rust: {}", input))
        .expect("Couldn't create java string!");

    // Initialize Android logger
    android_log::init("KeyMapperSystemBridge").unwrap();

    set_log_panic_hook();

    info!("Hello from evdev Rust");

    output.into_raw()
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
