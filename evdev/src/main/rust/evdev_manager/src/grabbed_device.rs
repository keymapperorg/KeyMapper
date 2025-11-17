use crate::evdev_error::EvdevError;
use evdev::enums::{EventCode, EV_SYN};
use evdev::{util::int_to_event_code, Device, GrabMode, InputEvent, TimeVal, UInputDevice};
use std::borrow::Cow::Owned;
use std::fs::OpenOptions;
use std::os::fd::{AsFd, AsRawFd, BorrowedFd, FromRawFd, OwnedFd, RawFd};
use std::os::unix::fs::OpenOptionsExt;

/// Device context containing all information about a grabbed evdev device
pub struct GrabbedDevice {
    pub evdev: Device,
    pub uinput: UInputDevice,
    pub device_path: String,
}

impl GrabbedDevice {
    /// Create a new device context by grabbing a device
    pub fn new(device_path: &str) -> Result<Self, EvdevError> {
        // Open device with O_NONBLOCK so that the loop reading events eventually returns
        // due to an EAGAIN error

        // TODO do not allow grabbing uinput devices

        let file = OpenOptions::new()
            .read(true)
            .custom_flags(libc::O_NONBLOCK)
            .open(device_path)
            .map_err(|e| EvdevError::from(e))?;

        // Create device from file
        let mut evdev = Device::new_from_file(file).map_err(|e| EvdevError::from(e))?;

        // Grab the device
        evdev
            .grab(GrabMode::Grab)
            .map_err(|e| EvdevError::from(e))?;

        // Create uinput device for forwarding unconsumed events
        let uinput = UInputDevice::create_from_device(&evdev).map_err(|e| EvdevError::from(e))?;

        Ok(Self {
            evdev,
            uinput,
            device_path: device_path.to_string(),
        })
    }

    /// Write an event to the uinput device
    pub fn write_event(&self, event_type: u32, code: u32, value: i32) -> Result<(), EvdevError> {
        self.uinput
            .write_event(event_type, code, value)
            .map_err(|e| EvdevError::from(e))?;

        // Send SYN_REPORT
        self.uinput
            .write_syn_event(EV_SYN::SYN_REPORT)
            .map_err(|e| EvdevError::from(e))?;

        Ok(())
    }
}

impl Drop for GrabbedDevice {
    fn drop(&mut self) {
        // Ungrab the device
        let _ = self.evdev.grab(GrabMode::Ungrab);
        // uinput device is dropped automatically
    }
}
