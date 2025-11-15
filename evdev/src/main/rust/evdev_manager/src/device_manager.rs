use crate::evdev_error::EvdevError;
use evdev::enums::{EventCode, EV_SYN};
use evdev::{util::int_to_event_code, Device, GrabMode, InputEvent, TimeVal, UInputDevice};
use std::fs::OpenOptions;
use std::os::fd::{AsFd, AsRawFd, BorrowedFd, FromRawFd, OwnedFd, RawFd};
use std::os::unix::fs::OpenOptionsExt;

/// Device context containing all information about a grabbed evdev device
pub struct DeviceContext {
    pub evdev: Device,
    pub uinput: UInputDevice,
    pub device_path: String,
    pub fd: OwnedFd,
}

impl DeviceContext {
    /// Create a new device context by grabbing a device
    pub fn grab_device(device_path: &str) -> Result<Self, EvdevError> {
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

        // Get the file descriptor for keeping track (Device owns the file, so we get the fd)
        let fd = unsafe { OwnedFd::from_raw_fd(evdev.as_raw_fd()) };

        // Create uinput device for forwarding unconsumed events
        let uinput = UInputDevice::create_from_device(&evdev).map_err(|e| EvdevError::from(e))?;

        Ok(Self {
            evdev,
            uinput,
            device_path: device_path.to_string(),
            fd,
        })
    }

    /// Write an event to the uinput device
    pub fn write_event(&self, event_type: u32, code: u32, value: i32) -> Result<(), EvdevError> {
        // Convert raw event type and code to EventCode
        let event_code = int_to_event_code(event_type, code);

        // Create InputEvent
        let event = InputEvent::new(&TimeVal::new(0, 0), &event_code, value);

        self.uinput
            .write_event(&event)
            .map_err(|e| EvdevError::from(e))?;

        // Send SYN_REPORT
        let syn_event = InputEvent::new(
            &TimeVal::new(0, 0),
            &EventCode::EV_SYN(EV_SYN::SYN_REPORT),
            0,
        );

        self.uinput
            .write_event(&syn_event)
            .map_err(|e| EvdevError::from(e))?;

        Ok(())
    }
}

impl Drop for DeviceContext {
    fn drop(&mut self) {
        // Ungrab the device
        let _ = self.evdev.grab(GrabMode::Ungrab);

        // uinput device is dropped automatically
    }
}
