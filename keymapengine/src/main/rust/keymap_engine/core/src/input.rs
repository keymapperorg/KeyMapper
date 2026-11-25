
use tokio::time::Instant;

use crate::device::{DeviceHandle, DeviceSelector};

#[derive(Clone, Copy, Debug)]
pub struct Input {
    id: InputRef,
    device: DeviceHandle,
    time: Instant,
}

impl Input {
    pub fn now(id: InputRef, device: DeviceHandle) -> Self {
        Self {
            id,
            device,
            time: Instant::now(),
        }
    }
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub struct InputRef(pub u64);
