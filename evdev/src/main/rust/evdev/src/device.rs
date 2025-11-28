use crate::{AbsInfo, GrabMode, InputEvent, LedState, ReadFlag, ReadStatus, TimeVal};
use libc::{c_int, c_uint, c_void};
use std::ffi::CString;
use std::fs::File;
use std::fs::OpenOptions;
use std::io::Read;
use std::mem::ManuallyDrop;
use std::os::unix::fs::OpenOptionsExt;
use std::os::unix::io::{AsRawFd, FromRawFd, RawFd};
use std::path::Path;
use std::{io, ptr};

use crate::enums::*;
use crate::libevdev;
use crate::util::*;

/// Types that can be enabled on a DeviceWrapper (i.e. buttons, keys, relative motion)
///
/// Generally this method will not be called directly, but will insted be called through [Device::enable()](crate::Device::enable)
///
/// ```rust
/// # use evdev_rs::{UninitDevice, DeviceWrapper, Enable, enums::{EventCode, EV_REL::REL_X}};
/// let dev = UninitDevice::new().expect("Device creation failed");
/// dev.enable(EventCode::EV_REL(REL_X)).expect("Enable failed");
/// ```
///
/// If you need to enable a EV_ABS or EV_REP event code, use
/// [enable_event_code](crate::Device::enable_event_code), as this
/// implementation doesn't pass EV_ABS data.
pub trait Enable {
    fn enable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()>;
    fn disable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()>;
    fn has<D: DeviceWrapper>(&self, device: &D) -> bool;
}

impl Enable for InputProp {
    fn enable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()> {
        device.enable_property(self)
    }
    fn disable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()> {
        device.disable_property(self)
    }
    fn has<D: DeviceWrapper>(&self, device: &D) -> bool {
        device.has_property(self)
    }
}

impl Enable for EventType {
    fn enable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()> {
        device.enable_event_type(self)
    }
    fn disable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()> {
        device.disable_event_type(self)
    }
    fn has<D: DeviceWrapper>(&self, device: &D) -> bool {
        device.has_event_type(self)
    }
}

impl Enable for EventCode {
    fn enable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()> {
        device.enable_event_code(self, None)
    }
    fn disable<D: DeviceWrapper>(&self, device: &D) -> io::Result<()> {
        device.disable_event_code(self)
    }
    fn has<D: DeviceWrapper>(&self, device: &D) -> bool {
        device.has_event_code(self)
    }
}

/// Extra data for use with enable_event_code
#[derive(Clone, Copy, Debug)]
pub enum EnableCodeData {
    AbsInfo(AbsInfo),
    RepInfo(i32),
}

/// Abstraction over structs which contain an inner `*mut libevdev`
pub trait DeviceWrapper: Sized {
    fn raw(&self) -> *mut libevdev::libevdev;

    /// Forcibly enable an EventType/InputProp on this device, even if the underlying
    /// device does not support it. While this cannot make the device actually
    /// report such events, it will now return true for has().
    ///
    /// This is a local modification only affecting only this representation of
    /// this device.
    fn enable<E: Enable>(&self, e: E) -> io::Result<()> {
        e.enable(self)
    }

