use evdev::enums::EventCode;

use crate::evdev_device_info::EvdevDeviceInfo;

/// The information required to grab a device.
///
#[derive(Hash, Eq, PartialEq, Clone, Debug)]
pub struct GrabTarget {
    pub name: String,
    pub bus: u16,
    pub vendor: u16,
    pub product: u16,
    /// The extra event codes that should be enabled for the device. This is so that the
    /// uinput device can input events that the original device didn't support.
    pub extra_event_codes: Vec<EventCode>,
}

impl GrabTarget {
    pub fn matches_device_info(&self, device_info: &EvdevDeviceInfo) -> bool {
        device_info.name == self.name
            && device_info.bus == self.bus
            && device_info.vendor == self.vendor
            && device_info.product == self.product
    }
}
