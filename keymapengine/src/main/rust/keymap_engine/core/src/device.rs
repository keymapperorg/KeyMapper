pub enum Device {
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