    /// Enables this property, a call to `set_file` will overwrite any previously set values
    ///
    /// Note: Please use the `enable` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn enable_property(&self, prop: &InputProp) -> io::Result<()> {
        let result =
            unsafe { libevdev::libevdev_enable_property(self.raw(), *prop as c_uint) as i32 };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Forcibly enable an event type on this device, even if the underlying
    /// device does not support it. While this cannot make the device actually
    /// report such events, it will now return true for libevdev_has_event_type().
    ///
    /// This is a local modification only affecting only this representation of
    /// this device.
    ///
    /// Note: Please use the `enable` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn enable_event_type(&self, ev_type: &EventType) -> io::Result<()> {
        let result =
            unsafe { libevdev::libevdev_enable_event_type(self.raw(), *ev_type as c_uint) };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Forcibly enable an event type on this device, even if the underlying
    /// device does not support it. While this cannot make the device actually
    /// report such events, it will now return true for libevdev_has_event_code().
    ///
    /// The last argument depends on the type and code:
    /// If type is EV_ABS, data must be a pointer to a struct input_absinfo
    /// containing the data for this axis.
    /// If type is EV_REP, data must be a pointer to a int containing the data
    /// for this axis.
    /// For all other types, the argument must be `None`.
    ///
    /// Note: Please use the `enable` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn enable_event_code(
        &self,
        ev_code: &EventCode,
        data: Option<EnableCodeData>,
    ) -> io::Result<()> {
        let data = match ev_code {
            EventCode::EV_ABS(_) => match data {
                Some(EnableCodeData::AbsInfo(info)) => &info.as_raw() as *const _ as *const c_void,
                _ => {
                    return Err(io::Error::new(
                        io::ErrorKind::InvalidInput,
                        "EventCode::EV_ABS must be paired with EnableCodeData::AbsInfo",
                    ))
                }
            },
            EventCode::EV_REP(_) => match data {
                Some(EnableCodeData::RepInfo(info)) => {
                    &libc::c_int::from(info) as *const _ as *const c_void
                }
                _ => {
                    return Err(io::Error::new(
                        io::ErrorKind::InvalidInput,
                        "EventCode::EV_REP must be paired with EnableCodeData::RepInfo",
                    ))
                }
            },
            _ => ptr::null(),
        };

        let (ev_type, ev_code) = event_code_to_int(ev_code);

        let result =
            unsafe { libevdev::libevdev_enable_event_code(self.raw(), ev_type, ev_code, data) };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Forcibly disable an EventType/EventCode on this device, even if the
    /// underlying device provides it. This effectively mutes the respective set of
    /// events. has() will return false for this EventType/EventCode
    ///
    /// In most cases, a caller likely only wants to disable a single code, not
    /// the whole type.
    ///
    /// Disabling EV_SYN will not work. In Peter's Words "Don't shoot yourself
    /// in the foot. It hurts".
    ///
    /// This is a local modification only affecting only this representation of
    /// this device.
    fn disable<E: Enable>(&self, d: E) -> io::Result<()> {
        d.disable(self)
    }

    /// Forcibly disable an event type on this device, even if the underlying
    /// device provides it. This effectively mutes the respective set of
    /// events. libevdev will filter any events matching this type and none will
    /// reach the caller. libevdev_has_event_type() will return false for this
    /// type.
    ///
    /// In most cases, a caller likely only wants to disable a single code, not
    /// the whole type. Use `disable_event_code` for that.
    ///
    /// Disabling EV_SYN will not work. In Peter's Words "Don't shoot yourself
    /// in the foot. It hurts".
    ///
    /// This is a local modification only affecting only this representation of
    /// this device.
    ///
    /// Note: Please use the `disable` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn disable_event_type(&self, ev_type: &EventType) -> io::Result<()> {
        let result =
            unsafe { libevdev::libevdev_disable_event_type(self.raw(), *ev_type as c_uint) };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }
    /// Forcibly disable an event code on this device, even if the underlying
    /// device provides it. This effectively mutes the respective set of
    /// events. libevdev will filter any events matching this type and code and
    /// none will reach the caller. `has_event_code` will return false for
    /// this code.
    ///
    /// Disabling all event codes for a given type will not disable the event
    /// type. Use `disable_event_type` for that.
    ///
    /// This is a local modification only affecting only this representation of
    /// this device.
    ///
    /// Disabling codes of type EV_SYN will not work. Don't shoot yourself in the
    /// foot. It hurts.
    ///
    /// Note: Please use the `disable` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn disable_event_code(&self, code: &EventCode) -> io::Result<()> {
        let (ev_type, ev_code) = event_code_to_int(code);
        let result = unsafe { libevdev::libevdev_disable_event_code(self.raw(), ev_type, ev_code) };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    fn disable_property(&self, prop: &InputProp) -> io::Result<()> {
        let result = unsafe { libevdev::libevdev_disable_property(self.raw(), (*prop) as c_uint) };
        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Returns `true` if device support the InputProp/EventType/EventCode and false otherwise
    fn has<E: Enable>(&self, e: E) -> bool {
        e.has(self)
    }

    /// Returns `true` if device support the property and false otherwise
    ///
    /// Note: Please use the `has` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn has_property(&self, prop: &InputProp) -> bool {
        unsafe { libevdev::libevdev_has_property(self.raw(), *prop as c_uint) != 0 }
    }

    /// Returns `true` is the device support this event type and `false` otherwise
    ///
    /// Note: Please use the `has` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn has_event_type(&self, ev_type: &EventType) -> bool {
        unsafe { libevdev::libevdev_has_event_type(self.raw(), *ev_type as c_uint) != 0 }
    }

    /// Return `true` is the device support this event type and code and `false` otherwise
    ///
    /// Note: Please use the `has` function instead. This function is only
    /// available for the sake of maintaining compatibility with libevdev.
    fn has_event_code(&self, code: &EventCode) -> bool {
        unsafe {
            let (ev_type, ev_code) = event_code_to_int(code);
            libevdev::libevdev_has_event_code(self.raw(), ev_type, ev_code) != 0
        }
    }

    string_getter!(
        #[doc = "Get device's name, as set by the kernel, or overridden by a call to `set_name`"],
        name, libevdev_get_name,
        #[doc = "Get device's physical location, as set by the kernel, or overridden by a call to `set_phys`"],
        phys, libevdev_get_phys,
        #[doc = "Get device's unique identifier, as set by the kernel, or overridden by a call to `set_uniq`"],
        uniq, libevdev_get_uniq
    );

    string_setter!(
        set_name,
        libevdev_set_name,
        set_phys,
        libevdev_set_phys,
        set_uniq,
        libevdev_set_uniq
    );

    product_getter!(
        product_id,
        libevdev_get_id_product,
        vendor_id,
        libevdev_get_id_vendor,
        bustype,
        libevdev_get_id_bustype,
        version,
        libevdev_get_id_version
    );

    product_setter!(
        set_product_id,
        libevdev_set_id_product,
        set_vendor_id,
        libevdev_set_id_vendor,
        set_bustype,
        libevdev_set_id_bustype,
        set_version,
        libevdev_set_id_version
    );

    /// Get the axis info for the given axis, as advertised by the kernel.
    ///
    /// Returns the `AbsInfo` for the given the code or None if the device
    /// doesn't support this code
    fn abs_info(&self, code: &EventCode) -> Option<AbsInfo> {
        let (_, ev_code) = event_code_to_int(code);
        let a = unsafe { libevdev::libevdev_get_abs_info(self.raw(), ev_code).as_ref()? };

        Some(AbsInfo {
            value: a.value,
            minimum: a.minimum,
            maximum: a.maximum,
            fuzz: a.fuzz,
            flat: a.flat,
            resolution: a.resolution,
        })
    }

    /// Change the abs info for the given EV_ABS event code, if the code exists.
    ///
    /// This function has no effect if `has_event_code` returns false for
    /// this code.
    fn set_abs_info(&self, code: &EventCode, absinfo: &AbsInfo) {
        let (_, ev_code) = event_code_to_int(code);

        unsafe {
            libevdev::libevdev_set_abs_info(self.raw(), ev_code, &absinfo.as_raw() as *const _);
        }
    }

    ///  Returns the current value of the event type.
    ///
    /// If the device supports this event type and code, the return value is
    /// set to the current value of this axis. Otherwise, `None` is returned.
    fn event_value(&self, code: &EventCode) -> Option<i32> {
        let mut value: i32 = 0;
        let (ev_type, ev_code) = event_code_to_int(code);
        let valid = unsafe {
            libevdev::libevdev_fetch_event_value(self.raw(), ev_type, ev_code, &mut value)
        };

        match valid {
            0 => None,
            _ => Some(value),
        }
    }

    /// Set the value for a given event type and code.
    ///
    /// This only makes sense for some event types, e.g. setting the value for
    /// EV_REL is pointless.
    ///
    /// This is a local modification only affecting only this representation of
    /// this device. A future call to event_value() will return this
    /// value, unless the value was overwritten by an event.
    ///
    /// If the device supports ABS_MT_SLOT, the value set for any ABS_MT_*
    /// event code is the value of the currently active slot. You should use
    /// `set_slot_value` instead.
    ///
    /// If the device supports ABS_MT_SLOT and the type is EV_ABS and the code is
    /// ABS_MT_SLOT, the value must be a positive number less then the number of
    /// slots on the device. Otherwise, `set_event_value` returns Err.
    fn set_event_value(&self, code: &EventCode, val: i32) -> io::Result<()> {
        let (ev_type, ev_code) = event_code_to_int(code);
        let result = unsafe {
            libevdev::libevdev_set_event_value(self.raw(), ev_type, ev_code, val as c_int)
        };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    abs_getter!(
        abs_minimum,
        libevdev_get_abs_minimum,
        abs_maximum,
        libevdev_get_abs_maximum,
        abs_fuzz,
        libevdev_get_abs_fuzz,
        abs_flat,
        libevdev_get_abs_flat,
        abs_resolution,
        libevdev_get_abs_resolution
    );

    abs_setter!(
        set_abs_minimum,
        libevdev_set_abs_minimum,
        set_abs_maximum,
        libevdev_set_abs_maximum,
        set_abs_fuzz,
        libevdev_set_abs_fuzz,
        set_abs_flat,
        libevdev_set_abs_flat,
        set_abs_resolution,
        libevdev_set_abs_resolution
    );

    /// Return the current value of the code for the given slot.
    ///
    /// If the device supports this event code, the return value is
    /// is set to the current value of this axis. Otherwise, or
    /// if the event code is not an ABS_MT_* event code, `None` is returned
    fn slot_value(&self, slot: u32, code: &EventCode) -> Option<i32> {
        let (_, ev_code) = event_code_to_int(code);
        let mut value: i32 = 0;
        let valid = unsafe {
            libevdev::libevdev_fetch_slot_value(self.raw(), slot as c_uint, ev_code, &mut value)
        };

        match valid {
            0 => None,
            _ => Some(value),
        }
    }

    /// Set the value for a given code for the given slot.
    ///
    /// This is a local modification only affecting only this representation of
    /// this device. A future call to `slot_value` will return this value,
    /// unless the value was overwritten by an event.
    ///
    /// This function does not set event values for axes outside the ABS_MT range,
    /// use `set_event_value` instead.
    fn set_slot_value(&self, slot: u32, code: &EventCode, val: i32) -> io::Result<()> {
        let (_, ev_code) = event_code_to_int(code);
        let result = unsafe {
            libevdev::libevdev_set_slot_value(self.raw(), slot as c_uint, ev_code, val as c_int)
        };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Get the number of slots supported by this device.
    ///
    /// The number of slots supported, or `None` if the device does not provide
    /// any slots
    ///
    /// A device may provide ABS_MT_SLOT but a total number of 0 slots. Hence
    /// the return value of `None` for "device does not provide slots at all"
    fn num_slots(&self) -> Option<i32> {
        let result = unsafe { libevdev::libevdev_get_num_slots(self.raw()) };

        match result {
            -1 => None,
            slots => Some(slots),
        }
    }

    /// Get the currently active slot.
    ///
    /// This may differ from the value an ioctl may return at this time as
    /// events may have been read off the file since changing the slot value
    /// but those events are still in the buffer waiting to be processed.
    /// The returned value is the value a caller would see if it were to
    /// process events manually one-by-one.
    fn current_slot(&self) -> Option<i32> {
        let result = unsafe { libevdev::libevdev_get_current_slot(self.raw()) };

        match result {
            -1 => None,
            slots => Some(slots),
        }
    }
}

/// Opaque struct representing an evdev device with no backing file
pub struct UninitDevice {
    raw: *mut libevdev::libevdev,
}

unsafe impl Send for UninitDevice {}

impl DeviceWrapper for UninitDevice {
    fn raw(&self) -> *mut libevdev::libevdev {
        self.raw
    }
}

impl UninitDevice {
    /// Initialize a new libevdev device.
    ///
    /// Generally you should use Device::new_from_file instead of this method
    /// This function only initializes the struct to sane default values.
    /// To actually hook up the device to a kernel device, use `set_file`.
    pub fn new() -> Option<UninitDevice> {
        let libevdev = unsafe { libevdev::libevdev_new() };

        if libevdev.is_null() {
            None
        } else {
            Some(UninitDevice { raw: libevdev })
        }
    }

    /// Set the file for this struct and initialize internal data.
    ///
    /// If the device changed and you need to re-read a device, use `Device::new_from_file` method.
    /// If you need to change the file after
    /// closing and re-opening the same device, use `change_file`.
    pub fn set_file(self, file: File) -> io::Result<Device> {
        // Don't call UninitDevice's destructor so we can reuse the inner libevdev
        let leak = ManuallyDrop::new(self);
        let result = unsafe { libevdev::libevdev_set_fd(leak.raw, file.as_raw_fd()) };
        match result {
            0 => Ok(Device {
                file,
                raw: leak.raw,
            }),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    #[deprecated(
        since = "0.5.0",
        note = "Prefer `set_file`. Some function names were changed so they
        more closely match their type signature. See issue 42 for discussion
        https://github.com/ndesh26/evdev-rs/issues/42"
    )]
    pub fn set_fd(self, file: File) -> io::Result<Device> {
        self.set_file(file)
    }
}

impl Drop for UninitDevice {
    fn drop(&mut self) {
        unsafe {
            libevdev::libevdev_free(self.raw);
        }
    }
}

impl std::fmt::Debug for UninitDevice {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        f.debug_struct("UninitDevice")
            .field("name", &self.name())
            .field("phys", &self.phys())
            .field("uniq", &self.uniq())
            .finish()
    }
}

