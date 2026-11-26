use std::{collections::HashMap, sync::mpsc::Sender, time::Duration};

use tokio::time::Instant;

use crate::{
    device::DeviceHandle,
    input::InputRef,
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
    pub fn new() -> Self {
        Self {
            pending: Vec::new(),
            active_key_maps: HashMap::new(),
        }
    }

    pub fn process_signals(&mut self, signals: &[SchedulerSignal]) -> Vec<PerformSignal> {
        // TODO parallel key maps have priority over sequence key maps. Is that checked here?

        // TODO delay of zero means perform immediately now, do not schedule

        let mut output_signals: Vec<PerformSignal> = Vec::new();

        for signal in signals {
            match signal {
                SchedulerSignal::KeyMapAdvanced { key_map } => todo!(),
                SchedulerSignal::ScheduleKeyMap {
                    key_map,
                    delay,
                    cancel_if,
                    stop_when,
                } => {
                    if let Some(perform) =
                        self.schedule_key_map(*key_map, *delay, cancel_if, stop_when)
                    {
                        output_signals.push(perform);
                    }
                }
                SchedulerSignal::InputReleased { input, device } => {
                    self.input_released(*input, *device, &mut output_signals);
                }
            }
        }

        output_signals
    }

    fn schedule_key_map(
        &mut self,
        key_map: KeyMapId,
        delay: Duration,
        cancel_if: &[CancelCondition],
        stop_when: &[StopCondition],
    ) -> Option<PerformSignal> {
        if delay.is_zero() {
            self.active_key_maps.insert(key_map, Vec::from(stop_when));

            return PerformSignal::Start { key_map }.into();
        }

        None
    }

    fn input_released(
        &mut self,
        input: InputRef,
        device: DeviceHandle,
        output_signals: &mut Vec<PerformSignal>,
    ) {
        let stopped_key_maps: Vec<KeyMapId> = self
            .active_key_maps
            .iter()
            .filter_map(|(key_map, stop_conditions)| {
                stop_conditions
                    .contains(&StopCondition::Released { input, device })
                    .then_some(*key_map)
            })
            .collect();

        for key_map in stopped_key_maps {
            self.active_key_maps.remove(&key_map);
            output_signals.push(PerformSignal::Stop { key_map });
        }
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

#[cfg(test)]
mod tests {
    use std::time::Duration;

    use pretty_assertions::assert_eq;

    use crate::{
        device::DeviceHandle,
        input::InputRef,
        scheduler::KeyMapScheduler,
        signal::{CancelCondition, PerformSignal, SchedulerSignal, StopCondition},
        test_utils::input,
        tree::KeyMapId,
    };

    const TEST_DEVICE: DeviceHandle = DeviceHandle(0);

    fn schedule(
        key_map: KeyMapId,
        delay: Duration,
        cancel_if: &[CancelCondition],
        stop_when: &[StopCondition],
    ) -> SchedulerSignal {
        SchedulerSignal::ScheduleKeyMap {
            key_map,
            delay,
            cancel_if: cancel_if.to_vec(),
            stop_when: stop_when.to_vec(),
        }
    }

    fn input_released(input: InputRef, device: DeviceHandle) -> SchedulerSignal {
        SchedulerSignal::InputReleased { input, device }
    }

    fn stop_when_released(input: InputRef) -> StopCondition {
        StopCondition::Released {
            input,
            device: TEST_DEVICE,
        }
    }

    /// `key_map` runs immediately until `input` is released.
    fn schedule_until_released(key_map: KeyMapId, input: InputRef) -> SchedulerSignal {
        schedule(key_map, Duration::ZERO, &[], &[stop_when_released(input)])
    }

    fn start_signal(key_map: KeyMapId) -> PerformSignal {
        PerformSignal::Start { key_map }
    }

    fn stop_signal(key_map: KeyMapId) -> PerformSignal {
        PerformSignal::Stop { key_map }
    }

    #[test]
    fn schedule_key_map_with_no_delay_immediately() {
        let mut scheduler = KeyMapScheduler::new();

        let output = scheduler.process_signals(&[schedule_until_released(KeyMapId(0), input('A'))]);

        assert_eq!(output, &[start_signal(KeyMapId(0))]);

        assert_eq!(
            scheduler.active_key_maps.get(&KeyMapId(0)),
            Some(&vec![StopCondition::Released {
                input: input('A'),
                device: TEST_DEVICE
            }])
        );
    }

    #[test]
    fn stop_key_map_when_released() {
        let mut scheduler = KeyMapScheduler::new();

        scheduler.process_signals(&[schedule_until_released(KeyMapId(0), input('A'))]);

        let output = scheduler.process_signals(&[input_released(input('A'), TEST_DEVICE)]);

        assert_eq!(output, &[stop_signal(KeyMapId(0))]);
        assert_eq!(scheduler.active_key_maps.len(), 0);
    }
}
