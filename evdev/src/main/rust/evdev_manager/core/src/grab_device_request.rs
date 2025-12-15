use crate::device_identifier::DeviceIdentifier;

#[derive(Debug)]
pub struct GrabDeviceRequest {
    pub device_identifier: DeviceIdentifier,
    pub extra_key_codes: Vec<u32>,
}
