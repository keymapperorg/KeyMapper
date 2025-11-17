use crate::device_manager_tokio::DeviceTaskManager;
use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::observer::EvdevEventNotifier;
use evdev::util::event_code_to_int;
use evdev::{ReadFlag, ReadStatus};
use std::collections::HashSet;
use std::sync::{Arc, Mutex, OnceLock};



/// Read and process events from a device
fn start_event_loop(notifier: &EvdevEventNotifier) -> Result<(), EvdevError> {
    // Read all available events from device
    loop {
        match device.evdev.next_event(ReadFlag::NORMAL) {
            Ok((ReadStatus::Success, event)) => {
                // Notify all observers
                let consumed = notifier.notify(device_path, &event);

                // If no observer consumed the event, forward to uinput
                if !consumed {
                    // Extract event type and code from EventCode
                    let (ev_type, ev_code) = event_code_to_int(&event.event_code);

                    if let Err(e) = device.write_event(ev_type as u32, ev_code as u32, event.value)
                    {
                        error!("Failed to write event to uinput: {}", e);
                    }
                }
            }
            Ok((ReadStatus::Sync, _event)) => {
                // Handle sync event - read sync events until EAGAIN
                loop {
                    match device.evdev.next_event(ReadFlag::NORMAL | ReadFlag::SYNC) {
                        Ok((ReadStatus::Sync, _)) => {
                            // Continue reading sync events
                        }
                        Ok((ReadStatus::Success, _)) => {
                            // Sync complete, break inner loop
                            break;
                        }
                        Err(e) => {
                            // Check if it's EAGAIN (no more events)
                            if let Some(err) = e.raw_os_error() {
                                if err == -(libc::EAGAIN as i32) {
                                    break;
                                }
                            }
                            return Err(EvdevError::from(e));
                        }
                    }
                }
            }
            Err(e) => {
                // Check if it's EAGAIN (no more events available)
                if let Some(err) = e.raw_os_error() {
                    if err == -(libc::EAGAIN as i32) {
                        // No more events available
                        break;
                    }
                }
                return Err(EvdevError::from(e));
            }
        }
    }

    Ok(())
}

/// Add a device to be handled by a Tokio task
pub fn add_device(device_path: String, device: Arc<GrabbedDevice>) -> Result<(), EvdevError> {
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
