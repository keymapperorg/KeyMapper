use evdev::{util::event_code_to_int, InputEvent};
use evdev_manager_core::android::android_codes;
use evdev_manager_core::android::android_codes::AKEYCODE_UNKNOWN;
use evdev_manager_core::android::keylayout::key_layout_map::KeyLayoutKey;
use evdev_manager_core::android::keylayout::key_layout_map_manager::KeyLayoutMapManager;
use evdev_manager_core::device_identifier::DeviceIdentifier;
use jni::objects::{GlobalRef, JObject, JValue};
use jni::JavaVM;
use std::process;
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::Arc;

pub struct EvdevJniObserver {
    jvm: Arc<JavaVM>,
    system_bridge: GlobalRef,
    key_layout_map_manager: Arc<KeyLayoutMapManager>,
}

impl std::fmt::Debug for EvdevJniObserver {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("EvdevJniObserver")
            .field("jvm", &"<JavaVM>")
            .field("system_bridge", &"<GlobalRef>")
            .field("key_layout_map_manager", &"<KeyLayoutMapManager>")
            .finish()
    }
}

static POWER_BUTTON_DOWN_TIME: AtomicI64 = AtomicI64::new(0);

/// Observer that forwards events to BaseSystemBridge via JNI
/// Performs KeyLayoutMap conversion from raw evdev codes to Android keycodes
impl EvdevJniObserver {
    pub fn new(
        jvm: Arc<JavaVM>,
        system_bridge: GlobalRef,
        key_layout_map_manager: Arc<KeyLayoutMapManager>,
    ) -> Self {
        EvdevJniObserver {
            jvm,
            system_bridge,
            key_layout_map_manager,
        }
    }

    /// Handle power button emergency kill.
    fn handle_power_button(
        &self,
        ev_code: u32,
        android_code: u32,
        value: i32,
        time_sec: i64,
    ) {
        // KEY_POWER scan code = 116
        if ev_code == 116 || android_code == android_codes::AKEYCODE_POWER {
            if value == 1 {
                POWER_BUTTON_DOWN_TIME.store(time_sec, Ordering::Relaxed);
            } else if value == 0 {
                // Button up - check if held for 10+ seconds
                let down_time = POWER_BUTTON_DOWN_TIME.load(Ordering::Relaxed);
                if down_time > 0 && time_sec - down_time >= 10 {
                    // Call BaseSystemBridge.onEmergencyKillSystemBridge() via JNI
                    if let Ok(mut env) = self.jvm.attach_current_thread() {
                        let _ = env.call_method(
                            &self.system_bridge,
                            "onEmergencyKillSystemBridge",
                            "()V",
                            &[],
                        );
                    }
                    process::exit(0);
                }
                POWER_BUTTON_DOWN_TIME.store(0, Ordering::Relaxed);
            }
        }
    }

    const UNKNOWN_KEY: KeyLayoutKey = KeyLayoutKey {
        key_code: AKEYCODE_UNKNOWN,
        flags: 0,
    };

    pub fn on_event(
        &self,
        device_path: &str,
        device_id: &DeviceIdentifier,
        event: &InputEvent,
    ) -> bool {
        // Extract event type and code from EventCode
        let (ev_type, ev_code) = event_code_to_int(&event.event_code);

        // Convert raw evdev code to Android keycode
        let android_code = self
            .key_layout_map_manager
            .map_key(device_id, ev_code)
            .unwrap_or(Some(Self::UNKNOWN_KEY))
            .map(|key| key.key_code)
            .unwrap_or(AKEYCODE_UNKNOWN);

        // Handle power button emergency kill
        self.handle_power_button(ev_code, android_code, event.value, event.time.tv_sec);

        // Call BaseSystemBridge.onEvdevEvent() via JNI
        // TODO attach permanently? Is attaching necessary if sending primitives?
        let mut env = match self.jvm.attach_current_thread() {
            Ok(env) => env,
            Err(e) => {
                error!("Failed to attach to JVM thread: {:?}", e);
                return false;
            }
        };

        let device_path_jstring = match env.new_string(device_path) {
            Ok(s) => s,
            Err(e) => {
                error!("Failed to create Java string: {:?}", e);
                return false;
            }
        };

        let result = env.call_method(
            &self.system_bridge,
            "onEvdevEvent",
            "(Ljava/lang/String;JJIIII)Z",
            &[
                JValue::Object(&JObject::from(device_path_jstring)),
                JValue::Long(event.time.tv_sec),
                JValue::Long(event.time.tv_usec),
                JValue::Int(ev_type as i32),
                JValue::Int(ev_code as i32),
                JValue::Int(event.value),
                JValue::Int(android_code as i32),
            ],
        );

        match result {
            Ok(value) => {
                // The method returns a primitive boolean (Z)
                // Extract the boolean value using z() method which returns Result<bool, _>
                match value.z() {
                    Ok(consumed) => consumed,
                    Err(e) => {
                        error!("Failed to extract boolean from result: {:?}", e);
                        false
                    }
                }
            }
            Err(e) => {
                error!("Failed to call onEvdevEvent: {:?}", e);
                false
            }
        }
    }

    pub fn on_event_loop_started(&self) {
        let mut env = match self.jvm.attach_current_thread() {
            Ok(env) => env,
            Err(e) => {
                error!("Failed to attach to JVM thread: {:?}", e);
                return;
            }
        };

        if let Err(e) = env.call_method(
            &self.system_bridge,
            "onEvdevEventLoopStarted",
            "()V",
            &[],
        ) {
            error!("Failed to call onEvdevEventLoopStarted: {:?}", e);
        }
    }
}

