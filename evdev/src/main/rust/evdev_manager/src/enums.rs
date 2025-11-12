use crate::bindings;
use std::fmt;

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
#[repr(u32)]
pub enum EventType {
    Syn = bindings::EV_SYN,
    Key = bindings::EV_KEY,
    Rel = bindings::EV_REL,
    Abs = bindings::EV_ABS,
    Msc = bindings::EV_MSC,
    Sw = bindings::EV_SW,
    Led = bindings::EV_LED,
    Snd = bindings::EV_SND,
    Rep = bindings::EV_REP,
    Ff = bindings::EV_FF,
    Pwr = bindings::EV_PWR,
    FfStatus = bindings::EV_FF_STATUS,
}

impl EventType {
    /// Convert a u32 value to an EventType
    pub fn from_raw(value: u32) -> Option<Self> {
        match value {
            bindings::EV_SYN => Some(Self::Syn),
            bindings::EV_KEY => Some(Self::Key),
            bindings::EV_REL => Some(Self::Rel),
            bindings::EV_ABS => Some(Self::Abs),
            bindings::EV_MSC => Some(Self::Msc),
            bindings::EV_SW => Some(Self::Sw),
            bindings::EV_LED => Some(Self::Led),
            bindings::EV_SND => Some(Self::Snd),
            bindings::EV_REP => Some(Self::Rep),
            bindings::EV_FF => Some(Self::Ff),
            bindings::EV_PWR => Some(Self::Pwr),
            bindings::EV_FF_STATUS => Some(Self::FfStatus),
            _ => None,
        }
    }

    /// Convert EventType to u32
    pub const fn as_raw(self) -> u32 {
        self as u32
    }
}

impl std::fmt::Display for EventType {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

impl From<EventType> for u32 {
    fn from(event_type: EventType) -> Self {
        event_type.as_raw()
    }
}

impl TryFrom<u32> for EventType {
    type Error = ();

    fn try_from(value: u32) -> Result<Self, Self::Error> {
        Self::from_raw(value).ok_or(())
    }
}

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
#[repr(u32)]
pub enum EvRel {
    X = bindings::REL_X,
    Y = bindings::REL_Y,
    Z = bindings::REL_Z,
    Rx = bindings::REL_RX,
    Ry = bindings::REL_RY,
    Rz = bindings::REL_RZ,
    Hwheel = bindings::REL_HWHEEL,
    Dial = bindings::REL_DIAL,
    Wheel = bindings::REL_WHEEL,
    Misc = bindings::REL_MISC,
    Reserved = bindings::REL_RESERVED,
    WheelHiRes = bindings::REL_WHEEL_HI_RES,
    HwheelHiRes = bindings::REL_HWHEEL_HI_RES,
}

impl EvRel {
    pub fn from_raw(value: u32) -> Option<Self> {
        match value {
            bindings::REL_X => Some(Self::X),
            bindings::REL_Y => Some(Self::Y),
            bindings::REL_Z => Some(Self::Z),
            bindings::REL_RX => Some(Self::Rx),
            bindings::REL_RY => Some(Self::Ry),
            bindings::REL_RZ => Some(Self::Rz),
            bindings::REL_HWHEEL => Some(Self::Hwheel),
            bindings::REL_DIAL => Some(Self::Dial),
            bindings::REL_WHEEL => Some(Self::Wheel),
            bindings::REL_MISC => Some(Self::Misc),
            bindings::REL_RESERVED => Some(Self::Reserved),
            bindings::REL_WHEEL_HI_RES => Some(Self::WheelHiRes),
            bindings::REL_HWHEEL_HI_RES => Some(Self::HwheelHiRes),
            _ => None,
        }
    }

    pub const fn as_raw(self) -> u32 {
        self as u32
    }
}

impl std::fmt::Display for EvRel {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

impl From<EvRel> for u32 {
    fn from(ev_rel: EvRel) -> Self {
        ev_rel.as_raw()
    }
}

impl TryFrom<u32> for EvRel {
    type Error = ();

