#[derive(Hash, Eq, PartialEq, Clone)]
#[derive(Debug)]
pub struct DeviceIdentifier {
    pub(crate) name: String,
    pub(crate) vendor: u16,
    pub(crate) product: u16,
    pub(crate) version: u16,
}