//! This crate is from evdev_rs https://github.com/ndesh26/evdev-rs.
//! It is copied so I can compile libevdev against the Android NDK and so that
//! a libevdev submodule isn't required. Also so more complex build steps can be removed.
//! Rust bindings to libevdev, a wrapper for evdev devices.

#[macro_use]
mod macros;

mod device;
pub mod enums;
mod libevdev;
mod uinput;
pub mod util;

use bitflags::bitflags;
use libc::{c_uint, suseconds_t, time_t};
use std::convert::{TryFrom, TryInto};
use std::time::{Duration, SystemTime, SystemTimeError, UNIX_EPOCH};

use enums::*;
use util::*;

pub use util::EventCodeIterator;
pub use util::EventTypeIterator;
pub use util::InputPropIterator;

#[doc(inline)]
pub use device::Device;
#[doc(inline)]
pub use device::DeviceWrapper;
#[doc(inline)]
pub use device::Enable;
#[doc(inline)]
pub use device::EnableCodeData;
#[doc(inline)]
pub use device::UninitDevice;
#[doc(inline)]
pub use uinput::UInputDevice;

pub enum GrabMode {
    /// Grab the device if not currently grabbed
    Grab = libevdev::LIBEVDEV_GRAB as isize,
    /// Ungrab the device if currently grabbed
    Ungrab = libevdev::LIBEVDEV_UNGRAB as isize,
}

bitflags! {
#[derive(PartialEq, Eq, PartialOrd, Ord, Hash, Debug, Clone, Copy)]
    pub struct ReadFlag: u32 {
        /// Process data in sync mode
        const SYNC = 1;
        /// Process data in normal mode
        const NORMAL = 2;
        /// Pretend the next event is a SYN_DROPPED and require the
        /// caller to sync
        const FORCE_SYNC = 4;
        /// The fd is not in O_NONBLOCK and a read may block
        const BLOCKING = 8;
    }
}

#[derive(PartialEq)]
pub enum ReadStatus {
    /// `next_event` has finished without an error and an event is available
    /// for processing.
    Success = libevdev::LIBEVDEV_READ_STATUS_SUCCESS as isize,
    /// Depending on the `next_event` read flag:
    /// libevdev received a SYN_DROPPED from the device, and the caller should
    /// now resync the device, or, an event has been read in sync mode.
    Sync = libevdev::LIBEVDEV_READ_STATUS_SYNC as isize,
}

pub enum LedState {
    /// Turn the LED on
    On = libevdev::LIBEVDEV_LED_ON as isize,
    /// Turn the LED off
    Off = libevdev::LIBEVDEV_LED_OFF as isize,
}

#[derive(Debug)]
pub struct DeviceId {
    pub bustype: BusType,
    pub vendor: u16,
    pub product: u16,
    pub version: u16,
}

#[derive(Clone, Copy, Debug)]
/// used by EVIOCGABS/EVIOCSABS ioctls
pub struct AbsInfo {
    /// latest reported value for the axis
    pub value: i32,
    /// specifies minimum value for the axis
    pub minimum: i32,
    /// specifies maximum value for the axis
    pub maximum: i32,
    /// specifies fuzz value that is used to filter noise from
    /// the event stream
    pub fuzz: i32,
    /// values that are within this value will be discarded by
    /// joydev interface and reported as 0 instead
    pub flat: i32,
    /// specifies resolution for the values reported for
    /// the axis
    pub resolution: i32,
}

impl AbsInfo {
    pub const fn from_raw(absinfo: libevdev::input_absinfo) -> AbsInfo {
        AbsInfo {
            value: absinfo.value,
            minimum: absinfo.minimum,
            maximum: absinfo.maximum,
            fuzz: absinfo.fuzz,
            flat: absinfo.flat,
            resolution: absinfo.resolution,
        }
    }

