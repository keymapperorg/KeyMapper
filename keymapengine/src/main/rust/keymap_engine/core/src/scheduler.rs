use std::{collections::HashMap, sync::mpsc::Sender};

use tokio::time::Instant;

use crate::{
    signal::{CancelCondition, PerformSignal, SchedulerSignal, StopCondition},
    tree::KeyMapId,
};

pub struct KeyMapScheduler {
    pending: Vec<Pending>,
    /// Which key maps are currently performing actions.
    active_key_maps: HashMap<KeyMapId, Vec<StopCondition>>,
}

struct Pending {
    key_map: KeyMapId,
    deadline: Instant,
    cancel_if: Vec<CancelCondition>,
    stop_when: Vec<StopCondition>,
    /// Whether one of the stop conditions was met while it was pending. In this case,
    /// if it is still pending when the deadline is reached, then emit a down-up immediately.
    stopped_early: bool,
}

impl KeyMapScheduler {
    pub fn new(output: Sender<SchedulerSignal>) -> Self {
        Self {
            pending: Vec::new(),
            active_key_maps: HashMap::new(),
        }
    }

    pub fn process_signals(&self, signals: &[SchedulerSignal]) -> Vec<PerformSignal> {
        // TODO parallel key maps have priority over sequence key maps. Is that checked here?

        // TODO delay of zero means perform immediately now, do not schedule

        let output_signals: Vec<PerformSignal> = Vec::new();

        output_signals
    }

    pub fn on_tick(&mut self, now: Instant) -> Vec<PerformSignal> {
        // TODO Say trigger A is pending because A → B might follow, and the user releases A before the timeout.
        // Then send a down/up for A when the timeout is reached, and then the timeout is reached. Otherwise, if the timeout is reached and A is not released, then just send a down. And an up when the event gets an up.

        let output_signals: Vec<PerformSignal> = Vec::new();

        output_signals
    }

    pub fn next_deadline(&self) -> Option<Instant> {
        None
    }
}
