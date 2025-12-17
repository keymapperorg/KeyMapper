use crate::evdev_device_info::EvdevDeviceInfo;

#[derive(Debug)]
pub struct GrabDeviceRequest {
    pub device_identifier: EvdevDeviceInfo,
    pub extra_key_codes: Vec<u32>,
}
