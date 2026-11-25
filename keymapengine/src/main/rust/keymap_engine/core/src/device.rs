/// An opaque handle on a device.
#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash)]
pub struct DeviceHandle(pub u64);

pub enum DeviceSelector {
    Evdev {
        name: String,
        bus: u16,
        vendor: u16,
        product: u16,
    },
    InputDevice {
        descriptor: String,
        name: String,
    },
}
