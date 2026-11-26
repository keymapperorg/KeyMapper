use std::time::Duration;

use crate::{
    cursor::Cursor,
    cursor_cleanup::cleanup_cursors,
    device::DeviceHandle,
    input::{Input, InputRef},
    signal::{CancelCondition, SchedulerSignal, StopCondition},
    tree::{Edge, Node, NodeId, Tree},
};

pub struct Detector {
    tree: Tree,
    cursors: Vec<Cursor>,
}

impl Detector {
    pub fn empty() -> Self {
        Self::new(Tree::new())
    }

    pub fn new(tree: Tree) -> Self {
        Self {
            tree,
            cursors: Vec::new(),
        }
    }

    fn on_device_connected(&self) {
        // TODO
        // Reset the world. Rebuild the tree, stop all actions.
        // Each node gets an opaque device handle so the algorithm doesn't need
        // to care about evdev or inputdevice concepts.
        // Move this method to a separate DeviceManager. Whoever builds the Tree
        // based off the KeyMaps can use the device manager.
    }

    fn on_device_disconnected(&self) {
        // TODO
        // Reset the world like in on_device_connected
    }

    /// Returns any signals to send to the
    /// key map scheduler.
    pub fn on_input_down(
        &mut self,
        input_ref: InputRef,
        device: DeviceHandle,
    ) -> Vec<SchedulerSignal> {
        let input = Input::now(input_ref, device);

        let mut signals: Vec<SchedulerSignal> = Vec::new();

        // Add a cursor pointing to the root so when iterating over the cursors
        // it will also check if any edges can be followed.
        self.cursors.push(Cursor::new(self.tree.root()));

        let mut new_cursors: Vec<Cursor> = Vec::new();

        for cursor in self.cursors.iter_mut() {
            if cursor.is_pending() {
                continue;
            }

            if let Some(node) = self.tree.get(cursor.node()) {
                let next_nodes: Vec<(&Edge, NodeId)> =
                    node.next_cursor_targets(input, cursor.clone()).collect();

                for (edge, next) in next_nodes.iter() {
                    // The cursor must wait for the long press
                    // before advancing to the node after this one.
                    let advanced = cursor.advance(*next, input, edge.hold_duration());
                    new_cursors.push(advanced);

                    for key_map in edge.key_maps() {
                        signals.push(SchedulerSignal::KeyMapAdvanced { key_map: *key_map });
                    }
                }

                // If all nodes are followed then the old cursor should
                // be canceled.
                if next_nodes.len() == node.children_len() {
                    cursor.cancel();
                }
            }
        }

        self.cursors.extend(new_cursors);

        for cursor in self.cursors.iter_mut() {
            if cursor.is_canceled() {
                continue;
            }

            if let Some(node) = self.tree.get(cursor.node()) {
                let longer_triggers = node.children().flat_map(|(edge, _)| edge.key_maps());

                let cancel_conditions: Vec<CancelCondition> = longer_triggers
                    .map(|key_map| CancelCondition::KeyMapAdvanced { key_map: *key_map })
                    .collect();

                for leaf in node.leaves() {
                    let key_map = leaf.key_map();

                    let delay: Duration = cursor.pending_time_remaining();

                    let stop_conditions = vec![StopCondition::Released {
                        input: input_ref,
                        device,
                    }];

                    let signal = SchedulerSignal::ScheduleKeyMap {
                        key_map,
                        delay,
                        cancel_if: cancel_conditions.clone(),
                        stop_when: stop_conditions,
                    };

                    signals.push(signal);
                }

                // Cancel the cursor if its reached the end of the tree.
                if node.children_len() == 0 {
                    cursor.cancel();
                }
            }
        }

        cleanup_cursors(&mut self.cursors);

        signals.dedup();

        signals
    }

    pub fn on_input_up(
        &mut self,
        input_ref: InputRef,
        device: DeviceHandle,
    ) -> Vec<SchedulerSignal> {
        Vec::from(&[SchedulerSignal::InputReleased {
            input: input_ref,
            device,
        }])
    }
}

