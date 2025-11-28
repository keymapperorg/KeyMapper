#[derive(Hash, Eq, PartialEq, Clone, Debug)]
pub struct DeviceIdentifier {
    pub name: String,
    pub bus: u16,
    pub vendor: u16,
    pub product: u16,
    /// Version is used internally for key layout matching but not exposed via JNI
    pub version: u16,
}
