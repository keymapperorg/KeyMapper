#[derive(Hash, Eq, PartialEq, Clone, Debug)]
pub struct EvdevDeviceInfo {
    pub name: String,
    pub bus: u16,
    pub vendor: u16,
    pub product: u16,
    pub version: u16,
}
