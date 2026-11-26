use crate::{device::DeviceHandle, input::InputRef, tree::KeyMapId};

pub struct Trigger {
    key_map: KeyMapId,
    inputs: Vec<TriggerInput>,
    mode: TriggerMode,
}

pub struct TriggerInput {
    input: InputRef,
    device: Option<DeviceHandle>,
}

pub enum TriggerMode {
    Combo,
    Sequence,
}