impl Node {
    pub fn next_cursor_targets(
        &self,
        input: Input,
        cursor: Cursor,
    ) -> impl Iterator<Item = (&Edge, NodeId)> {
        self.children().filter_map(move |(edge, node)| {
            // An edge without a device matches any device.
            let input_matches =
                edge.input() == input.id() && edge.device().is_none_or(|d| d == input.device());

            // An edge without a timeout never expires.
            let is_active = cursor.is_active(edge.timeout().unwrap_or(Duration::MAX));

            (input_matches && is_active).then_some((edge, *node))
        })
    }
}

#[cfg(test)]
mod tests {
    use std::{time::Duration, vec};

    use pretty_assertions::assert_eq;
    use tokio::time::advance;

    use crate::{
        detector::Detector,
        device::DeviceHandle,
        input::InputRef,
        signal::{CancelCondition, SchedulerSignal, StopCondition},
        test_utils::{edge_any, edge_long_press, input, single_key_tree},
        tree::{Edge, KeyMapId, Tree},
    };

    const TEST_DEVICE: DeviceHandle = DeviceHandle(0);
    const TIMEOUT: Duration = Duration::from_millis(500);
    const LONG_PRESS: Duration = Duration::from_millis(400);

    fn signal_key_map_advanced(key_map: KeyMapId) -> SchedulerSignal {
        SchedulerSignal::KeyMapAdvanced { key_map }
    }

    fn cancel_if_key_map_advanced(key_map: KeyMapId) -> CancelCondition {
        CancelCondition::KeyMapAdvanced { key_map }
    }

    fn signal_schedule(
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

    /// `key_map` runs immediately until `input` is released.
    fn signal_schedule_until_released(key_map: KeyMapId, input: InputRef) -> SchedulerSignal {
        signal_schedule(key_map, Duration::ZERO, &[], &[stop_when_released(input)])
    }

    fn signal_schedule_long_press(
        key_map: KeyMapId,
        input: InputRef,
        delay: Duration,
    ) -> SchedulerSignal {
        signal_schedule(key_map, delay, &[], &[stop_when_released(input)])
    }

    fn stop_when_released(input: InputRef) -> StopCondition {
        StopCondition::Released {
            input,
            device: TEST_DEVICE,
        }
    }

    impl Detector {
        #[track_caller]
        fn assert_down_signals(&mut self, input: InputRef, expected: &[SchedulerSignal]) {
            assert_eq!(self.on_input_down(input, TEST_DEVICE), expected);
        }

        /// The input is not part of any trigger.
        #[track_caller]
        fn assert_down_ignored(&mut self, input: InputRef) {
            self.assert_down_signals(input, &[]);
        }

        #[track_caller]
        fn assert_up_releases(&mut self, input: InputRef) {
            assert_eq!(
                self.on_input_up(input, TEST_DEVICE),
                [SchedulerSignal::InputReleased {
                    input,
                    device: TEST_DEVICE,
                }]
            );
        }
    }

    /// Key map 0 = Trigger 'A -> B'
    fn two_key_sequence_tree(second_edge: Edge) -> Tree {
        Tree::build(|root| {
            root.edge(edge_any('A', &[KeyMapId(0)]), |n| {
                n.edge(second_edge, |n| {
                    n.leaf(KeyMapId(0));
                });
            });
        })
    }

    // TODO sort these tests categorized into separate files

    #[tokio::test]
    async fn do_nothing_with_empty_tree() {
        let mut detector = Detector::new(Tree::new());

        detector.assert_down_ignored(input('A'));
    }

    #[tokio::test]
    async fn do_nothing_if_input_not_in_tree() {
        let mut detector = Detector::new(single_key_tree());

        detector.assert_down_ignored(input('B'));
    }

    #[tokio::test]
    async fn detect_single_key_trigger() {
        let mut detector = Detector::new(single_key_tree());

        detector.assert_down_signals(
            input('A'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_until_released(KeyMapId(0), input('A')),
            ],
        );
        detector.assert_up_releases(input('A'));
    }

    #[tokio::test(start_paused = true)]
    async fn detect_long_press_trigger() {
        let tree = Tree::build(|root| {
            root.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                n.leaf(KeyMapId(0));
            });
        });

        let mut detector = Detector::new(tree);

        detector.assert_down_signals(
            input('A'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_long_press(KeyMapId(0), input('A'), LONG_PRESS),
            ],
        );

        advance(LONG_PRESS).await;

