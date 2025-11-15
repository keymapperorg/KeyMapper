use crate::bindings;
use crate::enums::{EvAbs, EventType, InputProp};
use std::error::Error;
use std::ffi::{c_void, CStr, CString};
use std::io::Error as IOError;
use std::io::ErrorKind;
use std::mem::MaybeUninit;
use std::os::fd::{AsRawFd, FromRawFd, IntoRawFd, OwnedFd};
use std::os::raw::c_int;
use std::ptr;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum EvdevErrorCode {
    NoSuchFileOrDirectory,
    IoError,
    NoSuchDevice,
    BadFileDescriptor,
    OutOfMemory,
    WouldBlock,
    PermissionDenied,
    InvalidArgument,
    Unknown(i32),
}

impl EvdevErrorCode {
    pub fn from_code(code: i32) -> Self {
        // libevdev returns negative errno values
        match -code as u32 {
            bindings::ENOENT => Self::NoSuchFileOrDirectory,
            bindings::EIO => Self::IoError,
            bindings::EBADF => Self::BadFileDescriptor,
            bindings::EAGAIN => Self::WouldBlock,
            bindings::ENOMEM => Self::OutOfMemory,
            bindings::EACCES => Self::PermissionDenied,
            bindings::ENODEV => Self::NoSuchDevice,
            bindings::EINVAL => Self::InvalidArgument,
            _ => Self::Unknown(code),
        }
    }

    pub fn to_code(self) -> i32 {
        -(match self {
            Self::NoSuchFileOrDirectory => bindings::ENOENT as i32,
            Self::IoError => bindings::EIO as i32,
            Self::BadFileDescriptor => bindings::EBADF as i32,
            Self::WouldBlock => bindings::EAGAIN as i32,
            Self::OutOfMemory => bindings::ENOMEM as i32,
            Self::PermissionDenied => bindings::EACCES as i32,
            Self::NoSuchDevice => bindings::ENODEV as i32,
            Self::InvalidArgument => bindings::EINVAL as i32,
            Self::Unknown(code) => return code,
        })
    }

    pub fn description(&self) -> &'static str {
        match self {
            Self::NoSuchFileOrDirectory => "No such file or directory (device not found)",
            Self::IoError => "Input/output error",
            Self::NoSuchDevice => "No such device",
            Self::BadFileDescriptor => "Bad file descriptor",
            Self::OutOfMemory => "Out of memory",
            Self::WouldBlock => "Resource temporarily unavailable",
            Self::PermissionDenied => "Permission denied",
            Self::InvalidArgument => "Invalid argument",
            Self::Unknown(_) => "Unknown error",
        }
    }
}

#[derive(Debug)]
pub struct EvdevError {
    kind: EvdevErrorCode,
    code: i32,
    message: String,
}

impl EvdevError {
    pub(crate) fn new(code: i32) -> Self {
        let kind = EvdevErrorCode::from_code(code);
        let message = if let EvdevErrorCode::Unknown(_) = kind {
            format!("libevdev error: {}", code)
        } else {
            format!("libevdev error: {} ({})", kind.description(), -code)
        };

        Self {
            kind,
            code,
            message,
        }
    }

    pub fn code(&self) -> i32 {
        self.code
    }

    pub fn kind(&self) -> EvdevErrorCode {
        self.kind
    }
}

impl std::fmt::Display for EvdevError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", self.message)
    }
}

impl Error for EvdevError {}

impl From<EvdevError> for IOError {
    fn from(err: EvdevError) -> Self {
        IOError::from_raw_os_error(-err.code)
    }
}

pub struct EvdevDevice {
    ptr: *mut bindings::libevdev,
    fd: Option<OwnedFd>,
}

impl EvdevDevice {
    /// Create a new libevdev device from a file descriptor
    /// Must use IntoRawFd so ownership is transferred, instead of borrowing and it
    /// automatically being closed.
    pub fn new_from_fd<F: IntoRawFd>(file: F) -> Result<Self, EvdevError> {
        let owned_fd = unsafe { OwnedFd::from_raw_fd(file.into_raw_fd()) };
        let mut dev: *mut bindings::libevdev = ptr::null_mut();

        let result = unsafe { bindings::libevdev_new_from_fd(owned_fd.as_raw_fd(), &mut dev) };

        if result < 0 {
            return Err(EvdevError::new(result));
        }

        Ok(Self {
            ptr: dev,
            fd: Some(owned_fd),
        })
    }

