use std::{collections::HashMap, sync::Arc};

use crate::{
    detector::{self, Detector},
    device::DeviceHandle,
    input::InputRef,
    scheduler::KeyMapScheduler,
};

pub struct InputGate {
    /// Stores which inputs are being held down, and whether that down
    /// event was consumed.
    active_inputs: HashMap<(InputRef, DeviceHandle), bool>,
    detector: Detector,
    scheduler: KeyMapScheduler,
}

impl InputGate {
    pub fn new(detector: Detector, scheduler: KeyMapScheduler) -> Self {
        Self {
            active_inputs: HashMap::new(),
            detector,
            scheduler,
        }
    }

    /// Track a down input and return whether to consume the event.
    pub fn on_input_down(&mut self, input: InputRef, device: DeviceHandle) -> bool {
        // Do not process an input that is already held down. This would
        // be an inconsistent event stream.
        if self.active_inputs.contains_key(&(input, device)) {
            return false;
        }

        // Immediately track the input.
        self.active_inputs.insert((input, device), false);

        let scheduler_signals = self.detector.on_input_down(input, device);
        let consume = !scheduler_signals.is_empty();

        self.active_inputs.insert((input, device), consume);

        self.scheduler.process_signals(&scheduler_signals);

        consume
    }

    pub fn on_input_up(&mut self, input: InputRef, device: DeviceHandle) -> bool {
        if !self.active_inputs.contains_key(&(input, device)) {
            return false;
        }

        let scheduler_signals = self.detector.on_input_up(input, device);
        self.scheduler.process_signals(&scheduler_signals);

        // Only consume the up event if the down was also consumed.
        self.active_inputs.remove(&(input, device)).unwrap_or(false)
    }
}
