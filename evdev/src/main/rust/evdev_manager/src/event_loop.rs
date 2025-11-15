use crate::bindings;
use crate::device_manager::DeviceContext;
use crate::evdev::{EvdevError, EvdevEvent};
use crate::observer::EvdevEventNotifier;
use nix::sys::epoll::{Epoll, EpollCreateFlags, EpollEvent, EpollFlags};
use nix::sys::eventfd::{eventfd, EfdFlags};
use nix::unistd::read;
use std::collections::{HashMap, HashSet};
use std::mem;
use std::os::fd::{AsRawFd, OwnedFd, RawFd};
use std::slice;
use std::sync::{Arc, Mutex, OnceLock};
use std::thread;

const MAX_EPOLL_EVENTS: usize = 100;

enum Command {
    Stop,
}

/// Event loop state
struct EventLoopState {
    epoll: Option<Arc<Epoll>>,
    command_event_fd: Option<OwnedFd>,
    devices: HashMap<String, Arc<DeviceContext>>,
    fd_to_device_path: HashMap<RawFd, String>,
    command_queue: Vec<Command>,
    running: bool,
}

impl EventLoopState {
    fn new() -> Self {
        Self {
            epoll: None,
            command_event_fd: None,
            devices: HashMap::new(),
            fd_to_device_path: HashMap::new(),
            command_queue: Vec::new(),
            running: false,
        }
    }
}

/// Global event loop state
pub(crate) static EVENT_LOOP_STATE: Mutex<EventLoopState> = Mutex::new(EventLoopState::new());

/// Initialize logger if not already initialized
fn init_logger() {
    static LOGGER_INIT: OnceLock<()> = OnceLock::new();
    
    LOGGER_INIT.get_or_init(|| {
        android_log::init("KeyMapperSystemBridge").unwrap_or_else(|e| {
            eprintln!("Failed to initialize Android logger: {:?}", e);
        });
    });
}

/// Start the evdev event loop
pub fn start_event_loop(
    notifier: Arc<EvdevEventNotifier>,
) -> Result<(), EvdevError> {
    init_logger();
    
    let mut state = EVENT_LOOP_STATE.lock().unwrap();
    
    if state.running {
        error!("The evdev event loop has already started.");
        return Err(EvdevError::new(-(nix::errno::Errno::EBUSY as i32)));
    }

    // Create epoll instance
    let epoll = Epoll::new(EpollCreateFlags::EPOLL_CLOEXEC)
        .map_err(|e| EvdevError::new(-(e as i32)))?;

    // Create command eventfd
    let command_event_fd = eventfd(0, EfdFlags::EFD_CLOEXEC | EfdFlags::EFD_NONBLOCK)
        .map_err(|e| EvdevError::new(-(e as i32)))?;
    let command_event_fd = unsafe { OwnedFd::from_raw_fd(command_event_fd) };

    // Add command eventfd to epoll
    let event = EpollEvent::new(EpollFlags::EPOLLIN, command_event_fd.as_raw_fd() as u64);
    epoll.add(&command_event_fd, event)
        .map_err(|e| EvdevError::new(-(e as i32)))?;

    state.epoll = Some(Arc::new(epoll));
    state.command_event_fd = Some(command_event_fd);
    state.running = true;

    drop(state);

    info!("Start evdev event loop");

    // Notify observers that event loop started
    // This is done synchronously before starting the loop
    // The actual observer registration happens via JNI

    // Spawn event loop thread
    thread::spawn(move || {
        event_loop_thread(notifier);
    });

    Ok(())
}

/// Stop the evdev event loop
pub fn stop_event_loop() {
    let mut state = EVENT_LOOP_STATE.lock().unwrap();
    
    if !state.running {
        return;
    }

    // Add stop command to queue
    state.command_queue.push(Command::Stop);

    // Notify event loop via eventfd
    if let Some(ref command_fd) = state.command_event_fd {
        let val: u64 = 1;
        let _ = nix::unistd::write(command_fd.as_raw_fd(), unsafe {
            slice::from_raw_parts(
                &val as *const u64 as *const u8,
                mem::size_of::<u64>(),
            )
        });
    }
}

