use crate::device_identifier::DeviceIdentifier;
use crate::evdev_error::EvdevError;
use evdev::enums::EventCode;
use evdev::{Device, DeviceWrapper, GrabMode, UInputDevice};
use std::fs::OpenOptions;
use std::os::unix::fs::OpenOptionsExt;
use std::sync::Mutex;

/// Device context containing all information about a grabbed evdev device
pub struct GrabbedDevice {
    pub device_path: String,
    pub device_id: DeviceIdentifier,
    /// The extra events that should be supported by the uinput device.
    pub extra_events: Vec<EventCode>,
    /// The libevdev Device can not be shared safely across threads so wrap it in a mutex.
    pub evdev: Mutex<Device>,
    pub uinput: UInputDevice,
}

impl GrabbedDevice {
    /// Create a grabbed device that also enables the given EventCodes in the uinput device.
    pub fn new_with_extra_events(
        device_path: &str,
        extra_events: &[EventCode],
    ) -> Result<Self, EvdevError> {
        let mut evdev = Self::open_evdev_device(device_path)?;

        for event in extra_events {
            evdev.enable(*event)?;
        }

        evdev.grab(GrabMode::Grab).map_err(EvdevError::from)?;
        let uinput = UInputDevice::create_from_device(&evdev).map_err(EvdevError::from)?;

        let device_id: DeviceIdentifier = DeviceIdentifier {
            name: evdev.name().unwrap_or("").to_string(),
            bus: evdev.bustype(),
            vendor: evdev.vendor_id(),
            product: evdev.product_id(),
            version: evdev.version(),
        };

        Ok(Self {
            evdev: Mutex::new(evdev),
            device_id,
            uinput,
            device_path: device_path.to_string(),
            extra_events: extra_events.into(),
        })
    }

    fn open_evdev_device(device_path: &str) -> Result<Device, EvdevError> {
        // Open device with O_NONBLOCK so that the loop reading events eventually returns
        // due to an EAGAIN error

        let file = OpenOptions::new()
            .read(true)
            .custom_flags(libc::O_NONBLOCK)
            .open(device_path)
            .map_err(EvdevError::from)?;

        let evdev = Device::new_from_file(file).map_err(EvdevError::from)?;
        Ok(evdev)
    }
}

impl Drop for GrabbedDevice {
    fn drop(&mut self) {
        // Ungrab the device
        let _ = self.evdev.lock().unwrap().grab(GrabMode::Ungrab);
        // uinput device is dropped automatically
    }
}