    fn try_from(value: u32) -> Result<Self, Self::Error> {
        Self::from_raw(value).ok_or(())
    }
}

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
#[repr(u32)]
pub enum EvAbs {
    X = bindings::ABS_X,
    Y = bindings::ABS_Y,
    Z = bindings::ABS_Z,
    Rx = bindings::ABS_RX,
    Ry = bindings::ABS_RY,
    Rz = bindings::ABS_RZ,
    Throttle = bindings::ABS_THROTTLE,
    Rudder = bindings::ABS_RUDDER,
    Wheel = bindings::ABS_WHEEL,
    Gas = bindings::ABS_GAS,
    Brake = bindings::ABS_BRAKE,
    Hat0X = bindings::ABS_HAT0X,
    Hat0Y = bindings::ABS_HAT0Y,
    Hat1X = bindings::ABS_HAT1X,
    Hat1Y = bindings::ABS_HAT1Y,
    Hat2X = bindings::ABS_HAT2X,
    Hat2Y = bindings::ABS_HAT2Y,
    Hat3X = bindings::ABS_HAT3X,
    Hat3Y = bindings::ABS_HAT3Y,
    Pressure = bindings::ABS_PRESSURE,
    Distance = bindings::ABS_DISTANCE,
    TiltX = bindings::ABS_TILT_X,
    TiltY = bindings::ABS_TILT_Y,
    ToolWidth = bindings::ABS_TOOL_WIDTH,
    Volume = bindings::ABS_VOLUME,
    Profile = bindings::ABS_PROFILE,
    Misc = bindings::ABS_MISC,
    Reserved = bindings::ABS_RESERVED,
    MtSlot = bindings::ABS_MT_SLOT,
    MtTouchMajor = bindings::ABS_MT_TOUCH_MAJOR,
    MtTouchMinor = bindings::ABS_MT_TOUCH_MINOR,
    MtWidthMajor = bindings::ABS_MT_WIDTH_MAJOR,
    MtWidthMinor = bindings::ABS_MT_WIDTH_MINOR,
    MtOrientation = bindings::ABS_MT_ORIENTATION,
    MtPositionX = bindings::ABS_MT_POSITION_X,
    MtPositionY = bindings::ABS_MT_POSITION_Y,
    MtToolType = bindings::ABS_MT_TOOL_TYPE,
    MtBlobId = bindings::ABS_MT_BLOB_ID,
    MtTrackingId = bindings::ABS_MT_TRACKING_ID,
    MtPressure = bindings::ABS_MT_PRESSURE,
    MtDistance = bindings::ABS_MT_DISTANCE,
    MtToolX = bindings::ABS_MT_TOOL_X,
    MtToolY = bindings::ABS_MT_TOOL_Y,
}

impl EvAbs {
    pub fn from_raw(value: u32) -> Option<Self> {
        match value {
            bindings::ABS_X => Some(Self::X),
            bindings::ABS_Y => Some(Self::Y),
            bindings::ABS_Z => Some(Self::Z),
            bindings::ABS_RX => Some(Self::Rx),
            bindings::ABS_RY => Some(Self::Ry),
            bindings::ABS_RZ => Some(Self::Rz),
            bindings::ABS_THROTTLE => Some(Self::Throttle),
            bindings::ABS_RUDDER => Some(Self::Rudder),
            bindings::ABS_WHEEL => Some(Self::Wheel),
            bindings::ABS_GAS => Some(Self::Gas),
            bindings::ABS_BRAKE => Some(Self::Brake),
            bindings::ABS_HAT0X => Some(Self::Hat0X),
            bindings::ABS_HAT0Y => Some(Self::Hat0Y),
            bindings::ABS_HAT1X => Some(Self::Hat1X),
            bindings::ABS_HAT1Y => Some(Self::Hat1Y),
            bindings::ABS_HAT2X => Some(Self::Hat2X),
            bindings::ABS_HAT2Y => Some(Self::Hat2Y),
            bindings::ABS_HAT3X => Some(Self::Hat3X),
            bindings::ABS_HAT3Y => Some(Self::Hat3Y),
            bindings::ABS_PRESSURE => Some(Self::Pressure),
            bindings::ABS_DISTANCE => Some(Self::Distance),
            bindings::ABS_TILT_X => Some(Self::TiltX),
            bindings::ABS_TILT_Y => Some(Self::TiltY),
            bindings::ABS_TOOL_WIDTH => Some(Self::ToolWidth),
            bindings::ABS_VOLUME => Some(Self::Volume),
            bindings::ABS_PROFILE => Some(Self::Profile),
            bindings::ABS_MISC => Some(Self::Misc),
            bindings::ABS_RESERVED => Some(Self::Reserved),
            bindings::ABS_MT_SLOT => Some(Self::MtSlot),
            bindings::ABS_MT_TOUCH_MAJOR => Some(Self::MtTouchMajor),
            bindings::ABS_MT_TOUCH_MINOR => Some(Self::MtTouchMinor),
            bindings::ABS_MT_WIDTH_MAJOR => Some(Self::MtWidthMajor),
            bindings::ABS_MT_WIDTH_MINOR => Some(Self::MtWidthMinor),
            bindings::ABS_MT_ORIENTATION => Some(Self::MtOrientation),
            bindings::ABS_MT_POSITION_X => Some(Self::MtPositionX),
            bindings::ABS_MT_POSITION_Y => Some(Self::MtPositionY),
            bindings::ABS_MT_TOOL_TYPE => Some(Self::MtToolType),
            bindings::ABS_MT_BLOB_ID => Some(Self::MtBlobId),
            bindings::ABS_MT_TRACKING_ID => Some(Self::MtTrackingId),
            bindings::ABS_MT_PRESSURE => Some(Self::MtPressure),
            bindings::ABS_MT_DISTANCE => Some(Self::MtDistance),
            bindings::ABS_MT_TOOL_X => Some(Self::MtToolX),
            bindings::ABS_MT_TOOL_Y => Some(Self::MtToolY),
            _ => None,
        }
    }

