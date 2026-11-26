use std::sync::Arc;

use tokio::sync::Mutex;

use crate::{
    detector::Detector, performer::Performer, scheduler::KeyMapScheduler, trigger::Trigger,
};

pub struct Orchestrator {
    state: Mutex<State>,
    performer: Arc<dyn Performer>,
}

struct State {
    detector: Detector,
    scheduler: KeyMapScheduler,
}

impl State {
    pub fn empty() -> Self {
        Self {
            detector: Detector::empty(),
            scheduler: KeyMapScheduler::new(),
        }
    }
}

impl Orchestrator {
    pub fn new(performer: Arc<dyn Performer>) -> Self {
        Self {
            state: Mutex::new(State::empty()),
            performer,
        }
    }

    pub fn set_key_maps(&self, triggers: &[Trigger]) {}
}
