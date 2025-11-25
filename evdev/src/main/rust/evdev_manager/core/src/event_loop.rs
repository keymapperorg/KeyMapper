use crate::evdev_error::EvdevError;
use crate::grabbed_device::GrabbedDevice;
use crate::runtime::get_runtime;
use evdev::enums::EV_SYN;
use evdev::util::event_code_to_int;
use evdev::{Device, GrabMode, InputEvent, ReadFlag, ReadStatus, UInputDevice};
use io::ErrorKind;
use mio::event::{Event, Source};
use mio::unix::SourceFd;
use mio::{Events, Interest, Poll, Registry, Token, Waker};
use slab::Slab;
use std::collections::VecDeque;
use std::error::Error;
use std::fmt::format;
use std::io;
use std::os::fd::AsRawFd;
use std::sync::mpsc::Receiver;
use std::sync::{mpsc, Arc, LazyLock, LockResult, Mutex, OnceLock, PoisonError, RwLock};
use std::time::Duration;
use tokio::task::JoinHandle;

static EVENT_LOOP_MANAGER: OnceLock<EventLoopManager> = OnceLock::new();

/// This callback returns true if the observer consumed the input event.
pub type EvdevObserver = fn(device_path: &str, event: &InputEvent) -> bool;

/// This uses the Waker and command queue pattern for updating the grabbed devices
/// because the libevdev struct can not be safely shared between threads. Otherwise, the
/// grabbed_devices slab would be stored here and an EventLoop struct wouldn't be necessary.
/// This also has the benefit that the loop can be interrupted gracefully without having to
/// kill it.
pub struct EventLoopManager {
    loop_handle: Mutex<Option<JoinHandle<()>>>,
    waker: Arc<Mutex<Option<Waker>>>,
    command_queue: Arc<Mutex<VecDeque<Command>>>,
    observers: Arc<Mutex<Slab<EvdevObserver>>>,
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
            observers: Arc::new(Mutex::new(Slab::with_capacity(16))),
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
                let waker = self.waker.clone();
                let command_queue = self.command_queue.clone();
                let observers = self.observers.clone();

                let (tx, rx) = mpsc::channel();

                let handle = get_runtime().spawn(async move {
                    // TODO does logging still work across threads or does another need to be installed?
                    let poll = Poll::new().unwrap();
                    *waker.lock().unwrap() =
                        Some(Waker::new(poll.registry(), TOKEN_COMMAND).unwrap());

                    if let Err(e) = tx.send(()) {
                        error!("Failed to signal event loop start: {}", e);
                    }

                    EventLoop::new(poll).start(&command_queue, &observers);
                });

                get_runtime().spawn(async {});

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
                let (tx, rx) = mpsc::channel();
                self.send_command(Command::StopLoop(tx))?;

                match rx.recv_timeout(Duration::from_secs(2)) {
                    Ok(_) => {
                        // Loop stopped gracefully
                    }
                    Err(e) => {
                        error!("Failed to wait for event loop stop: {}", e);
                        handle.abort();
                        return Err(Box::new(EvdevError::new(-libc::ETIMEDOUT)));
                    }
                }
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
        .inspect_err(|e| error!("Failed to send grab device command {}: {}", path, e))
    }

    pub fn register_observer(&self, observer: EvdevObserver) {
        self.observers.lock().unwrap().insert(observer);
    }

    fn send_command(&self, command: Command) -> Result<(), Box<dyn Error>> {
        self.command_queue.lock().unwrap().push_back(command);
        match self.waker.lock().unwrap().as_ref() {
            None => {
                error!("EvdevManager event loop is not running");
            }
            Some(waker) => waker.wake()?,
        }

        Ok(())
    }
}

struct EventLoop {
    poll: Poll,
    grabbed_devices: Slab<GrabbedDevice>,
}

impl EventLoop {
    pub fn new(poll: Poll) -> Self {
        EventLoop {
            poll,
            grabbed_devices: Slab::with_capacity(32),
        }
    }