    pub const fn as_raw(self) -> u32 {
        self as u32
    }
}

impl std::fmt::Display for EvAbs {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

impl From<EvAbs> for u32 {
    fn from(ev_abs: EvAbs) -> Self {
        ev_abs.as_raw()
    }
}

impl TryFrom<u32> for EvAbs {
    type Error = ();

    fn try_from(value: u32) -> Result<Self, Self::Error> {
        Self::from_raw(value).ok_or(())
    }
}

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
#[repr(u32)]
pub enum InputProp {
    Pointer = bindings::INPUT_PROP_POINTER,
    Direct = bindings::INPUT_PROP_DIRECT,
    Buttonpad = bindings::INPUT_PROP_BUTTONPAD,
    SemiMt = bindings::INPUT_PROP_SEMI_MT,
    TopButtonpad = bindings::INPUT_PROP_TOPBUTTONPAD,
    PointingStick = bindings::INPUT_PROP_POINTING_STICK,
    Accelerometer = bindings::INPUT_PROP_ACCELEROMETER,
}

impl InputProp {
    pub fn from_raw(value: u32) -> Option<Self> {
        match value {
            bindings::INPUT_PROP_POINTER => Some(Self::Pointer),
            bindings::INPUT_PROP_DIRECT => Some(Self::Direct),
            bindings::INPUT_PROP_BUTTONPAD => Some(Self::Buttonpad),
            bindings::INPUT_PROP_SEMI_MT => Some(Self::SemiMt),
            bindings::INPUT_PROP_TOPBUTTONPAD => Some(Self::TopButtonpad),
            bindings::INPUT_PROP_POINTING_STICK => Some(Self::PointingStick),
            bindings::INPUT_PROP_ACCELEROMETER => Some(Self::Accelerometer),
            _ => None,
        }
    }

