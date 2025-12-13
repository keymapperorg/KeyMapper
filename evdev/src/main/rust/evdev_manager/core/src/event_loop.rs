use crate::android::keylayout::key_layout_map_manager::{
    get_generic_key_layout_map, KeyLayoutMapManager,
};
use crate::device_identifier::DeviceIdentifier;
use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::runtime::get_runtime;
use evdev::enums::{EventCode, EventType};
use evdev::util::{event_code_to_int, int_to_event_code};
use evdev::{DeviceWrapper, InputEvent, ReadFlag, ReadStatus};
use libc::c_uint;
use mio::event::Event;
use mio::unix::SourceFd;
use mio::{Events, Interest, Poll, Registry, Token, Waker};
use slab::Slab;
use std::collections::HashMap;
use std::error::Error;
use std::fmt;
use std::fs::read_dir;
use std::io;
use std::io::ErrorKind;
use std::os::fd::AsRawFd;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc, OnceLock, RwLock};
use std::time::{Duration, Instant};
use tokio::task::JoinHandle;

/// This callback returns true if the observer consumed the input event.
/// Parameters: device_id (slab key), device_identifier, event
pub type EvdevObserver =
    fn(device_id: usize, device_identifier: &DeviceIdentifier, event: &InputEvent) -> bool;

/// Key for device path map - only JNI-exposed fields (excludes version).
/// Used for O(1) HashMap lookup when matching devices.
#[derive(Hash, Eq, PartialEq)]
struct DeviceIdentifierKey {
    name: String,
    bus: u16,
    vendor: u16,
    product: u16,
}

impl From<&DeviceIdentifier> for DeviceIdentifierKey {
    fn from(id: &DeviceIdentifier) -> Self {
        DeviceIdentifierKey {
            name: id.name.clone(),
            bus: id.bus,
            vendor: id.vendor,
            product: id.product,
        }
    }
}

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

const WAKER_TOKEN: Token = Token(0);

pub struct EventLoopManager {
    stop_flag: Arc<AtomicBool>,
    poll: Arc<RwLock<Poll>>,
    registry: Arc<Registry>,
    join_handle: RwLock<Option<JoinHandle<()>>>,
    waker: Waker,
    observer: EvdevObserver,
    grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
    /// These are the scan codes that have equivalent key codes in Android.
    android_scan_codes: Vec<EventCode>,
}

impl fmt::Debug for EventLoopManager {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let is_running = self
            .join_handle
            .read()
            .map(|h| h.is_some())
            .unwrap_or(false);
        let device_count = self.grabbed_devices.read().map(|d| d.len()).unwrap_or(0);

        f.debug_struct("EventLoopManager")
            .field("is_running", &is_running)
            .field("grabbed_device_count", &device_count)
            .finish_non_exhaustive()
    }
}

impl EventLoopManager {
    /// Initialize the EventLoopManager with an observer. Must be called once before `get()`.
    /// Panics if called more than once.
    pub fn init(observer: EvdevObserver) {
        EVENT_LOOP_MANAGER
            .set(Self::new(observer))
            .expect("EventLoopManager already initialized");
    }