        detector.assert_up_releases(input('A'));
    }

    #[tokio::test(start_paused = true)]
    async fn timeout_first_key_in_double_long_press() {
        let tree = Tree::build(|root| {
            root.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                n.edge(edge_long_press('B', LONG_PRESS, &[KeyMapId(0)]), |n| {
                    n.leaf(KeyMapId(0));
                });
            });
        });

        let mut detector = Detector::new(tree);

        detector.assert_down_signals(input('A'), &[signal_key_map_advanced(KeyMapId(0))]);

        // A is not held down long enough
        advance(Duration::from_millis(50)).await;

        detector.assert_up_releases(input('A'));

        // Pressing B should not do anything because A was not long pressed.
        detector.assert_down_ignored(input('B'));

        advance(LONG_PRESS).await;
        // Do not advance time. Release immediately so it times out.
        detector.assert_up_releases(input('B'));
    }

    #[tokio::test(start_paused = true)]
    async fn timeout_last_key_in_double_long_press_same_key() {
        // Key map: A 400ms -> A 400ms -> $
        let tree = Tree::build(|root| {
            root.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                n.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                    n.leaf(KeyMapId(0));
                });
            });
        });

        let mut detector = Detector::new(tree);

        detector.assert_down_signals(input('A'), &[signal_key_map_advanced(KeyMapId(0))]);
        advance(LONG_PRESS).await;
        detector.assert_up_releases(input('A'));

        detector.assert_down_signals(
            input('A'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_long_press(KeyMapId(0), input('A'), LONG_PRESS),
            ],
        );

        // Do not advance time. Release immediately so it times out.
        detector.assert_up_releases(input('A'));
    }

    #[tokio::test(start_paused = true)]
    async fn detect_double_long_press_same_key() {
        let tree = Tree::build(|root| {
            root.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                n.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                    n.leaf(KeyMapId(0));
                });
            });
        });

        let mut detector = Detector::new(tree);

        detector.assert_down_signals(input('A'), &[signal_key_map_advanced(KeyMapId(0))]);
        advance(LONG_PRESS).await;
        detector.assert_up_releases(input('A'));

        detector.assert_down_signals(
            input('A'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_long_press(KeyMapId(0), input('A'), LONG_PRESS),
            ],
        );

        advance(LONG_PRESS).await;
        detector.assert_up_releases(input('A'));
    }

    #[tokio::test(start_paused = true)]
    async fn detect_double_long_press_different_keys() {
        let tree = Tree::build(|root| {
            root.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                n.edge(edge_long_press('B', LONG_PRESS, &[KeyMapId(0)]), |n| {
                    n.leaf(KeyMapId(0));
                });
            });
        });

        let mut detector = Detector::new(tree);

        detector.assert_down_signals(input('A'), &[signal_key_map_advanced(KeyMapId(0))]);
        advance(LONG_PRESS).await;
        detector.assert_up_releases(input('A'));

        detector.assert_down_signals(
            input('B'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_long_press(KeyMapId(0), input('B'), LONG_PRESS),
            ],
        );

        advance(LONG_PRESS).await;
        detector.assert_up_releases(input('B'));
    }

    #[tokio::test(start_paused = true)]
    async fn cancel_long_press_trigger() {
        let tree = Tree::build(|root| {
            root.edge(edge_long_press('A', LONG_PRESS, &[KeyMapId(0)]), |n| {
                n.leaf(KeyMapId(0));
            });
        });

        let mut detector = Detector::new(tree);

        detector.assert_down_signals(
            input('A'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_long_press(KeyMapId(0), input('A'), LONG_PRESS),
            ],
        );

        // Do not advance time. Immediately release.

        detector.assert_up_releases(input('A'));
    }

    #[tokio::test]
    async fn detect_overlapping_short_and_long_press_triggers() {
        panic!();
    }

    #[tokio::test]
    async fn detect_overlapping_short_and_long_and_double_press_triggers() {
        panic!();
    }

    #[tokio::test]
    async fn detect_single_key_trigger_multiple_times() {
        let mut detector = Detector::new(single_key_tree());

        for _ in 0..3 {
            detector.assert_down_signals(
                input('A'),
                &[
                    signal_key_map_advanced(KeyMapId(0)),
                    signal_schedule_until_released(KeyMapId(0), input('A')),
                ],
            );
            detector.assert_up_releases(input('A'));
        }
    }

    #[tokio::test]
    async fn detect_two_key_sequence_trigger_releasing_each_key() {
        let tree = two_key_sequence_tree(edge_any('B', &[KeyMapId(0)]));
        let mut detector = Detector::new(tree);

        detector.assert_down_signals(input('A'), &[signal_key_map_advanced(KeyMapId(0))]);
        detector.assert_up_releases(input('A'));

        detector.assert_down_signals(
            input('B'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_until_released(KeyMapId(0), input('B')),
            ],
        );
        detector.assert_up_releases(input('B'));
    }

    #[tokio::test]
    async fn detect_two_key_sequence_trigger() {
        let tree = two_key_sequence_tree(edge_any('B', &[KeyMapId(0)]));
        let mut detector = Detector::new(tree);

        detector.assert_down_signals(input('A'), &[signal_key_map_advanced(KeyMapId(0))]);
        detector.assert_down_signals(
            input('B'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_until_released(KeyMapId(0), input('B')),
            ],
        );

        // The key map runs until B is released, so releasing A
        // does not stop it. The scheduler decides that from `until`.
        detector.assert_up_releases(input('A'));
        detector.assert_up_releases(input('B'));
    }

    #[tokio::test(start_paused = true)]
    async fn two_key_sequence_trigger_times_out() {
        let tree = two_key_sequence_tree(Edge::with_timeout(input('B'), &[KeyMapId(0)], TIMEOUT));
        let mut detector = Detector::new(tree);

        detector.assert_down_signals(input('A'), &[signal_key_map_advanced(KeyMapId(0))]);

        advance(TIMEOUT + Duration::from_millis(100)).await;

        detector.assert_down_ignored(input('B'));
    }

    #[tokio::test]
    async fn two_sequence_triggers_sharing_the_first_key() {
        /*
         * Tree looks like
         *
         *           -> B -> $
         * root -> A
         *           -> C -> $
         */
        let tree = Tree::build(|root| {
            root.edge(edge_any('A', &[KeyMapId(0), KeyMapId(1)]), |n| {
                n.edge(edge_any('B', &[KeyMapId(0)]), |n| {
                    n.leaf(KeyMapId(0));
                });
                n.edge(edge_any('C', &[KeyMapId(1)]), |n| {
                    n.leaf(KeyMapId(1));
                });
            });
        });

        let mut detector = Detector::new(tree);

        detector.assert_down_signals(
            input('A'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_key_map_advanced(KeyMapId(1)),
            ],
        );

        detector.assert_down_signals(
            input('B'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_schedule_until_released(KeyMapId(0), input('B')),
            ],
        );

        detector.assert_down_signals(
            input('C'),
            &[
                signal_key_map_advanced(KeyMapId(1)),
                signal_schedule_until_released(KeyMapId(1), input('C')),
            ],
        );
    }

    #[tokio::test(start_paused = true)]
    async fn delay_key_map_until_longer_overlapping_sequence_trigger_times_out() {
        /*
         * Tree looks like
         *           -> $
         * root -> A
         *           -> B -> $
         */

        let tree = Tree::build(|root| {
            root.edge(edge_any('A', &[KeyMapId(0), KeyMapId(1)]), |n| {
                n.leaf(KeyMapId(0));
                n.edge(
                    Edge::with_timeout(input('B'), &[KeyMapId(1)], TIMEOUT),
                    |n| {
                        n.leaf(KeyMapId(1));
                    },
                );
            });
        });

        let mut detector = Detector::new(tree);

        // Key map 0 waits for A -> B to time out by cancelling
        // when Key map 1 advanced to B.
        detector.assert_down_signals(
            input('A'),
            &[
                signal_key_map_advanced(KeyMapId(0)),
                signal_key_map_advanced(KeyMapId(1)),
                signal_schedule(
                    KeyMapId(0),
                    TIMEOUT,
                    &[cancel_if_key_map_advanced(KeyMapId(1))],
                    &[stop_when_released(input('A'))],
                ),
            ],
        );

        // Trigger Key map 1: A -> B
        detector.assert_down_signals(
            input('B'),
            &[
                signal_key_map_advanced(KeyMapId(1)),
                signal_schedule_until_released(KeyMapId(1), input('B')),
            ],
        );
    }
}
