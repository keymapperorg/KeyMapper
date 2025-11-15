use crate::android::android_codes;
use crate::bindings;
use crate::key_layout_map_manager::KeyLayoutMapManager;
use crate::observer::EvdevEventObserver;
use evdev::{util::event_code_to_int, Device, DeviceWrapper, InputEvent};
use std::ffi::CString;
use std::process;
use std::sync::Arc;

/// Observer that forwards events to the AIDL IEvdevCallback interface
/// Performs KeyLayoutMap conversion from raw evdev codes to Android keycodes
pub struct EvdevCallbackBinderObserver {
    /// KeyLayoutMap manager for key code conversion
    key_layout_map_manager: Arc<KeyLayoutMapManager>,
}

impl EvdevCallbackBinderObserver {
    pub fn new() -> Self {
        Self {
            key_layout_map_manager: Arc::new(KeyLayoutMapManager::new()),
        }
    }

    /// Register a device and load its KeyLayoutMap
    pub fn register_device(&self, device_path: &str, evdev: &Device) {
        let name = evdev.name().expect("Evdev name is required");
        let bus = evdev.bustype();
        let vendor = evdev.vendor_id();
        let product = evdev.product_id();
        let version = evdev.version();

        self.key_layout_map_manager.register_device(
            device_path,
            name,
            bus,
            vendor,
            product,
            version,
        );
    }

    /// Unregister a device and cleanup its KeyLayoutMap
    pub fn unregister_device(&self, device_path: &str) {
        self.key_layout_map_manager.unregister_device(device_path);
    }

    pub fn unregister_all(&self) {
        self.key_layout_map_manager.unregister_device()
    }

    /// Handle power button emergency kill
    /// Returns true if system bridge should be killed
    fn handle_power_button(&self, code: u32, android_code: i32, value: i32, time_sec: i64) -> bool {
        use std::sync::atomic::{AtomicI64, Ordering};
        static POWER_BUTTON_DOWN_TIME: AtomicI64 = AtomicI64::new(0);

        // KEY_POWER scan code = 116
        if code == 116 || android_code == android_codes::AKEYCODE_POWER as i32 {
            if value == 1 {
                // Button down - store time
                POWER_BUTTON_DOWN_TIME.store(time_sec, Ordering::Relaxed);
            } else if value == 0 {
                // Button up - check if held for 10+ seconds
                let down_time = POWER_BUTTON_DOWN_TIME.load(Ordering::Relaxed);
                if down_time > 0 && time_sec - down_time >= 10 {
                    // Kill system bridge
                    let _ = unsafe { bindings::evdev_callback_on_emergency_kill_system_bridge() };
                    process::exit(0);
                }
                POWER_BUTTON_DOWN_TIME.store(0, Ordering::Relaxed);
            }
        }
        false
    }
}

impl EvdevEventObserver for EvdevCallbackBinderObserver {
    fn on_event(&self, device_path: &str, event: &InputEvent) -> bool {
        // Extract event type and code from EventCode
        let (ev_type, ev_code) = event_code_to_int(&event.event_code);
        let code = ev_code as u32;

        // Convert raw evdev code to Android keycode
        let (android_code, _flags) = self.key_layout_map_manager.map_key(device_path, code);

        // Handle power button emergency kill
        self.handle_power_button(code, android_code, event.value, event.time.tv_sec as i64);

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
            bindings::evdev_callback_on_evdev_event(
                device_path_cstr.as_ptr(),
                event.time.tv_sec as i64,
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

impl Default for EvdevCallbackBinderObserver {
    fn default() -> Self {
        Self::new()
    }
}