    /// Get the EventLoopManager instance. Panics if `init()` was not called.
    pub fn get() -> &'static EventLoopManager {
        EVENT_LOOP_MANAGER
            .get()
            .expect("EventLoopManager not initialized. Call init() first.")
    }

    fn new(observer: EvdevObserver) -> Self {
        let poll = Poll::new().unwrap();
        let registry = poll.registry().try_clone().unwrap();
        let waker = Waker::new(&registry, WAKER_TOKEN).unwrap();
        let poll_lock = Arc::new(RwLock::new(poll));

        let android_scan_codes: Vec<EventCode> = get_generic_key_layout_map()
            .scan_codes
            .iter()
            .map(|scan_code| int_to_event_code(EventType::EV_KEY as c_uint, *scan_code))
            .collect();

        EventLoopManager {
            stop_flag: Arc::new(AtomicBool::new(false)),
            poll: poll_lock,
            registry: Arc::new(registry),
            join_handle: RwLock::new(None),
            waker,
            observer,
            grabbed_devices: Arc::new(RwLock::new(Slab::with_capacity(32))),
            android_scan_codes,
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

            let observer = self.observer;
            let grabbed_devices = self.grabbed_devices.clone();

            let (tx, rx) = mpsc::channel();

            let poll_lock_clone = self.poll.clone();
            let stop_flag_clone = self.stop_flag.clone();

            let join_handle = get_runtime().spawn(async move {
                tx.send(()).unwrap();
                EventLoopThread::new(stop_flag_clone, poll_lock_clone, observer, grabbed_devices)
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

    /// Set the list of grabbed devices. This will ungrab any devices that are no longer in the list
    /// and grab any new devices. Devices are matched by DeviceIdentifier (name, bus, vendor, product).
    /// Returns: A list of (device_id, DeviceIdentifier) for all successfully grabbed devices.
    pub fn set_grabbed_devices(
        &self,
        requested_devices: Vec<DeviceIdentifier>,
    ) -> Vec<(usize, DeviceIdentifier)> {
        let mut devices = self.grabbed_devices.write().unwrap();

        // Find devices to ungrab (currently grabbed but not in requested list)
        // Compare by JNI-exposed fields only (name, bus, vendor, product), not version
        let keys_to_remove: Vec<usize> = devices
            .iter()
            .filter(|(_, device)| {
                !requested_devices
                    .iter()
                    .any(|req| device_id_matches_jni_fields(&device.device_id, req))
            })
            .map(|(key, _)| key)
            .collect();

        // Ungrab devices that are no longer requested
        for key in keys_to_remove {
            if let Some(device) = devices.get(key) {
                let fd = device.evdev.lock().unwrap().as_raw_fd();
                let mut source_fd = SourceFd(&fd);
                self.registry
                    .deregister(&mut source_fd)
                    .inspect_err(|e| {
                        error!("Failed to deregister device {}: {}", device.device_path, e)
                    })
                    .ok();
                info!("Ungrabbed device: {}", device.device_path);
            }
            devices.remove(key);
        }

        // Find devices to grab (requested but not currently grabbed)
        // Compare by JNI-exposed fields only (name, bus, vendor, product), not version
        let devices_to_grab: Vec<&DeviceIdentifier> = requested_devices
            .iter()
            .filter(|req| {
                !devices
                    .iter()
                    .any(|(_, device)| device_id_matches_jni_fields(&device.device_id, req))
            })
            .collect();

        // Build device path map once for all devices to grab - O(m) instead of O(n*m)
        if !devices_to_grab.is_empty() {
            // Collect uinput paths while we still hold the lock
            let uinput_paths: Vec<String> = devices
                .iter()
                .map(|(_, device)| device.uinput.devnode().unwrap().to_string())
                .collect();

            let device_path_map = build_device_path_map(&uinput_paths);

            for device_id in devices_to_grab {
                let key = DeviceIdentifierKey::from(device_id);
                match device_path_map.get(&key) {
                    Some(path) => match self.grab_device_internal(&mut devices, path) {
                        Ok(_) => {
                            KeyLayoutMapManager::get()
                                .preload_key_layout_map(device_id)
                                .ok();
                        }

                        Err(e) => error!("Failed to grab device {}: {:?}", path, e),
                    },
                    None => {
                        warn!("Device not found: {:?}", device_id);
                    }
                }
            }
        }

        // Return all currently grabbed devices as (slab_key, DeviceIdentifier) tuples
        // The slab_key is used as device_id for O(1) lookup when writing events
        devices
            .iter()
            .map(|(slab_key, device)| (slab_key, device.device_id.clone()))
            .collect()
    }

    /// Internal method to grab a device and register it with the poll
    fn grab_device_internal(
        &self,
        devices: &mut Slab<GrabbedDevice>,
        path: &str,
    ) -> Result<usize, Box<dyn Error>> {
        // Also enable all the scan codes supported by Android so that Key Event actions with
        // Key Mapper can input any key code through this device, regardless of what is supported
        // by the real physical evdev device.
        let device = GrabbedDevice::new_with_extra_events(path, &self.android_scan_codes)?;
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
        Ok(key)
    }

    /// Write an event to a grabbed device's uinput.
    /// The device_id is the slab key returned by set_grabbed_devices(), enabling O(1) lookup.
    pub fn write_event(
        &self,
        device_id: usize,
        event_type: u32,
        code: u32,
        value: i32,
    ) -> Result<(), EvdevError> {
        let devices = self.grabbed_devices.read().unwrap();

        let device = devices
            .get(device_id) // O(1) slab lookup
            .ok_or_else(|| EvdevError::new(-libc::ENODEV))?;
        device.write_event(event_type, code, value)
    }

    pub fn write_key_code_event(
        &self,
        device_id: usize,
        key_code: u32,
        value: i32,
    ) -> Result<(), Box<dyn Error>> {
        let devices = self.grabbed_devices.read().unwrap();

        let device = devices
            .get(device_id)
            .ok_or_else(|| EvdevError::new(-libc::ENODEV))?;

        let scan_code_result =
            KeyLayoutMapManager::get().find_scan_code_for_key(&device.device_id, key_code)?;

        match scan_code_result {
            None => {
                error!("Failed to find scan code for key: {}", key_code);
                Err(Box::new(EvdevError::new(-libc::ENODATA)))
            }
            Some(code) => device
                .write_event(EventType::EV_KEY as c_uint, code, value)
                .map_err(|err| err.into()),
        }
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
    observer: EvdevObserver,
    grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
}

impl EventLoopThread {
    pub fn new(
        stop_flag: Arc<AtomicBool>,
        poll: Arc<RwLock<Poll>>,
        observer: EvdevObserver,
        grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
    ) -> Self {
        EventLoopThread {
            stop_flag,
            poll,
            observer,
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
                    self.process_event(slab_key, &input_event, grabbed_device);
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

    fn process_event(&self, device_id: usize, event: &InputEvent, grabbed_device: &GrabbedDevice) {
        let consumed = (self.observer)(device_id, &grabbed_device.device_id, event);

        if !consumed {
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

/// Compare DeviceIdentifiers by JNI-exposed fields only (name, bus, vendor, product).
/// Version is excluded as it's not exposed through the Java API.
fn device_id_matches_jni_fields(a: &DeviceIdentifier, b: &DeviceIdentifier) -> bool {
    a.name == b.name && a.bus == b.bus && a.vendor == b.vendor && a.product == b.product
}

/// Build a map of DeviceIdentifierKey -> device path.
/// Scans /dev/input once for O(m) instead of O(n*m) when grabbing multiple devices.
fn build_device_path_map(uinput_paths: &[String]) -> HashMap<DeviceIdentifierKey, String> {
    let mut map = HashMap::new();

    let Ok(dir) = read_dir("/dev/input") else {
        return map;
    };

    for entry in dir.flatten() {
        let path = entry.path();

        // Skip uinput devices we created
        if let Some(path_str) = path.to_str() {
            if uinput_paths.iter().any(|p| p == path_str) {
                continue;
            }
        }

        if let Ok(device) = evdev::Device::new_from_path(&path) {
            let key = DeviceIdentifierKey {
                name: device.name().unwrap_or("").to_string(),
                bus: device.bustype(),
                vendor: device.vendor_id(),
                product: device.product_id(),
            };
            if let Some(path_str) = path.to_str() {
                map.insert(key, path_str.to_string());
            }
        }
    }

    map
}