    pub const fn as_raw(&self) -> libevdev::input_absinfo {
        libevdev::input_absinfo {
            value: self.value,
            minimum: self.minimum,
            maximum: self.maximum,
            fuzz: self.fuzz,
            flat: self.flat,
            resolution: self.resolution,
        }
    }
}

#[derive(Copy, Clone, Eq, Hash, PartialOrd, Ord, Debug, PartialEq)]
pub struct TimeVal {
    pub tv_sec: time_t,
    pub tv_usec: suseconds_t,
}

impl TryFrom<SystemTime> for TimeVal {
    type Error = SystemTimeError;
    fn try_from(system_time: SystemTime) -> Result<Self, Self::Error> {
        let d = system_time.duration_since(UNIX_EPOCH)?;
        Ok(TimeVal {
            tv_sec: d.as_secs() as time_t,
            tv_usec: d.subsec_micros() as suseconds_t,
        })
    }
}

impl TryInto<SystemTime> for TimeVal {
    type Error = ();
    /// Fails if TimeVal.tv_usec is >= 10^6 or if the TimeVal is outside
    /// the range of SystemTime
    fn try_into(self) -> Result<SystemTime, Self::Error> {
        let secs = self.tv_sec.try_into().map_err(|_| ())?;
        let nanos = (self.tv_usec * 1000).try_into().map_err(|_| ())?;
        let duration = Duration::new(secs, nanos);
        UNIX_EPOCH.checked_add(duration).ok_or(())
    }
}

impl TimeVal {
    pub const fn new(tv_sec: time_t, tv_usec: suseconds_t) -> TimeVal {
        const MICROS_PER_SEC: suseconds_t = 1_000_000;
        TimeVal {
            tv_sec: tv_sec + (tv_usec / MICROS_PER_SEC) as time_t,
            tv_usec: tv_usec % MICROS_PER_SEC,
        }
    }

    pub const fn from_raw(timeval: &libc::timeval) -> TimeVal {
        TimeVal {
            tv_sec: timeval.tv_sec,
            tv_usec: timeval.tv_usec,
        }
    }

    pub const fn as_raw(&self) -> libc::timeval {
        libc::timeval {
            tv_sec: self.tv_sec,
            tv_usec: self.tv_usec,
        }
    }
}

/// The event structure itself
#[derive(Clone, Debug, PartialEq, Eq, Hash)]
pub struct InputEvent {
    /// The time at which event occured
    pub time: TimeVal,
    pub event_code: EventCode,
    pub value: i32,
}

impl InputEvent {
    pub const fn new(timeval: &TimeVal, code: &EventCode, value: i32) -> InputEvent {
        InputEvent {
            time: *timeval,
            event_code: *code,
            value,
        }
    }

    pub fn event_type(&self) -> Option<EventType> {
        int_to_event_type(event_code_to_int(&self.event_code).0)
    }

    pub fn from_raw(event: &libevdev::input_event) -> InputEvent {
        let ev_type = event.type_ as u32;
        let event_code = int_to_event_code(ev_type, event.code as u32);
        InputEvent {
            time: TimeVal::from_raw(&event.time),
            event_code,
            value: event.value,
        }
    }

    pub fn as_raw(&self) -> libevdev::input_event {
        let (ev_type, ev_code) = event_code_to_int(&self.event_code);
        libevdev::input_event {
            time: self.time.as_raw(),
            type_: ev_type as u16,
            code: ev_code as u16,
            value: self.value,
        }
    }

    pub fn is_type(&self, ev_type: &EventType) -> bool {
        unsafe { libevdev::libevdev_event_is_type(&self.as_raw(), *ev_type as c_uint) == 1 }
    }

    pub fn is_code(&self, code: &EventCode) -> bool {
        let (ev_type, ev_code) = event_code_to_int(code);

        unsafe { libevdev::libevdev_event_is_code(&self.as_raw(), ev_type, ev_code) == 1 }
    }
}
