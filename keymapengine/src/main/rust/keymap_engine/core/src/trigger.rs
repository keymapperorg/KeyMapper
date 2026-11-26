use crate::{device::DeviceHandle, input::InputRef, tree::KeyMapId};

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Trigger {
    pub key_map: KeyMapId,
    pub inputs: Vec<TriggerInput>,
    pub mode: TriggerMode,
}

#[derive(Debug, Copy, Clone, PartialEq, Eq)]
pub struct TriggerInput {
    pub input: InputRef,
    pub device: Option<DeviceHandle>,
}

#[derive(Debug, Copy, Clone, PartialEq, Eq)]
pub enum TriggerMode {
    Combo,
    Sequence,
}
