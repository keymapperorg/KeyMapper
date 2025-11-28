use crate::device_identifier::DeviceIdentifier;
use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::runtime::get_runtime;
use evdev::util::event_code_to_int;
use evdev::{InputEvent, ReadFlag, ReadStatus};
use io::ErrorKind;
use mio::event::Event;
use mio::unix::SourceFd;
use mio::{Events, Interest, Poll, Registry, Token, Waker};
use slab::Slab;
use std::error::Error;
use std::fs::read_dir;
use std::io;
use std::os::fd::AsRawFd;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc, Mutex, OnceLock, RwLock};
use std::time::{Duration, Instant};
use tokio::task::JoinHandle;

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

const WAKER_TOKEN: Token = Token(0);

/// This callback returns true if the observer consumed the input event.
pub type EvdevObserver =
    fn(device_path: &str, device_id: &DeviceIdentifier, event: &InputEvent) -> bool;

pub struct EventLoopManager {
    stop_flag: Arc<AtomicBool>,
    poll: Arc<RwLock<Poll>>,
    registry: Arc<Registry>,
    join_handle: RwLock<Option<JoinHandle<()>>>,
    waker: Waker,
    observers: Arc<RwLock<Slab<EvdevObserver>>>,
    grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
}

impl EventLoopManager {
    pub fn get() -> &'static EventLoopManager {
        EVENT_LOOP_MANAGER.get_or_init(Self::new)
    }

    fn new() -> Self {
        let poll = Poll::new().unwrap();
        let registry = poll.registry().try_clone().unwrap();
        let waker = Waker::new(&registry, WAKER_TOKEN).unwrap();
        let poll_lock = Arc::new(RwLock::new(poll));

        EventLoopManager {
            stop_flag: Arc::new(AtomicBool::new(false)),
            poll: poll_lock,
            registry: Arc::new(registry),
            join_handle: RwLock::new(None),
            waker,
            observers: Arc::new(RwLock::new(Slab::with_capacity(16))),
            grabbed_devices: Arc::new(RwLock::new(Slab::with_capacity(32))),
        }
    }

    pub fn start(&self) -> Result<(), EvdevError> {
        let is_running = { self.join_handle.read().unwrap().is_some() };

        if is_running {
            // Do nothing. The event loop is already started.
            info!("EvdevManager event loop is already running");
            Ok(())
        } else {
            self.stop_flag.store(false, Ordering::Relaxed);

            let observers = self.observers.clone();
            let grabbed_devices = self.grabbed_devices.clone();

            let (tx, rx) = mpsc::channel();

            let poll_lock_clone = self.poll.clone();
            let stop_flag_clone = self.stop_flag.clone();

            let join_handle = get_runtime().spawn(async move {
                tx.send(()).unwrap();
                EventLoopThread::new(stop_flag_clone, poll_lock_clone, observers, grabbed_devices)
                    .start();
            });

            match rx.recv_timeout(Duration::from_secs(2)) {
                Ok(_) => {
                    self.join_handle.write().unwrap().replace(join_handle);

                    Ok(())
                }
                Err(e) => {
                    error!("Failed to wait for event loop start: {}", e);
                    join_handle.abort();
                    Err(EvdevError::new(-libc::ETIMEDOUT))
                }
            }
        }
    }

    pub fn stop(&self) -> Result<(), io::Error> {
        let handle_option = self.join_handle.write().unwrap().take();

        match handle_option {
            None => {
                error!("Event loop not running");
            }
            Some(handle) => {
                self.stop_flag.store(true, Ordering::Relaxed);
                self.waker.wake()?;

                // Wait for the loop to finish (with timeout)
                let start = Instant::now();
                while !handle.is_finished() {
                    if start.elapsed() > Duration::from_secs(2) {
                        error!("Event loop did not stop in time, aborting");
                        handle.abort();

                        return Err(io::Error::new(
                            ErrorKind::TimedOut,
                            "Event loop did not stop in time",
                        ));
                    }
                    std::thread::sleep(Duration::from_millis(10));
                }
            }
        }

        Ok(())
    }

    /// returns: The new list of grabbed devices.
    pub fn grab_device(
        &self,
        device_identifier: DeviceIdentifier,
    ) -> Result<Vec<(usize, DeviceIdentifier)>, Box<dyn Error>> {
        let mut devices = self.grabbed_devices.write().unwrap();

        // Check if device is already grabbed
        if devices
            .iter()
            .any(|(_, device)| device.device_id == device_identifier)
        {
            return Err(Box::new(io::Error::new(
                ErrorKind::AlreadyExists,
                format!("Device already grabbed: {}", device_identifier),
            )));
        }

        let device = GrabbedDevice::new(path)?;
        let fd = device.evdev.lock().unwrap().as_raw_fd();
        let key = devices.insert(device);

        let mut source_fd = SourceFd(&fd);

        // Register with key + 1 because 0 is reserved for the waker
        self.registry
            .register(&mut source_fd, Token(key + 1), Interest::READABLE)
            .inspect_err(|e| {
                // Remove device on registration failure
                devices.remove(key);
                error!("Failed to register device {}: {}", path, e);
            })?;

        info!("Grabbed device: {}", path);

        let grabbed_devices_result = devices
            .iter()
            .map(|(key, device)| (key, device.device_id))
            .collect();
        Ok(grabbed_devices_result)
    }

    pub fn ungrab_device(&self, path: &str) -> Result<(), io::Error> {
        let mut devices = self.grabbed_devices.write().unwrap();

        let device_entry = devices
            .iter()
            .find(|(_, device)| device.device_path == path);

        match device_entry {
            None => Err(io::Error::new(
                ErrorKind::NotFound,
                format!("Device not found: {}", path),
            )),
            Some((key, device)) => {
                let fd = device.evdev.lock().unwrap().as_raw_fd();
                let mut source_fd = SourceFd(&fd);

                self.registry
                    .deregister(&mut source_fd)
                    .inspect_err(|e| error!("Failed to deregister device {}: {}", path, e))
                    .ok();

                // Remove device (Drop will ungrab it)
                devices.remove(key);
                info!("Ungrabbed device: {}", path);
                Ok(())
            }
        }
    }

    pub fn ungrab_all_devices(&self) -> Result<(), Box<dyn Error>> {
        let mut devices = self.grabbed_devices.write().unwrap();

        // Deregister all devices from the registry
        for (_, device) in devices.iter() {
            let fd = device.evdev.lock().unwrap().as_raw_fd();
            let mut source_fd = SourceFd(&fd);

            self.registry
                .deregister(&mut source_fd)
                .inspect_err(|e| {
                    error!("Failed to deregister device {}: {}", device.device_path, e)
                })
                .ok();
        }

        // Clear all devices (Drop will ungrab each one)
        devices.clear();
        info!("Ungrabbed all devices");
        Ok(())
    }

    pub fn register_observer(&self, observer: EvdevObserver) {
        self.observers.write().unwrap().insert(observer);
    }

    /// Get the paths to all the real (non uinput) connected devices.
    pub fn get_all_real_devices(&self) -> Result<Vec<PathBuf>, EvdevError> {
        let mut paths: Vec<PathBuf> = Vec::new();

        let dir = read_dir("/dev/input")?;
        let grabbed_devices = self.grabbed_devices.read().unwrap();
        let uinput_paths: Vec<&str> = grabbed_devices
            .iter()
            .map(|(_, device)| device.uinput.devnode().unwrap())
            .collect();

        for entry_result in dir {
            match entry_result {
                Ok(entry) => {
                    let path = entry.path();

                    // Do not return paths to uinput devices that were created.
                    if uinput_paths.contains(&path.to_str().unwrap()) {
                        debug!("Skipping uinput device: {:?}", path);
                        continue;
                    }

                    paths.push(path);
                }
                Err(_) => {
                    debug!(
                        "Failed to read /dev/input entry: {}",
                        entry_result.unwrap_err()
                    );
                }
            }
        }

        debug!("EvdevManager: get real devices: {:?}", paths);
        Ok(paths)
    }
}

