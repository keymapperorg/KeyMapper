use crate::evdev_device_info::EvdevDeviceInfo;

/// Handle to a grabbed device with its assigned ID for O(1) lookup
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct GrabbedDeviceHandle {
    pub id: usize,
    pub device_info: EvdevDeviceInfo,
}

impl GrabbedDeviceHandle {
    pub fn new(id: usize, device_info: EvdevDeviceInfo) -> Self {
        Self { id, device_info }
    }
}