    /// This blocks until a Stop command is sent in the command queue.
    fn start(
        &mut self,
        command_queue: &Mutex<VecDeque<Command>>,
        observers: &Mutex<Slab<EvdevObserver>>,
    ) {
        let mut events = Events::with_capacity(128);

        info!("Started evdev event loop");

        'main_loop: loop {
            match self.poll.poll(&mut events, None) {
                Ok(_) => {
                    for event in events.iter() {
                        if self.on_poll_event(event, command_queue, observers) {
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
            self.poll
                .registry()
                .deregister(&mut source_fd)
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

    fn ungrab_device(&mut self, path: &str) -> io::Result<()> {
        let device_option = self
            .grabbed_devices
            .iter()
            .find(|(_, device)| device.device_path == path);

        match device_option {
            None => Err(io::Error::new(
                ErrorKind::NotFound,
                format!("Device not found {}", path),
            )),
            Some((key, _)) => {
                let mut device = self.grabbed_devices.remove(key);

                device
                    .evdev
                    .grab(GrabMode::Ungrab)
                    .inspect_err(|e| error!("Failed to ungrab device {}: {}", path, e))
            }
        }
    }

    fn grab_device(&mut self, path: &str) -> io::Result<()> {
        if self
            .grabbed_devices
            .iter()
            .any(|(_, device)| device.device_path == path)
        {
            return Err(io::Error::new(
                ErrorKind::AlreadyExists,
                format!("Device already grabbed: {}", path),
            ));
        }

        let device = GrabbedDevice::new(path)?;
        let key = self.grabbed_devices.insert(device);

        let stored_device = self.grabbed_devices.get(key).unwrap();
        let mut source_fd = SourceFd(&stored_device.evdev.as_raw_fd());

        // Register with key + 1 because 0 is reserved for commands.
        self.poll
            .registry()
            .register(&mut source_fd, Token(key + 1), Interest::READABLE)
    }

    /// Returns whether to stop the loop.
    fn on_poll_event(
        &mut self,
        event: &Event,
        command_queue: &Mutex<VecDeque<Command>>,
        observers: &Mutex<Slab<EvdevObserver>>,
    ) -> bool {
        match event.token() {
            TOKEN_COMMAND => {
                while let Some(command) = command_queue.lock().unwrap().pop_front() {
                    match command {
                        Command::StopLoop(tx) => {
                            self.stop();
                            tx.send(()).ok();
                            return true;
                        }

                        Command::GrabDevice { path } => {
                            self.grab_device(path.as_str())
                                .inspect_err(|e| error!("Failed to grab device {}: {}", path, e))
                                .ok();
                        }

                        Command::UngrabDevice { path } => {}

                        Command::UngrabAllDevices => {}
                    }
                }
            }

            Token(key) => {
                // Subtract 1 because Token(0) is used for commands
                let slab_key = key - 1;

                let grabbed_device = self
                    .grabbed_devices
                    .get(slab_key)
                    .unwrap_or_else(|| panic!("Can not find grabbed device with key {}", slab_key));

                if let Ok(Some(event)) = self.read_evdev_events(&grabbed_device.evdev) {
                    Self::process_observers(&event, grabbed_device, observers);
                }
            }
        }

        false
    }

    fn process_observers(
        event: &InputEvent,
        grabbed_device: &GrabbedDevice,
        observers: &Mutex<Slab<EvdevObserver>>,
    ) {
        let mut consume = false;

        for (_, observer) in observers.lock().unwrap().iter() {
            if observer(&grabbed_device.device_path, event) {
                consume = true;
            }
        }

        if !consume {
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

    fn read_evdev_events(&self, evdev_device: &Device) -> io::Result<Option<InputEvent>> {
        match evdev_device.next_event(ReadFlag::NORMAL)? {
            (ReadStatus::Success, event) => {
                // Keep this logging line. Later debug/verbose events can be toggled from
                // the frontend.
                debug!("Evdev event: {:?}", event);
                Ok(Some(event))
            }

            (ReadStatus::Sync, _) => {
                Self::process_sync_event(evdev_device);
                Ok(None)
            }
        }
    }

    fn process_sync_event(evdev_device: &Device) {
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

                    let evdev_error = EvdevError::from(e);
                    error!("Evdev sync event error: {}", evdev_error);
                }
            }
        }
    }

    pub fn write_event_to_device(
        uinput: &UInputDevice,
        event_type: u32,
        code: u32,
        value: i32,
    ) -> Result<(), EvdevError> {
        uinput
            .write_event(event_type, code, value)
            .map_err(|e| EvdevError::from(e))?;
        uinput
            .write_syn_event(EV_SYN::SYN_REPORT)
            .map_err(|e| EvdevError::from(e))
    }
}

#[derive(Debug, Clone)]
enum Command {
    StopLoop(mpsc::Sender<()>),

    GrabDevice { path: String },

    UngrabDevice { path: String },

    UngrabAllDevices,
}
