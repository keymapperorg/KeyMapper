use crate::bindings;
use crate::evdev::{EvdevDevice, EvdevError, UInputDevice};
use nix::errno::Errno;
use nix::fcntl::{open, OFlag};
use nix::sys::stat::Mode;
use std::os::fd::{AsRawFd, BorrowedFd, OwnedFd, RawFd};

/// Device context containing all information about a grabbed evdev device
pub struct DeviceContext {
    pub evdev: EvdevDevice,
    pub uinput: UInputDevice,
    pub device_path: String,
    pub fd: OwnedFd,
}

impl DeviceContext {
    /// Create a new device context by grabbing a device
    pub fn grab_device(device_path: &str) -> Result<Self, EvdevError> {
        // Open device with O_NONBLOCK so that the loop reading events eventually returns
        // due to an EAGAIN error
        let fd = open(
            device_path,
            OFlag::O_RDONLY | OFlag::O_NONBLOCK,
            Mode::empty(),
        ).map_err(|e| EvdevError::new(-(e as i32)))?;

        let fd = unsafe { OwnedFd::from_raw_fd(fd) };
        let mut evdev = EvdevDevice::new_from_fd(fd.as_raw_fd())?;

        // Grab the device
        evdev.grab()?;

        // Create uinput device for forwarding unconsumed events
        let uinput = UInputDevice::create_from_device(&evdev)?;

        Ok(Self {
            evdev,
            uinput,
            device_path: device_path.to_string(),
            fd,
        })
    }

    /// Write an event to the uinput device
    pub fn write_event(&self, event_type: u32, code: u32, value: i32) -> Result<(), EvdevError> {
        self.uinput.write_event(
            crate::enums::EventType::from_raw(event_type)
                .ok_or_else(|| EvdevError::new(-(Errno::EINVAL as i32)))?,
            code,
            value,
        )?;
        // Send SYN_REPORT
        self.uinput.write_event(
            crate::enums::EventType::Syn,
            bindings::SYN_REPORT,
            0,
        )?;
        Ok(())
    }

    /// Get the raw file descriptor for epoll operations
    pub fn fd(&self) -> RawFd {
        self.fd.as_raw_fd()
    }
    
    /// Get a borrowed reference to the file descriptor
    pub fn fd_borrowed(&self) -> BorrowedFd<'_> {
        self.fd.as_fd()
    }
}

impl Drop for DeviceContext {
    fn drop(&mut self) {
        // Ungrab the device
        let _ = self.evdev.ungrab();
        
        // uinput device is dropped automatically
    }
}

/// Check if a device path is a uinput device (should not be grabbed)
pub fn is_uinput_device(device_path: &str) -> bool {
    device_path.starts_with("/dev/input/event") && {
        // Check if it's a uinput device by trying to read its name
        // This is a heuristic - uinput devices typically have specific characteristics
        // For now, we'll check if we can open it and if it has certain properties
        // A better approach would be to check sysfs, but for simplicity we'll
        // rely on the caller to not pass uinput devices
        false // We'll rely on the caller to not pass uinput devices
    }
}

