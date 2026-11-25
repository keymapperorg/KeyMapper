use std::{collections::HashMap, sync::mpsc::Sender};

use crate::{
    signal::{ScheduleSignal, Until},
    tree::KeyMapId,
};

pub struct KeyMapScheduler {
    output: Sender<ScheduleSignal>,
    /// Which key maps are currently performing actions.
    active_key_maps: HashMap<KeyMapId, ActiveKeyMap>,
}

struct ActiveKeyMap {
    id: KeyMapId,
    until: Vec<Until>,
}

impl ActiveKeyMap {
    pub fn new(id: KeyMapId, cancel_hooks: &[Until]) -> Self {
        Self {
            id,
            until: Vec::from(cancel_hooks),
        }
    }
}

impl KeyMapScheduler {
    pub fn new(output: Sender<ScheduleSignal>) -> Self {
        Self {
            output,
            active_key_maps: HashMap::new(),
        }
    }

    pub fn process_signals(&self, signals: &[ScheduleSignal]) {
        // TODO parallel key maps have priority over sequence key maps. Is that checked here?
    }
}
