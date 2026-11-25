use std::time::Duration;

use crate::{device::DeviceHandle, input::InputRef, tree::KeyMapId};

#[derive(Debug, Clone, PartialEq)]
pub enum ScheduleSignal {
    // Some trigger was advanced to the next key.
    TriggerAdvanced,
    ScheduleKeyMap {
        key_map: KeyMapId,
        delay: Duration,
        until: Vec<Until>,
    },
    Released {
        input: InputRef,
        device: DeviceHandle,
    },
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub enum Until {
    Released {
        input: InputRef,
        device: DeviceHandle,
    },
    KeyMapPerformed {
        key_map: KeyMapId,
    },
}

#[derive(Copy, Clone, Debug)]
pub enum PerformSignal {
    Start { key_map: KeyMapId },
    Stop { key_map: KeyMapId },
}