/// Opaque struct representing an evdev device
///
/// Unlike libevdev, this `Device` maintains an associated file as an invariant
pub struct Device {
    file: File,
    raw: *mut libevdev::libevdev,
}

unsafe impl Send for Device {}

impl DeviceWrapper for Device {
    fn raw(&self) -> *mut libevdev::libevdev {
        self.raw
    }
}

impl Device {
    /// Initialize a new libevdev device from the given file.
    ///
    /// This is a shortcut for
    ///
    /// ```rust,no_run
    /// use evdev_rs::{Device, UninitDevice};
    /// # use std::fs::File;
    ///
    /// let uninit_device = UninitDevice::new().unwrap();
    /// # let file = File::open("/dev/input/event0").unwrap();
    /// let device = uninit_device.set_file(file);
    /// ```
    ///
    /// The caller is responsible for opening the file and setting
    /// the `O_NONBLOCK` flag and handling permissions.
    /// If the file is opened without O_NONBLOCK flag then next_event
    /// should be called with ReadFlag::BLOCKING. Due to the caching
    /// nature of next_event we might block while trying to buffer
    /// new events even though some events are already present.
    pub fn new_from_file(file: File) -> io::Result<Device> {
        let mut libevdev = std::ptr::null_mut();
        let result = unsafe { libevdev::libevdev_new_from_fd(file.as_raw_fd(), &mut libevdev) };

        match result {
            0 => Ok(Device {
                file,
                raw: libevdev,
            }),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    #[deprecated(
        since = "0.5.0",
        note = "Prefer `new_from_file`. Some function names were changed so they
        more closely match their type signature. See issue 42 for discussion
        https://github.com/ndesh26/evdev-rs/issues/42"
    )]
    pub fn new_from_fd(file: File) -> io::Result<Device> {
        Self::new_from_file(file)
    }