    /// Create a new libevdev device struct
    pub fn new() -> Result<Self, EvdevError> {
        let dev: *mut bindings::libevdev = unsafe { bindings::libevdev_new() };
        Ok(Self { ptr: dev, fd: None })
    }

    pub fn set_name(&mut self, name: &str) -> Result<(), Box<dyn Error>> {
        let name_cstr = CString::new(name)?;
        unsafe {
            bindings::libevdev_set_name(self.ptr, name_cstr.as_ptr());
        }
        Ok(())
    }

    /// Get the device name
    pub fn name(&self) -> String {
        let name_ptr = unsafe { bindings::libevdev_get_name(self.ptr) };

        if name_ptr.is_null() {
            String::new()
        } else {
            let name_cstr = unsafe { CStr::from_ptr(name_ptr) };
            name_cstr.to_string_lossy().into_owned()
        }
    }

    pub fn enable_property(&mut self, prop: InputProp) -> Result<(), Box<dyn Error>> {
        let result = unsafe { bindings::libevdev_enable_property(self.ptr, prop.into()) };

        if result < 0 {
            return Err(Box::new(EvdevError::new(result)));
        }

        Ok(())
    }

    /// Enable an event code.
    pub fn enable_event_code(
        &mut self,
        event_type: EventType,
        event_code: u32,
    ) -> Result<(), Box<dyn Error>> {
        if event_type == EventType::Abs {
            let error = IOError::new(
                ErrorKind::InvalidInput,
                "Enabling EV_ABS requires abs_info. Use enable_event_abs function.",
            );

            return Err(Box::new(error));
        }

        if event_type == EventType::Rep {
            let error = IOError::new(
                ErrorKind::InvalidInput,
                "EV_REP requires an integer is not supported in this wrapper. A dedicated function must be created.", );

            return Err(Box::new(error));
        }

        let result = unsafe {
            bindings::libevdev_enable_event_code(
                self.ptr,
                event_type.into(),
                event_code,
                ptr::null(),
            )
        };

        if result < 0 {
            return Err(Box::new(EvdevError::new(result)));
        }

        Ok(())
    }

    /// libevdev will automatically enable the event type.
    pub fn enable_event_abs(
        &mut self,
        abs_code: EvAbs,
        abs_info: impl Into<bindings::input_absinfo>,
    ) -> Result<(), EvdevError> {
        let abs_info_raw = abs_info.into();

        let result = unsafe {
            bindings::libevdev_enable_event_code(
                self.ptr,
                EventType::Abs.into(),
                abs_code.into(),
                ptr::from_ref(&abs_info_raw) as *const c_void,
            )
        };

        if result < 0 {
            return Err(EvdevError::new(result));
        }

        Ok(())
    }

    /// Grab the device through a kernel EVIOCGRAB
    ///
    /// This prevents other clients (including kernel-internal ones such as rfkill)
    /// from receiving events from this device.
    ///
    /// **Warning:** This is generally a bad idea. Use with caution.
    ///
    /// Grabbing an already grabbed device is a no-op and always succeeds.
    ///
    /// A grab is an operation tied to a file descriptor, not a device. If you
    /// change the file descriptor, you must also re-issue a grab.
    ///
    /// # Returns
    /// * `Ok(())` - If the device was successfully grabbed
    /// * `Err(EvdevError)` - If the grab failed
    pub fn grab(&mut self) -> Result<(), EvdevError> {
        self.set_grab_mode(bindings::libevdev_grab_mode_LIBEVDEV_GRAB)
    }

    /// Ungrab the device if currently grabbed
    ///
    /// This allows other clients to receive events from this device again.
    ///
    /// Ungrabbing an ungrabbed device is a no-op and always succeeds.
    ///
    /// # Returns
    /// * `Ok(())` - If the device was successfully ungrabbed
    /// * `Err(EvdevError)` - If the ungrab failed
    pub fn ungrab(&mut self) -> Result<(), EvdevError> {
        self.set_grab_mode(bindings::libevdev_grab_mode_LIBEVDEV_UNGRAB)
    }

    fn set_grab_mode(&mut self, mode: u32) -> Result<(), EvdevError> {
        let result = unsafe { bindings::libevdev_grab(self.ptr, mode) };

        if result < 0 {
            return Err(EvdevError::new(result));
        }

        Ok(())
    }

