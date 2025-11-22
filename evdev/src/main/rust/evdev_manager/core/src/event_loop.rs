use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::observer::EvdevEventNotifier;
use crate::runtime::get_runtime;
use evdev::{Device, GrabMode, ReadFlag, ReadStatus};
use mio::event::{Event, Source};
use mio::unix::SourceFd;
use mio::{Events, Interest, Poll, Registry, Token, Waker};
use slab::Slab;
use std::collections::VecDeque;
use std::error::Error;
use std::os::fd::AsRawFd;
use std::sync::mpsc::Receiver;
use std::sync::{mpsc, Arc, LazyLock, LockResult, Mutex, OnceLock, PoisonError, RwLock};
use tokio::task::JoinHandle;

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

/// This uses the Waker and command queue pattern for updating the grabbed devices
/// because the libevdev struct can not be safely shared between threads. Otherwise, the
/// grabbed_devices slab would be stored here and an EventLoop struct wouldn't be necessary.
/// This also has the benefit that the loop can be interrupted gracefully without having to
/// kill it.
pub struct EventLoopManager {
    loop_handle: Mutex<Option<JoinHandle<()>>>,
    waker: Arc<Mutex<Option<Waker>>>,
    command_queue: Arc<Mutex<VecDeque<Command>>>,
}

const TOKEN_COMMAND: Token = Token(0);

impl EventLoopManager {
    pub fn get() -> &'static EventLoopManager {
        EVENT_LOOP_MANAGER.get_or_init(Self::new)
    }

    fn new() -> Self {
        EventLoopManager {
            loop_handle: Mutex::new(None),
            waker: Arc::new(Mutex::new(None)),
            command_queue: Arc::new(Mutex::new(VecDeque::with_capacity(64))),
        }
    }

    pub fn start(&self, notifier: &'static EvdevEventNotifier) -> Result<(), EvdevError> {
        let mut handle_option = self.loop_handle.lock().unwrap();

        match *handle_option {
            Some(_) => {
                // Do nothing. The event loop is already started.
                info!("EvdevManager event loop is already running");
                Ok(())
            }

            None => {
                let waker = self.waker.clone();
                let command_queue = self.command_queue.clone();

                let (tx, rx) = mpsc::channel();

                let handle = get_runtime().spawn(async move {
                    // TODO does logging still work across threads or does another need to be installed?
                    let poll = Poll::new().unwrap();
                    *waker.lock().unwrap() =
                        Some(Waker::new(poll.registry(), TOKEN_COMMAND).unwrap());

                    if let Err(e) = tx.send(()) {
                        error!("Failed to signal event loop start: {}", e);
                    }

                    EventLoop::new(poll).start(&command_queue, notifier);
                });

                get_runtime().spawn(async {});

                *handle_option = Some(handle);

                match rx.recv_timeout(std::time::Duration::from_secs(2)) {
                    Ok(_) => Ok(()),
                    Err(e) => {
                        error!("Failed to wait for event loop start: {}", e);
                        if let Some(handle) = handle_option.take() {
                            handle.abort();
                        }
                        Err(EvdevError::new(-libc::ETIMEDOUT))
                    }
                }
            }
        }
    }

    pub fn stop(&self) -> Result<(), Box<dyn Error>> {
        let handle_option = self
            .loop_handle
            .lock()
            .inspect_err(|e| error!("Failed to get loop handle: {}", e))
            .unwrap()
            .take();

        match handle_option {
            None => {}
            Some(handle) => {
                self.send_command(Command::StopLoop)?;

                // TODO does this block, join until finished? Test by not sending stop loop command.
                handle.abort();
            }
        }

        let queue_option = self
            .command_queue
            .lock()
            .inspect_err(|e| error!("Failed to get command queue: {}", e))
            .ok();

        if let Some(mut queue) = queue_option {
            queue.clear();
        }

        let waker_option = self
            .waker
            .lock()
            .inspect_err(|e| error!("Failed to get loop handle: {}", e))
            .ok();

        if let Some(mut waker) = waker_option {
            waker.take();
        }

        Ok(())
    }

    pub fn grab_device(&self, path: &str) -> Result<(), Box<dyn Error>> {
        self.send_command(Command::GrabDevice {
            path: path.to_string(),
        })
    }

    fn send_command(&self, command: Command) -> Result<(), Box<dyn Error>> {
        error!("Sending command: {:?}", command);
        match self.waker.lock().unwrap().as_ref() {
            None => {
                error!("EvdevManager event loop is not running");
            }
            Some(waker) => {
                self.command_queue.lock().unwrap().push_back(command);
                waker.wake()?
            }
        }

        Ok(())
    }
}

