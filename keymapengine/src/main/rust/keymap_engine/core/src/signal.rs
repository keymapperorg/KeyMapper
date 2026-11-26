use std::time::Duration;

use crate::{device::DeviceHandle, input::InputRef, tree::KeyMapId};

#[derive(Debug, Clone, PartialEq)]
pub enum SchedulerSignal {
    // Some trigger was advanced to the next key.
    KeyMapAdvanced {
        key_map: KeyMapId,
    },

    ScheduleKeyMap {
        key_map: KeyMapId,
        delay: Duration,
        cancel_if: Vec<CancelCondition>,
        stop_when: Vec<StopCondition>,
    },

    InputReleased {
        input: InputRef,
        device: DeviceHandle,
    },
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub enum CancelCondition {
    // TODO write a test that a pending key map is cancelled when a longer one advanced.
    // E.g key maps: A and A -> B -> C. A is scheduled when A is pressed without releasing, and then canceled when B pressed down.
    /// A key map was advanced to the next key.
    KeyMapAdvanced {
        key_map: KeyMapId,
    },
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub enum StopCondition {
    Released {
        input: InputRef,
        device: DeviceHandle,
    },
}

#[derive(Copy, Clone, Debug)]
pub enum PerformSignal {
    Start { key_map: KeyMapId },
    Stop { key_map: KeyMapId },
}
