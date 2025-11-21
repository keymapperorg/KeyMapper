use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::observer::EvdevEventNotifier;
use evdev::util::event_code_to_int;
use evdev::{Device, ReadFlag, ReadStatus};
use mio::{event, Events, Interest, Poll, Token};
use std::collections::HashSet;
use std::error::Error;
use std::os::fd::AsRawFd;
use std::sync::{Arc, Mutex, OnceLock};
use mio::unix::SourceFd;

// TODO use Mutex to store tokens
const TOKEN: Token = Token(0);

pub fn start_event_loop(notifier: &EvdevEventNotifier) -> Result<(), Box<dyn Error>> {
    error!("START EVDEV EVENT LOOP");

    let mut poll = Poll::new()?;
    let mut events = Events::with_capacity(128);
    let evdev_device = Device::new_from_path("/dev/input/event12")?;

    let source = &mut SourceFd(&evdev_device.as_raw_fd());
    poll.registry().register(source, TOKEN, Interest::READABLE)?;

    // Read all available events from device
    loop {
        poll.poll(&mut events, None)?;

        for event in events.iter() {
            match evdev_device.next_event(ReadFlag::NORMAL) {
                Ok((ReadStatus::Success, event)) => {
                    error!("Evdev event: {:?}", event);
                }

                Ok((ReadStatus::Sync, event)) => {
                    loop {
                        match evdev_device.next_event(ReadFlag::NORMAL | ReadFlag::SYNC) {
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
                                return Err(EvdevError::from(e).into());
                            }
                        }
                    }
                }
                Err(_) => {
                    error!("EvdevManager: failed to get next event: {:?}", event);
                }
            }
        }
    }

    Ok(())
}

// fn process_next_event(device: Device, notifier: &mut EvdevEventNotifier) -> Result<(), EvdevError> {
//     match device.next_event(ReadFlag::NORMAL) {
//         Ok((ReadStatus::Success, event)) => {
//             // Notify all observers
//             let consumed = notifier.notify(device_path, &event);
//
//             // If no observer consumed the event, forward to uinput
//             if !consumed {
//                 // Extract event type and code from EventCode
//                 let (ev_type, ev_code) = event_code_to_int(&event.event_code);
//
//                 if let Err(e) = device.write_event(ev_type as u32, ev_code as u32, event.value)
//                 {
//                     error!("Failed to write event to uinput: {}", e);
//                 }
//             }
//         }
//         Ok((ReadStatus::Sync, _event)) => {
//             // Handle sync event - read sync events until EAGAIN
//             loop {
//                 match device.evdev.next_event(ReadFlag::NORMAL | ReadFlag::SYNC) {
//                     Ok((ReadStatus::Sync, _)) => {
//                         // Continue reading sync events
//                     }
//                     Ok((ReadStatus::Success, _)) => {
//                         // Sync complete, break inner loop
//                         break;
//                     }
//                     Err(e) => {
//                         // Check if it's EAGAIN (no more events)
//                         if let Some(err) = e.raw_os_error() {
//                             if err == -(libc::EAGAIN as i32) {
//                                 break;
//                             }
//                         }
//                         return Err(EvdevError::from(e));
//                     }
//                 }
//             }
//         }
//         Err(e) => {
//             // Check if it's EAGAIN (no more events available)
//             if let Some(err) = e.raw_os_error() {
//                 if err == -(libc::EAGAIN as i32) {
//                     // No more events available
//                     break;
//                 }
//             }
//             return Err(EvdevError::from(e));
//         }
//     }
// }

// Add a device to be handled by a Tokio task
// pub fn add_device(device_path: String, device: Arc<GrabbedDevice>) -> Result<(), EvdevError> {
//     let manager =
//         get_device_task_manager().ok_or_else(|| EvdevError::new(-(libc::EINVAL as i32)))?;
//
//     manager
//         .add_device(device_path, device)
//         .map_err(|e| EvdevError::new(-(libc::EINVAL as i32)))
// }
//
// /// Remove a device and cancel its task
// pub fn remove_device(device_path: &str) -> Result<(), EvdevError> {
//     let manager =
//         get_device_task_manager().ok_or_else(|| EvdevError::new(-(libc::EINVAL as i32)))?;
//
//     manager
//         .remove_device(device_path)
//         .map_err(|_| EvdevError::new(-(libc::ENODEV as i32)))
// }
//
// /// Remove all devices and cancel their tasks
// pub fn remove_all_devices() {
//     if let Some(manager) = get_device_task_manager() {
//         manager.remove_all_devices();
//     }
// }
//
// /// Write an event to a device
// pub fn write_event_to_device(
//     device_path: &str,
//     event_type: u32,
//     code: u32,
//     value: i32,
// ) -> Result<(), EvdevError> {
//     let manager =
//         get_device_task_manager().ok_or_else(|| EvdevError::new(-(libc::EINVAL as i32)))?;
//
//     manager
//         .write_event_to_device(device_path, event_type, code, value)
//         .map_err(|_| EvdevError::new(-(libc::ENODEV as i32)))
// }
//
// /// Get uinput device paths
// pub fn get_uinput_device_paths() -> HashSet<String> {
//     if let Some(manager) = get_device_task_manager() {
//         manager.get_uinput_device_paths()
//     } else {
//         HashSet::new()
//     }
// }
//
// /// Check if a device is already grabbed
// pub fn is_device_grabbed(device_path: &str) -> bool {
//     if let Some(manager) = get_device_task_manager() {
//         manager.is_device_grabbed(device_path)
//     } else {
//         false
//     }
// }