    pub const fn as_raw(self) -> u32 {
        self as u32
    }
}

impl std::fmt::Display for InputProp {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

impl From<InputProp> for u32 {
    fn from(input_prop: InputProp) -> Self {
        input_prop.as_raw()
    }
}

impl TryFrom<u32> for InputProp {
    type Error = ();

    fn try_from(value: u32) -> Result<Self, Self::Error> {
        Self::from_raw(value).ok_or(())
    }
}

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
#[repr(u32)]
pub enum BusType {
    Pci = bindings::BUS_PCI,
    Isapnp = bindings::BUS_ISAPNP,
    Usb = bindings::BUS_USB,
    Hil = bindings::BUS_HIL,
    Bluetooth = bindings::BUS_BLUETOOTH,
    Virtual = bindings::BUS_VIRTUAL,
    Isa = bindings::BUS_ISA,
    I8042 = bindings::BUS_I8042,
    Xtkbd = bindings::BUS_XTKBD,
    Rs232 = bindings::BUS_RS232,
    Gameport = bindings::BUS_GAMEPORT,
    Parport = bindings::BUS_PARPORT,
    Amiga = bindings::BUS_AMIGA,
    Adb = bindings::BUS_ADB,
    I2c = bindings::BUS_I2C,
    Host = bindings::BUS_HOST,
    Gsc = bindings::BUS_GSC,
    Atari = bindings::BUS_ATARI,
    Spi = bindings::BUS_SPI,
    Rmi = bindings::BUS_RMI,
    Cec = bindings::BUS_CEC,
    IntelIshtp = bindings::BUS_INTEL_ISHTP,
    AmdSfh = bindings::BUS_AMD_SFH,
}

impl BusType {
    pub fn from_raw(value: u32) -> Option<Self> {
        match value {
            bindings::BUS_PCI => Some(Self::Pci),
            bindings::BUS_ISAPNP => Some(Self::Isapnp),
            bindings::BUS_USB => Some(Self::Usb),
            bindings::BUS_HIL => Some(Self::Hil),
            bindings::BUS_BLUETOOTH => Some(Self::Bluetooth),
            bindings::BUS_VIRTUAL => Some(Self::Virtual),
            bindings::BUS_ISA => Some(Self::Isa),
            bindings::BUS_I8042 => Some(Self::I8042),
            bindings::BUS_XTKBD => Some(Self::Xtkbd),
            bindings::BUS_RS232 => Some(Self::Rs232),
            bindings::BUS_GAMEPORT => Some(Self::Gameport),
            bindings::BUS_PARPORT => Some(Self::Parport),
            bindings::BUS_AMIGA => Some(Self::Amiga),
            bindings::BUS_ADB => Some(Self::Adb),
            bindings::BUS_I2C => Some(Self::I2c),
            bindings::BUS_HOST => Some(Self::Host),
            bindings::BUS_GSC => Some(Self::Gsc),
            bindings::BUS_ATARI => Some(Self::Atari),
            bindings::BUS_SPI => Some(Self::Spi),
            bindings::BUS_RMI => Some(Self::Rmi),
            bindings::BUS_CEC => Some(Self::Cec),
            bindings::BUS_INTEL_ISHTP => Some(Self::IntelIshtp),
            bindings::BUS_AMD_SFH => Some(Self::AmdSfh),
            _ => None,
        }
    }

    pub const fn as_raw(self) -> u32 {
        self as u32
    }
}

impl std::fmt::Display for BusType {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        write!(f, "{:?}", self)
    }
}

impl From<BusType> for u32 {
    fn from(bus_type: BusType) -> Self {
        bus_type.as_raw()
    }
}

impl TryFrom<u32> for BusType {
    type Error = ();

    fn try_from(value: u32) -> Result<Self, Self::Error> {
        Self::from_raw(value).ok_or(())
    }
}