struct EventLoop {
    poll: Poll,
    grabbed_devices: Slab<GrabbedDevice>,
}

impl EventLoop {
    fn new(poll: Poll) -> Self {
        EventLoop {
            poll,
            grabbed_devices: Slab::with_capacity(32),
        }
    }

    /// This blocks until a Stop command is sent in the command queue.
    fn start(
        &mut self,
        command_queue: &Mutex<VecDeque<Command>>,
        evdev_event_notifier: &EvdevEventNotifier,
    ) {
        info!("Start evdev event loop");

        let mut events = Events::with_capacity(128);

        'main_loop: loop {
            match self.poll.poll(&mut events, None) {
                Ok(_) => {
                    for event in events.iter() {
                        if self.on_poll_event(event, command_queue) {
                            break 'main_loop;
                        }
                    }
                }
                Err(e) => {
                    error!("EvdevManager poll error. Stopping loop: {}", e);
                    self.stop();
                    break 'main_loop;
                }
            }
        }
    }

    fn stop(&mut self) {
        for (_, device) in self.grabbed_devices.iter_mut() {
            let mut source_fd = SourceFd(&device.evdev.as_raw_fd());

            // Do not unwrap here so that other devices can be ungrabbed.
            source_fd
                .deregister(self.poll.registry())
                .inspect_err(|e| {
                    error!("Failed to deregister device {}: {}", device.device_path, e)
                })
                .ok();

            device
                .evdev
                .grab(GrabMode::Ungrab)
                .inspect_err(|e| error!("Failed to ungrab device {}: {}", device.device_path, e))
                .ok();
        }

        self.grabbed_devices.clear();
        info!("Stopped evdev event loop");
    }

    fn ungrab_device(&mut self) {}

    fn grab_device(&mut self, path: &str) -> std::io::Result<()> {
        let device = GrabbedDevice::new(path)?;
        let key = self.grabbed_devices.insert(device);

        let stored_device = self.grabbed_devices.get(key).unwrap();
        let mut source_fd = SourceFd(&stored_device.evdev.as_raw_fd());

        source_fd.register(self.poll.registry(), Token(key), Interest::READABLE)
    }

    /// Returns whether to stop the loop.
    fn on_poll_event(&mut self, event: &Event, command_queue: &Mutex<VecDeque<Command>>) -> bool {
        match event.token() {
            TOKEN_COMMAND => {
                for command in command_queue.lock().unwrap().iter() {
                    match command {
                        Command::StopLoop => {
                            self.stop();
                            return true;
                        }

                        Command::GrabDevice { path } => {
                            self.grab_device(path)
                                .inspect_err(|e| error!("Failed to grab device {}: {}", path, e))
                                .ok();
                        }

                        Command::UngrabDevice { path } => {}

                        Command::UngrabAllDevices => {}
                    }
                }
            }

            Token(key) => {
                let grabbed_device = self.grabbed_devices.get(key).unwrap();
                self.read_evdev_events(&grabbed_device.evdev);
            }
        }

        false
    }

    fn read_evdev_events(&self, device: &Device) {
        match device.next_event(ReadFlag::NORMAL) {
            Ok((ReadStatus::Success, event)) => {
                // TODO call notifier
                error!("Evdev event: {:?}", event);
            }

            Ok((ReadStatus::Sync, _)) => {
                loop {
                    match device.next_event(ReadFlag::NORMAL | ReadFlag::SYNC) {
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
                            let evdev_error = EvdevError::from(e);

                            error!("Evdev error: {}", evdev_error);
                        }
                    }
                }
            }

            Err(e) => {
                error!(
                    "EvdevManager: failed to read next event for device: {:?}, error: {}",
                    device, e
                );
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
