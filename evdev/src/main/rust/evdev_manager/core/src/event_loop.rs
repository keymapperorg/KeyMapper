use crate::android::keylayout::key_layout_map_manager::{
    get_generic_key_layout_map, KeyLayoutMapManager,
};
use crate::evdev_device_info::EvdevDeviceInfo;
use crate::evdev_error::EvdevError;
use crate::evdev_grab_controller::EvdevGrabController;
use crate::grab_device_request::GrabDeviceRequest;
use crate::grabbed_device::GrabbedDevice;
use crate::runtime::get_runtime;
use evdev::enums::{EventCode, EventType, EV_SYN};
use evdev::util::{event_code_to_int, int_to_event_code};
use evdev::{DeviceWrapper, InputEvent, ReadFlag, ReadStatus};
use libc::c_uint;
use mio::event::Event;
use mio::unix::SourceFd;
use mio::{Events, Interest, Poll, Registry, Token, Waker};
use slab::Slab;
use std::collections::HashMap;
use std::error::Error;
use std::fs::read_dir;
use std::io;
use std::io::ErrorKind;
use std::os::fd::AsRawFd;
use std::path::PathBuf;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc, OnceLock, RwLock};
use std::time::{Duration, Instant};
use std::{fmt, usize};
use tokio::task::JoinHandle;

/// This callback returns true if the observer consumed the input event.
/// Parameters: device_id (slab key), device_identifier, event
pub type EvdevObserver =
    fn(device_id: usize, device_identifier: &EvdevDeviceInfo, event: &InputEvent) -> bool;

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

const WAKER_TOKEN: Token = Token(usize::MAX - 1);

pub struct EventLoopManager {
    stop_flag: Arc<AtomicBool>,
    poll: Arc<RwLock<Poll>>,
    registry: Arc<Registry>,
    join_handle: RwLock<Option<JoinHandle<()>>>,
    waker: Waker,
    observer: EvdevObserver,
    poll_controller: EvdevGrabController
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

        EventLoopManager {
            stop_flag: Arc::new(AtomicBool::new(false)),
            poll: poll_lock,
            registry: Arc::new(registry),
            join_handle: RwLock::new(None),
            waker,
            observer,
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
        requested_devices: Vec<GrabDeviceRequest>,
    ) -> Vec<(usize, EvdevDeviceInfo)> {
        let mut devices_slab = self.grabbed_devices.write().unwrap();

        self.ungrab_unused_devices(&mut devices_slab, &requested_devices);

        self.grab_new_devices(&mut devices_slab, requested_devices);

        // Return all currently grabbed devices as (slab_key, DeviceIdentifier) tuples
        // The slab_key is used as device_id for O(1) lookup when writing events
        devices_slab
            .iter()
            .map(|(slab_key, device)| (slab_key, device.device_info.clone()))
            .collect()
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

        debug!(
            "Write evdev event: device_id={} event_type={} code={} value={}",
            device_id, event_type, code, value
        );

        device
            .uinput
            .write_event(EventType::EV_KEY as c_uint, code, value)
            .map_err(EvdevError::from)?;

        // Send SYN_REPORT
        device
            .uinput
            .write_syn_event(EV_SYN::SYN_REPORT)
            .map_err(|err| err.into())
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
            KeyLayoutMapManager::get().find_scan_code_for_key(&device.device_info, key_code)?;

        match scan_code_result {
            None => {
                error!("Failed to find scan code for key: {}", key_code);
                Err(Box::new(EvdevError::new(-libc::ENODATA)))
            }
            Some(code) => {
                debug!(
                    "Write key code evdev event: key_code={} value={}",
                    key_code, value
                );

                device
                    .uinput
                    .write_event(EventType::EV_KEY as c_uint, code, value)
                    .map_err(EvdevError::from)?;

                // Send SYN_REPORT
                device
                    .uinput
                    .write_syn_event(EV_SYN::SYN_REPORT)
                    .map_err(|err| err.into())
            }
        }
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
        let slab_key = key;

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
        let consumed = (self.observer)(device_id, &grabbed_device.device_info, event);

        if !consumed {
            let (event_type, event_code) = event_code_to_int(&event.event_code);
            grabbed_device
                .uinput
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
