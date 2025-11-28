use crate::device_identifier::DeviceIdentifier;
use crate::evdev_error::EvdevError;
use evdev::enums::EV_SYN;
use evdev::{Device, DeviceWrapper, GrabMode, UInputDevice};
use std::fs::OpenOptions;
use std::os::unix::fs::OpenOptionsExt;
use std::sync::Mutex;

/// Device context containing all information about a grabbed evdev device
pub struct GrabbedDevice {
    pub device_path: String,
    pub device_id: DeviceIdentifier,
    /// The libevdev Device can not be shared safely across threads so wrap it in a mutex.
    pub evdev: Mutex<Device>,
    pub uinput: UInputDevice,
}

impl GrabbedDevice {
    /// Create a new device context by grabbing a device
    pub fn new(device_path: &str) -> Result<Self, EvdevError> {
        // Open device with O_NONBLOCK so that the loop reading events eventually returns
        // due to an EAGAIN error

        let file = OpenOptions::new()
            .read(true)
            .custom_flags(libc::O_NONBLOCK)
            .open(device_path)
            .map_err(EvdevError::from)?;

        // Create device from file
        let mut evdev = Device::new_from_file(file).map_err(EvdevError::from)?;

        // Grab the device
        evdev.grab(GrabMode::Grab).map_err(EvdevError::from)?;

        // Create uinput device for forwarding unconsumed events
        let uinput = UInputDevice::create_from_device(&evdev).map_err(EvdevError::from)?;

        let device_id: DeviceIdentifier = DeviceIdentifier {
            name: evdev.name().unwrap_or("").to_string(),
            vendor: evdev.vendor_id(),
            product: evdev.product_id(),
            version: evdev.version(),
        };

        Ok(Self {
            evdev: Mutex::new(evdev),
            device_id,
            uinput,
            device_path: device_path.to_string(),
        })
    }

    /// Write an event to the uinput device
    pub fn write_event(&self, event_type: u32, code: u32, value: i32) -> Result<(), EvdevError> {
        self.uinput
            .write_event(event_type, code, value)
            .map_err(EvdevError::from)?;

        // Send SYN_REPORT
        self.uinput
            .write_syn_event(EV_SYN::SYN_REPORT)
            .map_err(EvdevError::from)?;

        Ok(())
    }
}

impl Drop for GrabbedDevice {
    fn drop(&mut self) {
        // Ungrab the device
        let _ = self.evdev.lock().unwrap().grab(GrabMode::Ungrab);
        // uinput device is dropped automatically
    }
}
