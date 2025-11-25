use evdev::{util::event_code_to_int, Device, DeviceWrapper, InputEvent};
use evdev_manager_core::android::android_codes;
use evdev_manager_core::android::android_codes::AKEYCODE_UNKNOWN;
use evdev_manager_core::android::keylayout::key_layout_map_manager::KeyLayoutMapManager;
use std::ffi::CString;
use std::os::raw::c_int;
use std::process;
use std::sync::atomic::AtomicI64;
use std::sync::Arc;

pub struct EvdevCallbackBinderObserver {
    key_layout_map_manager: Arc<KeyLayoutMapManager>,
}

static POWER_BUTTON_DOWN_TIME: AtomicI64 = AtomicI64::new(0);

/// Observer that forwards events to the AIDL IEvdevCallback interface
/// Performs KeyLayoutMap conversion from raw evdev codes to Android keycodes
impl EvdevCallbackBinderObserver {
    pub fn new(key_layout_map_manager: Arc<KeyLayoutMapManager>) -> Self {
        EvdevCallbackBinderObserver {
            key_layout_map_manager,
        }
    }

    /// Handle power button emergency kill
    /// Returns true if system bridge should be killed
    fn handle_power_button(code: u32, android_code: u32, value: i32, time_sec: i64) -> bool {
        use std::sync::atomic::{AtomicI64, Ordering};

        // KEY_POWER scan code = 116
        if code == 116 || android_code == android_codes::AKEYCODE_POWER {
            if value == 1 {
                POWER_BUTTON_DOWN_TIME.store(time_sec, Ordering::Relaxed);
            } else if value == 0 {
                // Button up - check if held for 10+ seconds
                let down_time = POWER_BUTTON_DOWN_TIME.load(Ordering::Relaxed);
                if down_time > 0 && time_sec - down_time >= 10 {
                    let _ = unsafe { evdev_callback_on_emergency_kill_system_bridge() };
                    process::exit(0);
                }
                POWER_BUTTON_DOWN_TIME.store(0, Ordering::Relaxed);
            }
        }
        false
    }

    pub fn on_event(&self, device_path: &str, event: &InputEvent) -> bool {
        // Extract event type and code from EventCode
        let (ev_type, ev_code) = event_code_to_int(&event.event_code);

        // Convert raw evdev code to Android keycode
        let android_code = self
            .key_layout_map_manager
            .map_key(device_path, ev_code)
            .map(|key| key.key_code)
            .unwrap_or(AKEYCODE_UNKNOWN);

        // Handle power button emergency kill
        self.handle_power_button(ev_code, android_code, event.value, event.time.tv_sec);

        // Call the AIDL callback via C++ callback manager
        // NOTE: The current C++ callback manager doesn't return the consumed status.
        // We need to modify evdev_callback_jni_manager.cpp to return the consumed status
        // or create a new function that returns it.
        // For now, we'll call the callback and assume it handles the consumed status internally.
        let device_path_cstr = match CString::new(device_path) {
            Ok(s) => s,
            Err(_) => return false,
        };

        let result = unsafe {
            evdev_callback_on_evdev_event(
                device_path_cstr.as_ptr(),
                event.time.tv_sec,
                event.time.tv_usec as i64,
                ev_type as i32,
                ev_code as i32,
                event.value,
                android_code,
            )
        };

        if result != 0 {
            // Callback failed or is dead
            return false;
        }

        // TODO: The C++ callback manager needs to be modified to return the consumed status.
        // The AIDL interface returns a boolean, but the C wrapper doesn't expose it.
        // For now, we'll need to either:
        // 1. Modify evdev_callback_on_evdev_event to return consumed status
        // 2. Create a new function evdev_callback_on_evdev_event_with_result
        // 3. Use a different approach to get the consumed status

        // Temporary: Assume event is consumed if callback succeeded
        // This is not correct - we need the actual return value from AIDL
        true
    }
}

// Manual FFI bindings for evdev_callback_jni_manager.h
#[link(name = "evdev_manager_cpp")]
extern "C" {
    /// Call onEvdevEvent using stored callback
    /// Returns 0 on success, non-zero error code on failure
    fn evdev_callback_on_evdev_event(
        device_path: *const std::ffi::c_char,
        time_sec: i64,
        time_usec: i64,
        type_: i32,
        code: i32,
        value: i32,
        android_code: u32,
    ) -> c_int;

    /// Call onEmergencyKillSystemBridge using stored callback
    /// Returns 0 on success, non-zero error code on failure
    fn evdev_callback_on_emergency_kill_system_bridge() -> c_int;
}

/// Error codes for evdev callback operations (matching C enum)
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum EvdevCallbackError {
    Success = 0,
    InvalidArg = -1,
    BinderConversionFailed = -2,
    CallbackCreationFailed = -3,
    NoCallback = -4,
    InvalidHandle = -5,
    CallbackFailed = -6,
}

impl From<c_int> for EvdevCallbackError {
    fn from(value: c_int) -> Self {
        match value {
            0 => EvdevCallbackError::Success,
            -1 => EvdevCallbackError::InvalidArg,
            -2 => EvdevCallbackError::BinderConversionFailed,
            -3 => EvdevCallbackError::CallbackCreationFailed,
            -4 => EvdevCallbackError::NoCallback,
            -5 => EvdevCallbackError::InvalidHandle,
            -6 => EvdevCallbackError::CallbackFailed,
            _ => EvdevCallbackError::InvalidArg,
        }
    }
}
