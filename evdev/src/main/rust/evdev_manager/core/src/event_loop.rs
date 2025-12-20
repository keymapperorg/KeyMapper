use crate::android::keylayout::key_layout_map_manager::KeyLayoutMapManager;
use crate::evdev_device_info::EvdevDeviceInfo;
use crate::evdev_error::{EvdevError, EvdevErrorCode};
use crate::evdev_grab_controller::EvdevGrabController;
use crate::grab_target::GrabTarget;
use crate::grab_target_key_code::GrabTargetKeyCode;
use crate::grabbed_device::GrabbedDevice;
use crate::grabbed_device_handle::GrabbedDeviceHandle;
use crate::runtime::get_runtime;
use evdev::enums::{EventType, EV_SYN};
use evdev::util::event_code_to_int;
use evdev::{InputEvent, ReadFlag, ReadStatus};
use libc::c_uint;
use mio::event::Event;
use mio::{Events, Poll, Token, Waker};
use slab::Slab;
use std::error::Error;
use std::io;
use std::io::ErrorKind;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{mpsc, Arc, OnceLock, RwLock};
use std::time::{Duration, Instant};
use std::{fmt, usize};
use tokio::task::JoinHandle;

/// Callback interface for evdev events and device changes
pub trait EvdevCallback: Send + Sync {
    /// Called when an input event is received from a grabbed device.
    /// Returns true if the callback consumed the event, false to pass through.
    /// Parameters: device_id (slab key), device_identifier, event
    fn on_evdev_event(
        &self,
        device_id: usize,
        device_identifier: &EvdevDeviceInfo,
        event: &InputEvent,
    ) -> bool;

    /// Called when the list of grabbed devices changes.
    /// Parameters: grabbed_devices list with their assigned IDs
    fn on_grabbed_devices_changed(&self, grabbed_devices: Vec<GrabbedDeviceHandle>);
}

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

const WAKER_TOKEN: Token = Token(usize::MAX - 1);

pub struct EventLoopManager {
    stop_flag: Arc<AtomicBool>,
    poll: Arc<RwLock<Poll>>,
    join_handle: RwLock<Option<JoinHandle<()>>>,
    waker: Waker,
    callback: Arc<dyn EvdevCallback>,
    grab_controller: EvdevGrabController,
}

impl fmt::Debug for EventLoopManager {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        f.debug_struct("EventLoopManager")
            .field("stop_flag", &self.stop_flag.load(Ordering::SeqCst))
            .field("poll", &"<Poll>")
            .field("registry", &"<Registry>")
            .field("join_handle", &"<JoinHandle>")
            .field("waker", &"<Waker>")
            .field("callback", &"<EvdevCallback>")
            .field("grab_controller", &"<EvdevGrabController>")
            .finish()
    }
}

impl EventLoopManager {
    /// Initialize the EventLoopManager with a callback. Must be called once before `get()`.
    /// Panics if called more than once.
    pub fn init(callback: Arc<dyn EvdevCallback>) {
        EVENT_LOOP_MANAGER
            .set(Self::new(callback))
            .expect("EventLoopManager already initialized");
    }

