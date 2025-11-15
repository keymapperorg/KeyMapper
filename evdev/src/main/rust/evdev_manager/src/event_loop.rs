use crate::device_manager::DeviceContext;
use crate::device_manager_tokio::DeviceTaskManager;
use crate::evdev_error::EvdevError;
use crate::observer::EvdevEventNotifier;
use std::collections::HashSet;
use std::sync::{Arc, Mutex, OnceLock};

/// Global device task manager
static DEVICE_TASK_MANAGER: OnceLock<Arc<Mutex<Option<Arc<DeviceTaskManager>>>>> = OnceLock::new();

/// Initialize the device task manager
pub fn init_device_task_manager(notifier: Arc<EvdevEventNotifier>) -> Result<(), EvdevError> {
    let manager = Arc::new(DeviceTaskManager::new(notifier));
    let global = DEVICE_TASK_MANAGER.get_or_init(|| Arc::new(Mutex::new(None)));
    let mut guard = global.lock().unwrap();

    if guard.is_some() {
        return Err(EvdevError::new(-(libc::EBUSY as i32)));
    }

    *guard = Some(manager);
    Ok(())
}

/// Get the device task manager
fn get_device_task_manager() -> Option<Arc<DeviceTaskManager>> {
    let global = DEVICE_TASK_MANAGER.get()?;
    let guard = global.lock().ok()?;
    guard.clone()
}

/// Add a device to be handled by a Tokio task
pub fn add_device(device_path: String, device: Arc<DeviceContext>) -> Result<(), EvdevError> {
    let manager =
        get_device_task_manager().ok_or_else(|| EvdevError::new(-(libc::EINVAL as i32)))?;

    manager
        .add_device(device_path, device)
        .map_err(|e| EvdevError::new(-(libc::EINVAL as i32)))
}

/// Remove a device and cancel its task
pub fn remove_device(device_path: &str) -> Result<(), EvdevError> {
    let manager =
        get_device_task_manager().ok_or_else(|| EvdevError::new(-(libc::EINVAL as i32)))?;

    manager
        .remove_device(device_path)
        .map_err(|_| EvdevError::new(-(libc::ENODEV as i32)))
}

/// Remove all devices and cancel their tasks
pub fn remove_all_devices() {
    if let Some(manager) = get_device_task_manager() {
        manager.remove_all_devices();
    }
}

/// Write an event to a device
pub fn write_event_to_device(
    device_path: &str,
    event_type: u32,
    code: u32,
    value: i32,
) -> Result<(), EvdevError> {
    let manager =
        get_device_task_manager().ok_or_else(|| EvdevError::new(-(libc::EINVAL as i32)))?;

    manager
        .write_event_to_device(device_path, event_type, code, value)
        .map_err(|_| EvdevError::new(-(libc::ENODEV as i32)))
}

/// Get uinput device paths
pub fn get_uinput_device_paths() -> HashSet<String> {
    if let Some(manager) = get_device_task_manager() {
        manager.get_uinput_device_paths()
    } else {
        HashSet::new()
    }
}

/// Check if a device is already grabbed
pub fn is_device_grabbed(device_path: &str) -> bool {
    if let Some(manager) = get_device_task_manager() {
        manager.is_device_grabbed(device_path)
    } else {
        false
    }
}
