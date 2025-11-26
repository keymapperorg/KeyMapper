use crate::device_identifier::DeviceIdentifier;
use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::runtime::get_runtime;
use evdev::util::event_code_to_int;
use evdev::{InputEvent, ReadFlag, ReadStatus};
use io::ErrorKind;
use std::error::Error;
use mio::event::Event;
use mio::unix::SourceFd;
use mio::{Events, Interest, Poll, Registry, Token};
use slab::Slab;
use std::fs::read_dir;
use std::io;
use std::os::fd::AsRawFd;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc, Mutex, OnceLock, RwLock};
use std::time::{Duration, Instant};
use tokio::task::JoinHandle;

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

/// This callback returns true if the observer consumed the input event.
pub type EvdevObserver =
    fn(device_path: &str, device_id: &DeviceIdentifier, event: &InputEvent) -> bool;

pub struct EventLoopManager {
    registry: Arc<Mutex<Option<Registry>>>,
    loop_handle: Mutex<Option<JoinHandle<()>>>,
    stop_flag: Arc<AtomicBool>,
    observers: Arc<Mutex<Slab<EvdevObserver>>>,
    grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
}

const POLL_TIMEOUT: Duration = Duration::from_millis(100);

impl EventLoopManager {
    pub fn get() -> &'static EventLoopManager {
        EVENT_LOOP_MANAGER.get_or_init(Self::new)
    }

    fn new() -> Self {
        EventLoopManager {
            registry: Arc::new(Mutex::new(None)),
            loop_handle: Mutex::new(None),
            stop_flag: Arc::new(AtomicBool::new(false)),
            observers: Arc::new(Mutex::new(Slab::with_capacity(16))),
            grabbed_devices: Arc::new(RwLock::new(Slab::with_capacity(32))),
        }
    }

    pub fn start(&self) -> Result<(), EvdevError> {
        let mut handle_option = self.loop_handle.lock().unwrap();

        match *handle_option {
            Some(_) => {
                // Do nothing. The event loop is already started.
                info!("EvdevManager event loop is already running");
                Ok(())
            }

            None => {
                self.stop_flag.store(false, Ordering::SeqCst);

                let stop_flag = self.stop_flag.clone();
                let observers = self.observers.clone();
                let grabbed_devices = self.grabbed_devices.clone();
                let registry = self.registry.clone();

                let (tx, rx) = mpsc::channel();

                let handle = get_runtime().spawn(async move {
                    let poll = Poll::new().unwrap();

                    // Store the registry for use by grab/ungrab operations
                    *registry.lock().unwrap() = Some(poll.registry().try_clone().unwrap());

                    if let Err(e) = tx.send(()) {
                        error!("Failed to signal event loop start: {}", e);
                    }

                    EventLoop::new(poll).start(&stop_flag, &grabbed_devices, &observers);
                });

                *handle_option = Some(handle);

                match rx.recv_timeout(Duration::from_secs(2)) {
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
                // Signal the loop to stop
                self.stop_flag.store(true, Ordering::SeqCst);

                // Wait for the loop to finish (with timeout)
                let start = Instant::now();
                while !handle.is_finished() {
                    if start.elapsed() > Duration::from_secs(2) {
                        error!("Event loop did not stop in time, aborting");
                        handle.abort();
                        return Err(Box::new(EvdevError::new(-libc::ETIMEDOUT)));
                    }
                    std::thread::sleep(Duration::from_millis(10));
                }
            }
        }

        // Clear the registry
        if let Ok(mut registry) = self.registry.lock() {
            registry.take();
        }

        Ok(())
    }

    pub fn grab_device(&self, path: &str) -> Result<(), Box<dyn Error>> {
        let registry_guard = self.registry.lock().unwrap();
        let registry = registry_guard
            .as_ref()
            .ok_or_else(|| io::Error::new(ErrorKind::NotConnected, "Event loop not running"))?;

        let mut devices = self.grabbed_devices.write().unwrap();

        // Check if device is already grabbed
        if devices.iter().any(|(_, device)| device.device_path == path) {
            return Err(Box::new(io::Error::new(
                ErrorKind::AlreadyExists,
                format!("Device already grabbed: {}", path),
            )));
        }

        let device = GrabbedDevice::new(path)?;
        let fd = device.evdev.lock().unwrap().as_raw_fd();
        let key = devices.insert(device);

        let mut source_fd = SourceFd(&fd);

        // Register with key + 1 because 0 could be reserved for future use
        registry
            .register(&mut source_fd, Token(key + 1), Interest::READABLE)
            .inspect_err(|e| {
                // Remove device on registration failure
                devices.remove(key);
                error!("Failed to register device {}: {}", path, e);
            })?;

        info!("Grabbed device: {}", path);
        Ok(())
    }

    pub fn ungrab_device(&self, path: &str) -> Result<(), io::Error> {
        let registry_guard = self.registry.lock().unwrap();
        let registry = registry_guard
            .as_ref()
            .ok_or_else(|| io::Error::new(ErrorKind::NotConnected, "Event loop not running"))?;

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

                registry
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
        let registry_guard = self.registry.lock().unwrap();
        let registry = registry_guard.as_ref();

        let mut devices = self.grabbed_devices.write().unwrap();

        // Deregister all devices from the registry
        if let Some(registry) = registry {
            for (_, device) in devices.iter() {
                let fd = device.evdev.lock().unwrap().as_raw_fd();
                let mut source_fd = SourceFd(&fd);

                registry
                    .deregister(&mut source_fd)
                    .inspect_err(|e| {
                        error!("Failed to deregister device {}: {}", device.device_path, e)
                    })
                    .ok();
            }
        }

        // Clear all devices (Drop will ungrab each one)
        devices.clear();
        info!("Ungrabbed all devices");
        Ok(())
    }

    pub fn register_observer(&self, observer: EvdevObserver) {
        self.observers.lock().unwrap().insert(observer);
    }

    /// Get the paths to all the real (non uinput) connected devices.
    pub fn get_all_real_devices() -> Result<Vec<PathBuf>, EvdevError> {
        let mut paths: Vec<PathBuf> = Vec::new();

        let dir = read_dir("/dev/input")?;

        for entry_result in dir {
            match entry_result {
                Ok(entry) => {
                    let path = entry.path();

                    // TODO filter devices that are not grabbed uinput devices
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

        Ok(paths)
    }
}

struct EventLoop {
    poll: Poll,
}

impl EventLoop {
    pub fn new(poll: Poll) -> Self {
        EventLoop { poll }
    }

    /// This blocks until the stop flag is set.
    fn start(
        &mut self,
        stop_flag: &AtomicBool,
        grabbed_devices: &RwLock<Slab<GrabbedDevice>>,
        observers: &Mutex<Slab<EvdevObserver>>,
    ) {
        let mut events = Events::with_capacity(128);

        info!("Started evdev event loop");

        loop {
            if stop_flag.load(Ordering::SeqCst) {
                break;
            }

            // TODO use waker to wake it up and then stop the loop
            match self.poll.poll(&mut events, Some(POLL_TIMEOUT)) {
                Ok(_) => {
                    for event in events.iter() {
                        self.on_poll_event(event, grabbed_devices, observers);
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

    fn on_poll_event(
        &mut self,
        event: &Event,
        grabbed_devices: &RwLock<Slab<GrabbedDevice>>,
        observers: &Mutex<Slab<EvdevObserver>>,
    ) {
        let Token(key) = event.token();
        // Subtract 1 because Token(0) is reserved
        let slab_key = key - 1;

        let devices = grabbed_devices.read().unwrap();

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
                    Self::process_observers(&input_event, grabbed_device, observers);
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

    fn process_observers(
        event: &InputEvent,
        grabbed_device: &GrabbedDevice,
        observers: &Mutex<Slab<EvdevObserver>>,
    ) {
        let mut consume = false;

        for (_, observer) in observers.lock().unwrap().iter() {
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