struct EventLoopThread {
    stop_flag: Arc<AtomicBool>,
    poll: Arc<RwLock<Poll>>,
    observers: Arc<RwLock<Slab<EvdevObserver>>>,
    grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
}

impl EventLoopThread {
    pub fn new(
        stop_flag: Arc<AtomicBool>,
        poll: Arc<RwLock<Poll>>,
        observers: Arc<RwLock<Slab<EvdevObserver>>>,
        grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
    ) -> Self {
        EventLoopThread {
            stop_flag,
            poll,
            observers,
            grabbed_devices,
        }
    }

    /// This blocks until the stop flag is set.
    fn start(&self) {
        let mut events = Events::with_capacity(128);

        info!("Started evdev event loop");

        'main: loop {
            let mut poll = self.poll.write().unwrap();

            match poll.poll(&mut events, None) {
                Ok(_) => {
                    for event in events.iter() {
                        // Break out of the loop if the stop flag is set.
                        if event.token() == WAKER_TOKEN && self.stop_flag.load(Ordering::SeqCst) {
                            info!("Received waker token. Stopping evdev event loop");
                            break 'main;
                        }

                        self.on_poll_event(event);
                    }
                }
                Err(e) if e.kind() == ErrorKind::Interrupted => {
                    // Interrupted, continue polling
                    continue;
                }
                Err(e) => {
                    error!("EvdevManager poll error. Stopping loop: {}", e);
                    break;
                }
            }
        }

        info!("Stopped evdev event loop");
    }

    fn on_poll_event(&self, event: &Event) {
        let Token(key) = event.token();
        // Subtract 1 because Token(0) is reserved
        let slab_key = key - 1;

        let devices = self.grabbed_devices.read().unwrap();

        let grabbed_device = match devices.get(slab_key) {
            Some(device) => device,
            None => {
                debug!("Device with key {} no longer exists", slab_key);
                return;
            }
        };

        let evdev = grabbed_device.evdev.lock().unwrap();
        let mut flags: ReadFlag = ReadFlag::NORMAL;

        loop {
            match evdev.next_event(flags) {
                Ok((ReadStatus::Success, input_event)) => {
                    flags = ReadFlag::NORMAL;
                    // Keep this logging line. Debug/verbose events will be disabled in production.
                    debug!("Evdev event: {:?}", input_event);
                    self.process_observers(&input_event, grabbed_device);
                }
                Ok((ReadStatus::Sync, _event)) => {
                    // Continue reading sync events
                    flags = ReadFlag::NORMAL | ReadFlag::SYNC;
                }
                Err(_error) => {
                    // Break if it's EAGAIN (no more events) or any other error.
                    // Do not log these errors because it is expected
                    break;
                }
            }
        }
    }

    fn process_observers(&self, event: &InputEvent, grabbed_device: &GrabbedDevice) {
        let mut consume = false;

        for (_, observer) in self.observers.read().unwrap().iter() {
            if observer(
                &grabbed_device.device_path,
                &grabbed_device.device_id,
                event,
            ) {
                consume = true;
            }
        }

        if !consume {
            let (event_type, event_code) = event_code_to_int(&event.event_code);
            grabbed_device
                .write_event(event_type, event_code, event.value)
                .inspect_err(|e| {
                    error!(
                        "Failed to passthrough event to {}. Event: {:?}. Error: {:?}",
                        grabbed_device.device_path, event, e
                    )
                })
                .ok();
        }
    }
}
