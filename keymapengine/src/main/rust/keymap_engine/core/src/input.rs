use std::time::Instant;

use crate::device::Device;

pub struct Input {
    id: u64,
    device: Device,
    time: Instant,
}
