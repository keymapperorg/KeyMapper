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

    #[cfg(test)]
    pub fn at(id: InputRef, device: DeviceHandle, time: Instant) -> Self {
        Self { id, device, time }
    }

    pub fn id(&self) -> InputRef {
        self.id
    }

    pub fn device(&self) -> DeviceHandle {
        self.device
    }

    pub fn time(&self) -> Instant {
        self.time
    }
}

#[derive(Copy, Clone, Debug, PartialEq, Hash, Eq)]
pub struct InputRef(pub u64);
