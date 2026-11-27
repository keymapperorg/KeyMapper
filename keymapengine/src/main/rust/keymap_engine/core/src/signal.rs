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
        /// The conditions to cancel the key map while it is still pending
        cancel_if: Vec<CancelCondition>,
        /// Once the key map is active, these conditions will stop it.
        stop_when: Vec<StopCondition>,
    },

    InputReleased {
        input: InputRef,
        device: DeviceHandle,
    },
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub enum CancelCondition {
    /// A key map was advanced to the next key.
    Advanced {
        key_map: KeyMapId,
    },
    // A key map was performed.
    Performed {
        key_map: KeyMapId,
    },
    /// Cancel the pending key map when this input is released.
    Released {
        input: InputRef,
        device: DeviceHandle,
    },
}

#[derive(Copy, Clone, Debug, PartialEq)]
pub enum StopCondition {
    Released {
        input: InputRef,
        device: DeviceHandle,
    },
}

#[derive(Copy, Clone, Debug, PartialEq, Eq)]
pub enum PerformSignal {
    Start { key_map: KeyMapId },
    Stop { key_map: KeyMapId },
}
