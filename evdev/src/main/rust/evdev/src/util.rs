use crate::enums::*;
use crate::libevdev as raw;
use libc::{c_char, c_uint};
use log;
use log::warn;
use std::ffi::{CStr, CString};
use std::fmt;

pub(crate) unsafe fn ptr_to_str(ptr: *const c_char) -> Option<&'static str> {
    let slice = CStr::from_ptr(ptr.as_ref()?);
    let buf = slice.to_bytes();
    std::str::from_utf8(buf).ok()
}

pub struct EventTypeIterator {
    current: EventType,
}

pub struct EventCodeIterator {
    current: EventCode,
}

pub struct InputPropIterator {
    current: InputProp,
}

impl EventTypeIterator {
    pub fn new() -> Self {
        EventTypeIterator {
            current: EventType::EV_SYN,
        }
    }
}

impl EventCodeIterator {
    pub fn new(event_type: &EventType) -> Self {
        let event_code = match *event_type {
            EventType::EV_SYN => EventCode::EV_SYN(EV_SYN::SYN_REPORT),
            EventType::EV_KEY => EventCode::EV_KEY(EV_KEY::KEY_RESERVED),
            EventType::EV_REL => EventCode::EV_REL(EV_REL::REL_X),
            EventType::EV_ABS => EventCode::EV_ABS(EV_ABS::ABS_X),
            EventType::EV_MSC => EventCode::EV_MSC(EV_MSC::MSC_SERIAL),
            EventType::EV_SW => EventCode::EV_SW(EV_SW::SW_LID),
            EventType::EV_LED => EventCode::EV_LED(EV_LED::LED_NUML),
            EventType::EV_SND => EventCode::EV_SND(EV_SND::SND_CLICK),
            EventType::EV_REP => EventCode::EV_REP(EV_REP::REP_DELAY),
            EventType::EV_FF => EventCode::EV_FF(EV_FF::FF_STATUS_STOPPED),
            EventType::EV_FF_STATUS => EventCode::EV_FF_STATUS(EV_FF::FF_STATUS_STOPPED),
            _ => EventCode::EV_MAX,
        };

        EventCodeIterator {
            current: event_code,
        }
    }
}

impl InputPropIterator {
    pub fn new() -> Self {
        InputPropIterator {
            current: InputProp::INPUT_PROP_POINTER,
        }
    }
}

pub fn event_code_to_int(event_code: &EventCode) -> (c_uint, c_uint) {
    match *event_code {
        EventCode::EV_SYN(code) => (EventType::EV_SYN as c_uint, code as c_uint),
        EventCode::EV_KEY(code) => (EventType::EV_KEY as c_uint, code as c_uint),
        EventCode::EV_REL(code) => (EventType::EV_REL as c_uint, code as c_uint),
        EventCode::EV_ABS(code) => (EventType::EV_ABS as c_uint, code as c_uint),
        EventCode::EV_MSC(code) => (EventType::EV_MSC as c_uint, code as c_uint),
        EventCode::EV_SW(code) => (EventType::EV_SW as c_uint, code as c_uint),
        EventCode::EV_LED(code) => (EventType::EV_LED as c_uint, code as c_uint),
        EventCode::EV_SND(code) => (EventType::EV_SND as c_uint, code as c_uint),
        EventCode::EV_REP(code) => (EventType::EV_REP as c_uint, code as c_uint),
        EventCode::EV_FF(code) => (EventType::EV_FF as c_uint, code as c_uint),
        EventCode::EV_FF_STATUS(code) => (EventType::EV_FF_STATUS as c_uint, code as c_uint),
        EventCode::EV_UNK {
            event_type,
            event_code,
        } => (event_type as c_uint, event_code as c_uint),
        _ => {
            warn!("Event code not found");
            (0, 0)
        }
    }
}

pub fn int_to_event_code(event_type: c_uint, event_code: c_uint) -> EventCode {
    let ev_type: EventType = int_to_event_type(event_type as u32).unwrap();
    let code = event_code as u32;

    let ev_code = match ev_type {
        EventType::EV_SYN => int_to_ev_syn(code).map(EventCode::EV_SYN),
        EventType::EV_KEY => int_to_ev_key(code).map(EventCode::EV_KEY),
        EventType::EV_ABS => int_to_ev_abs(code).map(EventCode::EV_ABS),
        EventType::EV_REL => int_to_ev_rel(code).map(EventCode::EV_REL),
        EventType::EV_MSC => int_to_ev_msc(code).map(EventCode::EV_MSC),
        EventType::EV_SW => int_to_ev_sw(code).map(EventCode::EV_SW),
        EventType::EV_LED => int_to_ev_led(code).map(EventCode::EV_LED),
        EventType::EV_SND => int_to_ev_snd(code).map(EventCode::EV_SND),
        EventType::EV_REP => int_to_ev_rep(code).map(EventCode::EV_REP),
        EventType::EV_FF => int_to_ev_ff(code).map(EventCode::EV_FF),
        EventType::EV_PWR => Some(EventCode::EV_PWR),
        EventType::EV_FF_STATUS => int_to_ev_ff(code).map(EventCode::EV_FF_STATUS),
        EventType::EV_UNK => None,
        EventType::EV_MAX => Some(EventCode::EV_MAX),
    };

    ev_code.unwrap_or(EventCode::EV_UNK {
        event_type,
        event_code,
    })
}

