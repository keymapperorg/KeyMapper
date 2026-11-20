use jni::objects::JObject;
use jni::sys::jstring;
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_io_github_sds100_keymapper_keymapengine_KeyMapEngine_version(
    env: JNIEnv,
    _this: JObject,
) -> jstring {
    env.new_string(keymap_engine_core::version())
        .expect("Failed to create Java string")
        .into_raw()
}
