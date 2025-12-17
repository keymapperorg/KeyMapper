use crate::evdev_device_info::EvdevDeviceInfo;
use crate::evdev_error::EvdevError;
use evdev::enums::EventCode;
use evdev::{Device, DeviceWrapper, GrabMode, UInputDevice};
use std::fs::OpenOptions;
use std::os::unix::fs::OpenOptionsExt;
use std::path::PathBuf;
use std::sync::Mutex;

/// Device context containing all information about a grabbed evdev device
pub struct GrabbedDevice {
    pub device_path: PathBuf,
    pub device_info: EvdevDeviceInfo,
    /// The libevdev Device can not be shared safely across threads so wrap it in a mutex.
    pub evdev: Mutex<Device>,
    pub uinput: UInputDevice,
    /// The extra event codes that were enabled for the uinput device. This is so that the
    /// uinput device can input events that the original device didn't support.
    pub extra_event_codes: Vec<EventCode>,
}

impl GrabbedDevice {
    /// Create a grabbed device that also enables the given EventCodes in the uinput device.
    pub fn new(device_path: PathBuf, extra_events: &[EventCode]) -> Result<Self, EvdevError> {
        let mut evdev = Self::open_evdev_device(&device_path)?;

        for event in extra_events {
            evdev.enable(*event)?;
        }

        evdev.grab(GrabMode::Grab).map_err(EvdevError::from)?;
        let uinput = UInputDevice::create_from_device(&evdev).map_err(EvdevError::from)?;

        let device_info = EvdevDeviceInfo {
            name: evdev.name().unwrap_or("").to_string(),
            bus: evdev.bustype(),
            vendor: evdev.vendor_id(),
            product: evdev.product_id(),
            version: evdev.version(),
        };

        Ok(Self {
            device_path,
            device_info,
            evdev: Mutex::new(evdev),
            uinput,
            extra_event_codes: extra_events.into(),
        })
    }

    fn open_evdev_device(device_path: &PathBuf) -> Result<Device, EvdevError> {
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
        let mut evdev = self.evdev.lock().unwrap();
        // Ungrab the device
        evdev.grab(GrabMode::Ungrab);
    }
}