    /// Opens a device with the given path as the location of devnode
    ///
    /// The devnode file is opened with `O_NONBLOCK` and all the pending
    /// events are first read from the file before creating the device.
    pub fn new_from_path<P: AsRef<Path>>(path: P) -> io::Result<Device> {
        let mut file = OpenOptions::new()
            .read(true)
            .write(false) // Writing is only allowed when rooted so must be false
            .custom_flags(libc::O_NONBLOCK)
            .open(path)?;
        let mut buffer = [0u8; 20 * std::mem::size_of::<libevdev::input_event>()];

        let last_result = loop {
            let result = file.read(&mut buffer);
            if result.is_err() {
                break result;
            }
        };
        let _error_code = io::Error::from(io::ErrorKind::WouldBlock);
        match last_result {
            Err(_error_code) => Self::new_from_file(file),
            _ => Err(io::Error::new(
                io::ErrorKind::WouldBlock,
                "Unable to open file with O_NONBLOCK",
            )),
        }
    }

    /// Returns the file associated with the device
    pub fn file(&self) -> &File {
        &self.file
    }

    #[deprecated(
        since = "0.5.0",
        note = "Prefer `file`. This function can easily be misused. Calling
        this method, then dropping the returned file will lead to failures
        e.g. next_event will return an Err()"
    )]
    pub fn fd(&self) -> Option<File> {
        let result = unsafe { libevdev::libevdev_get_fd(self.raw) };
        match result {
            0 => None,
            _ => unsafe { Some(File::from_raw_fd(result)) },
        }
    }

    /// Change the file for this device, without re-reading the actual device.
    ///
    /// On success, returns the file that was previously associated with this
    /// device.
    ///
    /// If the file changes after initializing the device, for example after a
    /// VT-switch in the X.org X server, this function updates the internal
    /// file to the newly opened. No check is made that new file points to the
    /// same device. If the device has changed, evdev's behavior is undefined.
    ///
    /// evdev device does not sync itself after changing the file and keeps the
    /// current device state. Use next_event with the FORCE_SYNC flag to force
    /// a re-sync.
    ///
    /// # Example
    ///
    /// ```rust,no_run
    /// use evdev_rs::{Device, UninitDevice, ReadFlag, ReadStatus};
    /// # use std::fs::File;
    /// # fn hidden() -> std::io::Result<()> {
    /// let mut dev = Device::new_from_file(File::open("/dev/input/input0")?)?;
    /// dev.change_file(File::open("/dev/input/input1")?)?;
    /// dev.next_event(ReadFlag::FORCE_SYNC);
    /// while dev.next_event(ReadFlag::SYNC).ok().unwrap().0 == ReadStatus::Sync
    ///                             {} // noop
    /// # Ok(())
    /// # }
    /// ```
    /// After changing the file, the device is assumed ungrabbed and a caller must
    /// call libevdev_grab() again.
    pub fn change_file(&mut self, file: File) -> io::Result<File> {
        let result = unsafe { libevdev::libevdev_change_fd(self.raw, file.as_raw_fd()) };

        match result {
            0 => {
                let mut file = file;
                std::mem::swap(&mut file, &mut self.file);
                Ok(file)
            }
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    #[deprecated(
        since = "0.5.0",
        note = "Prefer new_from_file. Some function names were changed so they
        more closely match their type signature. See issue 42 for discussion
        https://github.com/ndesh26/evdev-rs/issues/42"
    )]
    pub fn change_fd(&mut self, file: File) -> io::Result<()> {
        self.change_file(file)?;
        Ok(())
    }

    /// Grab or ungrab the device through a kernel EVIOCGRAB.
    ///
    /// This prevents other clients (including kernel-internal ones such as
    /// rfkill) from receiving events from this device. This is generally a
    /// bad idea. Don't do this. Grabbing an already grabbed device, or
    /// ungrabbing an ungrabbed device is a noop and always succeeds.
    ///
    /// A grab is an operation tied to a file descriptor, not a device. If a
    /// client changes the file descriptor with Device::change_file(), it must
    /// also re-issue a grab with libevdev_grab().
    pub fn grab(&mut self, grab: GrabMode) -> io::Result<()> {
        let result = unsafe { libevdev::libevdev_grab(self.raw, grab as c_int) };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Check if there are events waiting for us.
    ///
    /// This function does not consume an event and may not access the device
    /// file at all. If there are events queued internally this function will
    /// return true. If the internal queue is empty, this function will poll
    /// the file descriptor for data.
    ///
    /// This is a convenience function for simple processes, most complex programs
    /// are expected to use select(2) or poll(2) on the file descriptor. The kernel
    /// guarantees that if data is available, it is a multiple of sizeof(struct
    /// input_event), and thus calling `next_event` when select(2) or
    /// poll(2) return is safe. You do not need `has_event_pending` if
    /// you're using select(2) or poll(2).
    pub fn has_event_pending(&self) -> bool {
        unsafe { libevdev::libevdev_has_event_pending(self.raw) > 0 }
    }

    /// Return the driver version of a device already intialize with `set_file`
    pub fn driver_version(&self) -> i32 {
        unsafe { libevdev::libevdev_get_driver_version(self.raw) as i32 }
    }

    /// Set the device's EV_ABS axis to the value defined in the abs
    /// parameter. This will be written to the kernel.
    pub fn set_kernel_abs_info(&self, code: &EventCode, absinfo: &AbsInfo) {
        let (_, ev_code) = event_code_to_int(code);

        unsafe {
            libevdev::libevdev_kernel_set_abs_info(
                self.raw,
                ev_code,
                &absinfo.as_raw() as *const _,
            );
        }
    }

    /// Turn an LED on or off.
    ///
    /// enabling an LED requires write permissions on the device's file descriptor.
    pub fn kernel_set_led_value(&self, code: &EventCode, value: LedState) -> io::Result<()> {
        let (_, ev_code) = event_code_to_int(code);
        let result =
            unsafe { libevdev::libevdev_kernel_set_led_value(self.raw, ev_code, value as c_int) };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Set the clock ID to be used for timestamps. Further events from this device
    /// will report an event time based on the given clock.
    ///
    /// This is a modification only affecting this representation of
    /// this device.
    pub fn set_clock_id(&self, clockid: i32) -> io::Result<()> {
        let result = unsafe { libevdev::libevdev_set_clock_id(self.raw, clockid) };

        match result {
            0 => Ok(()),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }

    /// Get the next event from the device. This function operates in two different
    /// modes: normal mode or sync mode.
    ///
    /// In normal mode (when flags has `evdev::NORMAL` set), this function returns
    /// `ReadStatus::Success` and returns the event. If no events are available at
    /// this time, it returns `-EAGAIN` as `Err`.
    ///
    /// If the current event is an `EV_SYN::SYN_DROPPED` event, this function returns
    /// `ReadStatus::Sync` and is set to the `EV_SYN` event.The caller should now call
    /// this function with the `evdev::SYNC` flag set, to get the set of events that
    /// make up the device state delta. This function returns ReadStatus::Sync for
    /// each event part of that delta, until it returns `-EAGAIN` once all events
    /// have been synced.
    ///
    /// If a device needs to be synced by the caller but the caller does not call
    /// with the `evdev::SYNC` flag set, all events from the diff are dropped after
    /// evdev updates its internal state and event processing continues as normal.
    /// Note that the current slot and the state of touch points may have updated
    /// during the `SYN_DROPPED` event, it is strongly recommended that a caller
    /// ignoring all sync events calls `current_slot` and checks the
    /// `ABS_MT_TRACKING_ID` values for all slots.
    ///
    /// If a device has changed state without events being enqueued in evdev,
    /// e.g. after changing the file descriptor, use the `evdev::FORCE_SYNC` flag.
    /// This triggers an internal sync of the device and `next_event` returns
    /// `ReadStatus::Sync`.
    pub fn next_event(&self, flags: ReadFlag) -> io::Result<(ReadStatus, InputEvent)> {
        let mut ev = libevdev::input_event {
            time: libevdev::timeval {
                tv_sec: 0,
                tv_usec: 0,
            },
            type_: 0,
            code: 0,
            value: 0,
        };

        let result =
            unsafe { libevdev::libevdev_next_event(self.raw, flags.bits() as c_uint, &mut ev) };

        let event = InputEvent {
            time: TimeVal {
                tv_sec: ev.time.tv_sec,
                tv_usec: ev.time.tv_usec,
            },
            event_code: int_to_event_code(ev.type_ as u32, ev.code as u32),
            value: ev.value,
        };

        match result {
            libevdev::LIBEVDEV_READ_STATUS_SUCCESS => Ok((ReadStatus::Success, event)),
            libevdev::LIBEVDEV_READ_STATUS_SYNC => Ok((ReadStatus::Sync, event)),
            error => Err(io::Error::from_raw_os_error(-error)),
        }
    }
}

impl Drop for Device {
    fn drop(&mut self) {
        unsafe {
            libevdev::libevdev_free(self.raw);
        }
    }
}

impl std::fmt::Debug for Device {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        f.debug_struct("Device")
            .field("name", &self.name())
            .field("phys", &self.phys())
            .field("uniq", &self.uniq())
            .finish()
    }
}

impl AsRawFd for Device {
    fn as_raw_fd(&self) -> RawFd {
        self.file.as_raw_fd()
    }
}