    /// Get the EventLoopManager instance. Panics if `init()` was not called.
    pub fn get() -> &'static EventLoopManager {
        EVENT_LOOP_MANAGER
            .get()
            .expect("EventLoopManager not initialized. Call init() first.")
    }

    fn new(callback: Arc<dyn EvdevCallback>) -> Self {
        let poll = Poll::new().unwrap();
        let registry = poll.registry().try_clone().unwrap();
        let waker = Waker::new(&registry, WAKER_TOKEN).unwrap();
        let poll_lock = Arc::new(RwLock::new(poll));

        let registry_arc = Arc::new(registry);

        Self {
            stop_flag: Arc::new(AtomicBool::new(false)),
            poll: poll_lock,
            join_handle: RwLock::new(None),
            waker,
            callback: callback.clone(),
            grab_controller: EvdevGrabController::new(registry_arc.clone(), callback),
        }
    }

    pub fn start(&self) -> Result<(), EvdevError> {
        let is_running = { self.join_handle.read().unwrap().is_some() };

        if is_running {
            // Do nothing. The event loop is already started.
            info!("EvdevManager event loop is already running");
            return Ok(());
        }

        self.stop_flag.store(false, Ordering::Relaxed);

        let callback = self.callback.clone();

        let (tx, rx) = mpsc::channel();

        let poll_lock_clone = self.poll.clone();
        let stop_flag_clone = self.stop_flag.clone();

        let join_handle = get_runtime().spawn(async move {
            tx.send(()).unwrap();
            EventLoopThread::new(stop_flag_clone, poll_lock_clone, callback).start();
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
    pub fn set_grab_targets(&self, targets: Vec<GrabTargetKeyCode>) -> Vec<GrabbedDeviceHandle> {
        let internal_grab_targets = targets.iter().map(Self::convert_grab_target).collect();

        let handles = self.grab_controller.set_grab_targets(internal_grab_targets);

        for handle in handles.clone() {
            KeyLayoutMapManager::get()
                .preload_key_layout_map(&handle.device_info)
                .inspect_err(|err| {
                    error!(
                        "Failed to preload key layout map for device {:?}: {}",
                        handle.device_info, err
                    );
                })
                .ok();
        }

        handles
    }

    pub fn get_real_devices(&self) -> Result<Vec<EvdevDeviceInfo>, EvdevError> {
        self.grab_controller.get_real_devices()
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
        debug!(
            "Write evdev event: device_id={} event_type={} code={} value={}",
            device_id, event_type, code, value
        );

        self.grab_controller
            .with_grabbed_device(device_id, |device| {
                device
                    .uinput
                    .write_event(event_type, code, value)
                    .map_err(EvdevError::from)
            })
            .ok_or(EvdevError::from_enum(EvdevErrorCode::NoSuchDevice))?
    }

    pub fn write_key_code_event(
        &self,
        device_id: usize,
        key_code: u32,
        value: i32,
    ) -> Result<(), Box<dyn Error>> {
        let result = self
            .grab_controller
            .with_grabbed_device(device_id, |device| {
                let scan_code_result = KeyLayoutMapManager::get()
                    .find_scan_code_for_key(&device.device_info, key_code);

                match scan_code_result {
                    Err(e) => Err(e.into()),
                    Ok(None) => {
                        error!("Failed to find scan code for key: {}", key_code);
                        Err(Box::new(EvdevError::new(-libc::ENODATA)) as Box<dyn Error>)
                    }
                    Ok(Some(code)) => {
                        debug!(
                            "Write key code evdev event: key_code={} value={}",
                            key_code, value
                        );

                        device
                            .uinput
                            .write_event(EventType::EV_KEY as c_uint, code, value)
                            .map_err(|err| Box::new(err) as Box<dyn Error>)?;

                        // Send SYN_REPORT
                        device
                            .uinput
                            .write_event(
                                EventType::EV_SYN as c_uint,
                                EV_SYN::SYN_REPORT as c_uint,
                                0,
                            )
                            .map_err(|err| Box::new(err) as Box<dyn Error>)
                    }
                }
            });

        match result {
            Some(inner_result) => inner_result,
            None => Err(Box::new(EvdevError::from_enum(
                EvdevErrorCode::NoSuchDevice,
            ))),
        }
    }

    fn convert_grab_target(target: &GrabTargetKeyCode) -> GrabTarget {
        let event_codes =
            KeyLayoutMapManager::map_key_codes_to_event_codes(&target.extra_key_codes);

        GrabTarget {
            name: target.name.clone(),
            bus: target.bus,
            vendor: target.vendor,
            product: target.product,
            extra_event_codes: event_codes,
        }
    }
}

struct EventLoopThread {
    stop_flag: Arc<AtomicBool>,
    poll: Arc<RwLock<Poll>>,
    callback: Arc<dyn EvdevCallback>,
    grabbed_devices: Arc<RwLock<Slab<GrabbedDevice>>>,
}

impl EventLoopThread {
    pub fn new(
        stop_flag: Arc<AtomicBool>,
        poll: Arc<RwLock<Poll>>,
        callback: Arc<dyn EvdevCallback>,
    ) -> Self {
        EventLoopThread {
            stop_flag,
            poll,
            callback,
            grabbed_devices: Arc::new(RwLock::new(Slab::with_capacity(64))),
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
        let consumed = self
            .callback
            .on_evdev_event(device_id, &grabbed_device.device_info, event);

        if !consumed {
            let (event_type, event_code) = event_code_to_int(&event.event_code);
            grabbed_device
                .uinput
                .write_event(event_type, event_code, event.value)
                .inspect_err(|e| {
                    error!(
                        "Failed to passthrough event to {:?}. Event: {:?}. Error: {:?}",
                        grabbed_device.device_path, event, e
                    )
                })
                .ok();
        }
    }
}
