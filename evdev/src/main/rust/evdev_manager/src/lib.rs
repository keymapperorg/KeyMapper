mod bindings; // libevdev C bindings + KeyLayoutMap C interface bindings
mod device_manager;
mod device_manager_tokio;
mod device_task;
mod enums;
mod evdev;
mod evdevcallback_binder_observer;
mod event_loop;
mod key_layout_map_manager;
mod observer;
mod jni_bridge;
mod tokio_runtime;

// Public re-exports for testing
// Integration tests need public APIs, so we make these always available
// They're safe to expose as they're part of the internal API surface
pub use evdevcallback_binder_observer::{EmergencyKillCallback, EvdevCallbackBinderObserver};
pub use evdev::{EvdevEvent, EvdevError};
pub use enums::EventType;

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
    