/// Main event loop thread
fn event_loop_thread(notifier: Arc<EvdevEventNotifier>) {
    init_logger();
    
    let mut events = vec![EpollEvent::empty(); MAX_EPOLL_EVENTS];
    let mut running = true;

    // Get a reference to epoll that we'll use in the loop
    let epoll_arc = {
        let state = EVENT_LOOP_STATE.lock().unwrap();
        state.epoll.clone()
    };

    let epoll_arc = match epoll_arc {
        Some(ep) => ep,
        None => {
            error!("Epoll not initialized");
            return;
        }
    };

    while running {
        match epoll_arc.wait(&mut events, -1) {
            Ok(n) => {
                for i in 0..n {
                    let event = &events[i];
                    let fd_raw = event.data() as RawFd;

                    let mut state = EVENT_LOOP_STATE.lock().unwrap();
                    let command_fd = state.command_event_fd.as_ref().map(|f| f.as_raw_fd());

                    if Some(fd_raw) == command_fd {
                        // Handle command
                        let mut val: u64 = 0;
                        // nix::unistd::read takes RawFd, which is safe here since we own the command_event_fd
                        let _ = read(fd_raw, unsafe {
                            slice::from_raw_parts_mut(
                                &mut val as *mut u64 as *mut u8,
                                mem::size_of::<u64>(),
                            )
                        });

                        // Process commands
                        let commands: Vec<Command> = state.command_queue.drain(..).collect();
                        drop(state);

                        for cmd in commands {
                            match cmd {
                                Command::Stop => {
                                    running = false;
                                    break;
                                }
                            }
                        }
                    } else {
                        // Check for device disconnection
                        let events_flags = event.events();
                        if events_flags.contains(EpollFlags::EPOLLHUP) || events_flags.contains(EpollFlags::EPOLLERR) {
                            info!("Device disconnected, removing from epoll");
                            let mut state = EVENT_LOOP_STATE.lock().unwrap();
                            if let Some(ref epoll) = state.epoll {
                                // Get the device to remove it from epoll
                                if let Some(path) = state.fd_to_device_path.get(&fd_raw) {
                                    if let Some(device) = state.devices.get(path) {
                                        let _ = epoll.delete(&device.fd_borrowed());
                                    }
                                }
                            }
                            if let Some(path) = state.fd_to_device_path.remove(&fd_raw) {
                                state.devices.remove(&path);
                            }
                        } else {
                            // Handle device event
                            let device_path = state.fd_to_device_path.get(&fd_raw).cloned();
                            drop(state);

                            if let Some(path) = device_path {
                                if let Err(e) = handle_device_event(&path, &notifier) {
                                    error!("Error handling device event: {}", e);
                                    // If callback fails, stop the loop
                                    running = false;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            Err(e) => {
                error!("epoll wait failed: {}", e);
                running = false;
                break;
            }
        }
    }

    // Cleanup
    cleanup_event_loop();
    info!("Stopped evdev event loop");
}

/// Handle an event from a device
fn handle_device_event(
    device_path: &str,
    notifier: &EvdevEventNotifier,
) -> Result<(), EvdevError> {
    let state = EVENT_LOOP_STATE.lock().unwrap();
    let device = state.devices.get(device_path);

    let device = match device {
        Some(d) => d,
        None => return Ok(()), // Device was removed
    };
    let mut input_event = bindings::input_event {
        time: bindings::timeval { tv_sec: 0, tv_usec: 0 },
        type_: 0,
        code: 0,
        value: 0,
    };

    // Read events from device
    loop {
        let result = unsafe {
            bindings::libevdev_next_event(
                device.evdev.as_ptr(),
                bindings::libevdev_read_flag_LIBEVDEV_READ_FLAG_NORMAL,
                &mut input_event,
            )
        };

        if result < 0 {
            if result == -(nix::errno::Errno::EAGAIN as i32) {
                // No more events
                break;
            } else {
                return Err(EvdevError::new(result));
            }
        }

        if result == bindings::libevdev_read_status_LIBEVDEV_READ_STATUS_SUCCESS {
            // Create event for observers
            let event = EvdevEvent {
                time_sec: input_event.time.tv_sec,
                time_usec: input_event.time.tv_usec,
                event_type: crate::enums::EventType::from_raw(input_event.type_ as u32)
                    .unwrap_or(crate::enums::EventType::Syn),
                code: input_event.code as u32,
                value: input_event.value,
            };

            // Notify all observers
            let consumed = notifier.notify(device_path, &event);

            // If no observer consumed the event, forward to uinput
            if !consumed {
                device.write_event(
                    event.event_type.as_raw(),
                    event.code,
                    event.value,
                )?;
            }
        } else if result == bindings::libevdev_read_status_LIBEVDEV_READ_STATUS_SYNC {
            // Handle sync event
            let sync_result = unsafe {
                bindings::libevdev_next_event(
                    device.evdev.as_ptr(),
                    bindings::libevdev_read_flag_LIBEVDEV_READ_FLAG_NORMAL
                        | bindings::libevdev_read_flag_LIBEVDEV_READ_FLAG_SYNC,
                    &mut input_event,
                )
            };
            if sync_result < 0 && sync_result != -(nix::errno::Errno::EAGAIN as i32) {
                return Err(EvdevError::new(sync_result));
            }
        }
    }

    Ok(())
}

/// Cleanup event loop resources
fn cleanup_event_loop() {
    let mut state = EVENT_LOOP_STATE.lock().unwrap();

    // Cleanup all devices
    state.devices.clear();
    state.fd_to_device_path.clear();

    // Close file descriptors
    state.epoll = None;
    state.command_event_fd = None;
    state.running = false;
}

/// Add a device to the event loop
pub fn add_device(device_path: String, device: Arc<DeviceContext>) -> Result<(), EvdevError> {
    let mut state = EVENT_LOOP_STATE.lock().unwrap();

    if !state.running {
        return Err(EvdevError::new(-(nix::errno::Errno::EINVAL as i32)));
    }

    let epoll = state.epoll.as_ref().ok_or_else(|| {
        EvdevError::new(-(nix::errno::Errno::EINVAL as i32))
    })?;

    let fd_raw = device.fd();
    let event = EpollEvent::new(EpollFlags::EPOLLIN, fd_raw as u64);
    
    epoll.add(&device.fd_borrowed(), event)
        .map_err(|e| EvdevError::new(-(e as i32)))?;

    state.fd_to_device_path.insert(fd_raw, device_path.clone());
    state.devices.insert(device_path, device);

    Ok(())
}

/// Remove a device from the event loop
pub fn remove_device(device_path: &str) -> Result<(), EvdevError> {
    let mut state = EVENT_LOOP_STATE.lock().unwrap();

    let device = state.devices.remove(device_path);
    if let Some(device) = device {
        let fd_raw = device.fd();
        state.fd_to_device_path.remove(&fd_raw);

        if let Some(ref epoll) = state.epoll {
            let _ = epoll.delete(&device.fd_borrowed());
        }
    }

    Ok(())
}

/// Remove all devices from the event loop
pub fn remove_all_devices() {
    let mut state = EVENT_LOOP_STATE.lock().unwrap();

    if let Some(ref epoll) = state.epoll {
        // Remove all devices from epoll
        for (path, device) in &state.devices {
            let _ = epoll.delete(&device.fd_borrowed());
        }
    }

    state.devices.clear();
    state.fd_to_device_path.clear();
}

/// Write an event to a device
pub fn write_event_to_device(device_path: &str, event_type: u32, code: u32, value: i32) -> Result<(), EvdevError> {
    let state = EVENT_LOOP_STATE.lock().unwrap();
    if let Some(device) = state.devices.get(device_path) {
        device.write_event(event_type, code, value)
    } else {
        Err(EvdevError::new(-(nix::errno::Errno::ENODEV as i32)))
    }
}

/// Get uinput device paths
pub fn get_uinput_device_paths() -> HashSet<String> {
    let state = EVENT_LOOP_STATE.lock().unwrap();
    state.devices
        .values()
        .filter_map(|d| d.uinput.devnode())
        .collect()
}

/// Check if a device is already grabbed
pub fn is_device_grabbed(device_path: &str) -> bool {
    let state = EVENT_LOOP_STATE.lock().unwrap();
    state.devices.contains_key(device_path)
}

