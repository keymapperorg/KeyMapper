use std::time::Instant;

use crate::device::{DeviceHandle, DeviceSelector};

pub struct Input {
    id: InputRef,
    device: DeviceHandle,
    time: Instant,
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub struct InputRef(pub u64);