    /// Read the next event from the device (non-blocking)
    ///
    /// Returns `Ok(Some(EvdevEvent))` if an event was read,
    /// `Ok(None)` if no events are available (EAGAIN),
    /// or `Err(EvdevError)` on error.
    pub fn next_event(&mut self) -> Result<Option<EvdevEvent>, EvdevError> {
        let mut ev = MaybeUninit::<bindings::input_event>::uninit();

        let result = unsafe {
            bindings::libevdev_next_event(
                self.ptr,
                // TODO use same setup as sysbridge for reading from multiple devices at once in a blocking mannerz
                bindings::libevdev_read_flag_LIBEVDEV_READ_FLAG_BLOCKING,
                ev.as_mut_ptr(),
            )
        };

        match result {
            0.. => {
                let ev = unsafe { ev.assume_init() };
                Ok(Some(EvdevEvent {
                    time_sec: ev.time.tv_sec,
                    time_usec: ev.time.tv_usec,
                    event_type: EventType::from_raw(ev.type_ as u32).expect("Unknown event type"),
                    code: ev.code as u32,
                    value: ev.value,
                }))
            }
            -11 => Ok(None),
            _ => Err(EvdevError::new(result)),
        }
    }

    /// Get the raw pointer (for internal use)
    pub(crate) fn as_ptr(&self) -> *const bindings::libevdev {
        self.ptr as *const bindings::libevdev
    }
}

/// Represents a single evdev input event
#[derive(Debug, Clone, Copy)]
pub struct EvdevEvent {
    pub time_sec: i64,
    pub time_usec: i64,
    pub event_type: EventType,
    pub code: u32,
    pub value: i32,
}

impl Drop for EvdevDevice {
    fn drop(&mut self) {
        unsafe {
            if !self.ptr.is_null() {
                bindings::libevdev_free(self.ptr);
            }
        }

        // fd is dropped automatically
    }
}

pub struct UInputDevice {
    ptr: *mut bindings::libevdev_uinput,
}

impl UInputDevice {
    /// Create a uinput device from an existing libevdev device
    pub fn create_from_device(device: &EvdevDevice) -> Result<Self, EvdevError> {
        let mut uinput_dev: *mut bindings::libevdev_uinput = ptr::null_mut();

        let result = unsafe {
            bindings::libevdev_uinput_create_from_device(
                device.as_ptr(),
                bindings::libevdev_uinput_open_mode_LIBEVDEV_UINPUT_OPEN_MANAGED,
                &mut uinput_dev,
            )
        };

        if result < 0 {
            return Err(EvdevError::new(result));
        }

        Ok(Self { ptr: uinput_dev })
    }

    /// Get the device node path (e.g., "/dev/input/eventN")
    pub fn devnode(&self) -> Option<String> {
        let devnode_ptr = unsafe { bindings::libevdev_uinput_get_devnode(self.ptr) };

        if !devnode_ptr.is_null() {
            let devnode_cstr = unsafe { CStr::from_ptr(devnode_ptr) };
            Some(devnode_cstr.to_string_lossy().into_owned())
        } else {
            None
        }
    }

    /// Get the file descriptor for this uinput device
    pub fn fd(&self) -> c_int {
        unsafe { bindings::libevdev_uinput_get_fd(self.ptr) }
    }

    /// Write a generic event to the uinput device
    ///
    /// # Arguments
    /// * `event_type` - Event type (EV_ABS, EV_KEY, EV_SYN, etc.)
    /// * `event_code` - Event code (ABS_X, BTN_LEFT, SYN_REPORT, etc.)
    /// * `value` - Event value
    pub fn write_event(
        &self,
        event_type: EventType,
        event_code: u32,
        value: i32,
    ) -> Result<(), EvdevError> {
        let result = unsafe {
            bindings::libevdev_uinput_write_event(self.ptr, event_type.into(), event_code, value)
        };

        if result < 0 {
            return Err(EvdevError::new(result));
        }

        Ok(())
    }
}

impl Drop for UInputDevice {
    fn drop(&mut self) {
        unsafe {
            if !self.ptr.is_null() {
                bindings::libevdev_uinput_destroy(self.ptr);
            }
        }
    }
}