impl fmt::Display for EventType {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "{}",
            unsafe { ptr_to_str(raw::libevdev_event_type_get_name(*self as c_uint)) }.unwrap_or("")
        )
    }
}

impl fmt::Display for EventCode {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        let (ev_type, ev_code) = event_code_to_int(self);
        write!(
            f,
            "{}",
            unsafe { ptr_to_str(raw::libevdev_event_code_get_name(ev_type, ev_code)) }
                .unwrap_or("")
        )
    }
}

impl fmt::Display for InputProp {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "{}",
            unsafe { ptr_to_str(raw::libevdev_property_get_name(*self as c_uint)) }.unwrap_or("")
        )
    }
}

impl EventType {
    pub fn iter(&self) -> EventTypeIterator {
        EventTypeIterator { current: *self }
    }

    /// The given type constant for the passed name or Errno if not found.
    pub fn from_str(name: &str) -> Option<EventType> {
        let name = CString::new(name).unwrap();
        let result = unsafe { raw::libevdev_event_type_from_name(name.as_ptr()) };

        match result {
            -1 => None,
            k => int_to_event_type(k as u32),
        }
    }

    /// The max value defined for the given event type, e.g. ABS_MAX for a type
    /// of EV_ABS, or Errno for an invalid type.
    pub fn get_max(ev_type: &EventType) -> Option<u32> {
        let result = unsafe { raw::libevdev_event_type_get_max(*ev_type as c_uint) };

        match result {
            k if k < 0 => None,
            k => Some(k as u32),
        }
    }
}

impl EventCode {
    pub fn iter(&self) -> EventCodeIterator {
        EventCodeIterator { current: *self }
    }

    /// Look up an event code by its type and name. Event codes start with a fixed
    /// prefix followed by their name (eg., "ABS_X"). The prefix must be included in
    /// the name. It returns the constant assigned to the event code or Errno if not
    /// found.
    pub fn from_str(ev_type: &EventType, name: &str) -> Option<EventCode> {
        let name = CString::new(name).unwrap();
        let result =
            unsafe { raw::libevdev_event_code_from_name(*ev_type as c_uint, name.as_ptr()) };

        match result {
            -1 => None,
            k => Some(int_to_event_code(*ev_type as u32, k as u32)),
        }
    }
}

impl InputProp {
    pub fn iter(&self) -> InputPropIterator {
        InputPropIterator { current: *self }
    }

    /// Look up an input property by its name. Properties start with the fixed
    /// prefix "INPUT_PROP_" followed by their name (eg., "INPUT_PROP_POINTER").
    /// The prefix must be included in the name. It returns the constant assigned
    /// to the property or Errno if not found.
    pub fn from_str(name: &str) -> Option<InputProp> {
        let name = CString::new(name).unwrap();
        let result = unsafe { raw::libevdev_property_from_name(name.as_ptr()) };

        match result {
            -1 => None,
            k => int_to_input_prop(k as u32),
        }
    }
}

// Iterator trait for the enum iterators
impl Iterator for EventTypeIterator {
    type Item = EventType;

    fn next(&mut self) -> Option<EventType> {
        match self.current {
            EventType::EV_MAX => None,
            _ => {
                let mut raw_code = (self.current as u32) + 1;
                loop {
                    match int_to_event_type(raw_code) {
                        // TODO: Find a way to iterate over Unknown types
                        Some(EventType::EV_UNK) => raw_code += 1,
                        Some(x) => {
                            let code = self.current;
                            self.current = x;
                            return Some(code);
                        }
                        None => raw_code += 1,
                    }
                }
            }
        }
    }
}

impl Iterator for EventCodeIterator {
    type Item = EventCode;

    fn next(&mut self) -> Option<EventCode> {
        match self.current {
            EventCode::EV_SYN(code) => match code {
                EV_SYN::SYN_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_syn(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_SYN(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_KEY(code) => match code {
                EV_KEY::KEY_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_key(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_KEY(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_REL(code) => match code {
                EV_REL::REL_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_rel(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_REL(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_ABS(code) => match code {
                EV_ABS::ABS_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_abs(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_ABS(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_MSC(code) => match code {
                EV_MSC::MSC_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_msc(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_MSC(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_SW(code) => match code {
                EV_SW::SW_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_sw(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_SW(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_LED(code) => match code {
                EV_LED::LED_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_led(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_LED(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_SND(code) => match code {
                EV_SND::SND_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_snd(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_SND(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_REP(code) => match code {
                EV_REP::REP_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_rep(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_REP(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            EventCode::EV_FF(code) => match code {
                EV_FF::FF_MAX => None,
                _ => {
                    let mut raw_code = (code as u32) + 1;
                    loop {
                        match int_to_ev_ff(raw_code) {
                            Some(x) => {
                                let ev_code = self.current;
                                self.current = EventCode::EV_FF(x);
                                return Some(ev_code);
                            }
                            None => raw_code += 1,
                        }
                    }
                }
            },
            _ => None,
        }
    }
}

impl Iterator for InputPropIterator {
    type Item = InputProp;

    fn next(&mut self) -> Option<InputProp> {
        match self.current {
            InputProp::INPUT_PROP_MAX => None,
            _ => {
                let mut raw_enum = (self.current as u32) + 1;
                loop {
                    match int_to_input_prop(raw_enum) {
                        Some(x) => {
                            let prop = self.current;
                            self.current = x;
                            return Some(prop);
                        }
                        None => raw_enum += 1,
                    }
                }
            }
        }
    }
}
