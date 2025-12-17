use evdev::{util::event_code_to_int, InputEvent};
use evdev_manager_core::android::android_codes;
use evdev_manager_core::android::android_codes::AKEYCODE_UNKNOWN;
use evdev_manager_core::android::keylayout::key_layout_map_manager::KeyLayoutMapManager;
use evdev_manager_core::evdev_device_info::EvdevDeviceInfo;
use jni::objects::{GlobalRef, JValue};
use jni::JavaVM;
use std::process;
use std::sync::{Arc, Mutex};

pub struct EvdevJniObserver {
    jvm: Arc<JavaVM>,
    system_bridge: GlobalRef,
    key_layout_map_manager: Arc<KeyLayoutMapManager>,
    power_button_down_time: Mutex<libc::time_t>,
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
            power_button_down_time: Mutex::new(0),
        }
    }

    /// Handle power button emergency kill.
    fn handle_power_button(
        &self,
        ev_code: u32,
        android_code: u32,
        value: i32,
        time_sec: libc::time_t,
    ) {
        let mut time_guard = self.power_button_down_time.lock().unwrap();
        // KEY_POWER scan code = 116
        if ev_code == 116 || android_code == android_codes::AKEYCODE_POWER {
            if value == 1 {
                *time_guard = time_sec;
            } else if value == 0 {
                // Button up - check if held for 10+ seconds
                let down_time = *time_guard;
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
                *time_guard = 0
            }
        }
    }

    pub fn on_event(
        &self,
        device_id: usize,
        device_identifier: &EvdevDeviceInfo,
        event: &InputEvent,
    ) -> bool {
        let mut env = self
            .jvm
            .attach_current_thread_permanently()
            .expect("Failed to attach to JVM thread");

        // Extract event type and code from EventCode
        let (ev_type, ev_code) = event_code_to_int(&event.event_code);

        let key_result = self
            .key_layout_map_manager
            .map_key(device_identifier, ev_code);

        // Convert raw evdev code to Android keycode
        let android_code = match key_result {
            Ok(Some(key_code)) => key_code,
            Ok(None) | Err(_) => AKEYCODE_UNKNOWN,
        };

        // Handle power button emergency kill
        self.handle_power_button(ev_code, android_code, event.value, event.time.tv_sec);

        // Call BaseSystemBridge.onEvdevEvent() via JNI

        let result = env.call_method(
            &self.system_bridge,
            "onEvdevEvent",
            "(IJJIIII)Z",
            &[
                JValue::Int(device_id as i32),
                #[allow(clippy::unnecessary_cast)]
                // When building for 32 bit the tv_sec type may be i32
                JValue::Long(event.time.tv_sec as i64),
                JValue::Long(event.time.tv_usec.into()),
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
                value.z().unwrap_or_else(|e| {
                    error!("Failed to extract boolean from result: {:?}", e);
                    false
                })
            }
            Err(e) => {
                error!("Failed to call onEvdevEvent: {:?}", e);
                false
            }
        }
    }
}
