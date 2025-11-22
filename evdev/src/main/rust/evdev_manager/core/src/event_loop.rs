use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::observer::EvdevEventNotifier;
use crate::runtime::get_runtime;
use evdev::{ReadFlag, ReadStatus};
use mio::event::Event;
use mio::{Events, Interest, Poll, Registry, Token, Waker};
use slab::Slab;
use std::collections::VecDeque;
use std::error::Error;
use std::sync::mpsc::Receiver;
use std::sync::{mpsc, Arc, LazyLock, LockResult, Mutex, OnceLock, RwLock};
use tokio::task::JoinHandle;

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

pub struct EventLoopManager {
    notifier: EvdevEventNotifier,
    loop_handle: Mutex<Option<JoinHandle<()>>>,
    waker: OnceLock<Waker>,
    command_queue: Mutex<VecDeque<Command>>,
}

const TOKEN_COMMAND: Token = Token(0);

impl EventLoopManager {
    pub fn get() -> Option<&'static EventLoopManager> {
        EVENT_LOOP_MANAGER.get()
    }

    pub fn init(notifier: EvdevEventNotifier) -> &'static EventLoopManager {
        EVENT_LOOP_MANAGER.get_or_init(|| Self::new(notifier))
    }

    fn new(notifier: EvdevEventNotifier) -> Self {
        EventLoopManager {
            notifier,
            loop_handle: Mutex::new(None),
            waker: OnceLock::new(),
            command_queue: Mutex::new(VecDeque::with_capacity(32)),
        }
    }

    pub fn start(self) -> Result<(), Box<dyn Error>> {
        match self.loop_handle.lock() {
            Ok(guard) => match *(guard) {
                Some(handle) => {
                    // Do nothing. The event loop is already started.
                }

                None => {}
            },
            Err(err) => {
                return Err(Box::new(err));
            }
        }

        self.loop_handle.get_or_init(|| {
            get_runtime().spawn(async move {
                // TODO does logging still work across threads or does another need to be installed?
                let poll = Poll::new().unwrap();
                self.waker
                    .get_or_init(|| Waker::new(poll.registry(), TOKEN_COMMAND).unwrap());

                EventLoop::new().start(poll, &self.command_queue);
            })
        });

        Ok(())
    }

    pub fn stop(self) {
        match self.loop_handle.get() {}
    }

    pub fn grab_device(self, path: &str) {}
}

struct EventLoop {
    grabbed_devices: Slab<GrabbedDevice>,
}

impl EventLoop {
    fn new() -> Self {
        EventLoop {
            grabbed_devices: Slab::with_capacity(32),
        }
    }

    /// This blocks until a Stop command is sent in the command queue.
    fn start(&mut self, mut poll: Poll, command_queue: &Mutex<VecDeque<Command>>) {
        info!("Start evdev event loop");

        let mut events = Events::with_capacity(128);

        'main_loop: loop {
            poll.poll(&mut events, None).unwrap();

            for event in events.iter() {
                if self.on_poll_event(event, command_recv) {
                    break 'main_loop;
                }
            }
        }
    }

    fn stop() {
        // TODO
        // Ungrab all devices
        // Stop the loop
        // Stop the thread
    }

    /// Returns whether to stop the loop.
    fn on_poll_event(&mut self, event: &Event, command_recv: &Receiver<Command>) -> bool {
        match event.token() {
            TOKEN_COMMAND => {
                for command in command_recv.iter() {
                    match command {
                        Command::StopLoop => {
                            return true;
                        }
                        Command::GrabDevice { path } => {
                            let device = GrabbedDevice::new(path.as_str()).unwrap();
                            self.grabbed_devices.insert(device);
                        }
                        Command::UngrabDevice { path } => {}
                        Command::UngrabAllDevices => {}
                    }
                }
            }
            Token(n) => {}
        }

        false
    }
}

// TODO remove
pub fn start_event_loop(notifier: &EvdevEventNotifier) -> Result<(), Box<dyn Error>> {
    info!("Start evdev event loop");

    let mut poll = Poll::new()?;
    WAKER.get_or_init(|| Waker::new(poll.registry(), TOKEN_COMMAND).unwrap());

    let mut events = Events::with_capacity(128);

    // Read all available events from device
    loop {
        poll.poll(&mut events, None)?;

        for event in events.iter() {
            match event.token() {
                TOKEN_COMMAND => {}
            }

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

#[derive(Debug, Clone)]
enum Command {
    StopLoop,

    GrabDevice { path: String },

    UngrabDevice { path: String },

    UngrabAllDevices,
}
