use std::time::Duration;

use crate::{
    cursor::Cursor,
    cursor_cleanup::cleanup_cursors,
    device::DeviceHandle,
    input::{Input, InputRef},
    signal::{ScheduleSignal, Until},
    tree::{Node, NodeId, Tree},
};

pub struct Detector {
    tree: Tree,
    cursors: Vec<Cursor>,
}

impl Detector {
    fn new(tree: Tree) -> Self {
        // TODO support multiple trees
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
    ) -> Vec<ScheduleSignal> {
        let input = Input::now(input_ref, device);

        let mut signals: Vec<ScheduleSignal> = Vec::new();

        // Add a cursor pointing to the root so when iterating over the cursors
        // it will also check if any edges can be followed.
        self.cursors.push(Cursor::new(self.tree.root()));

        let mut new_cursors: Vec<Cursor> = Vec::new();

        for cursor in self.cursors.iter_mut() {
            if let Some(node) = self.tree.get(cursor.node()) {
                let next_nodes: Vec<NodeId> = node.next_cursor_targets(input, cursor).collect();

                match next_nodes.as_slice() {
                    [] => continue,
                    [next] => {
                        cursor.advance(*next, input);
                    }
                    next_nodes => {
                        for next in next_nodes {
                            new_cursors.push(cursor.branch(*next, input));
                        }
                    }
                }

                signals.push(ScheduleSignal::TriggerAdvanced);
            }
        }

        self.cursors.extend(new_cursors);

        for cursor in self.cursors.iter_mut() {
            if let Some(node) = self.tree.get(cursor.node()) {
                for leaf in node.leaves() {
                    let key_map = leaf.key_map();

                    let delay: Duration = node.get_max_delay();

                    let signal = ScheduleSignal::ScheduleKeyMap {
                        key_map,
                        delay,
                        until: Vec::from(&[Until::Released {
                            input: input_ref,
                            device,
                        }]),
                    };

                    signals.push(signal);

                    cursor.cancel();
                }
            }
        }

        cleanup_cursors(&mut self.cursors);

        signals
    }

    pub fn on_input_up(
        &mut self,
        input_ref: InputRef,
        device: DeviceHandle,
    ) -> Vec<ScheduleSignal> {
        Vec::from(&[ScheduleSignal::Released {
            input: input_ref,
            device,
        }])
    }
}

impl Node {
    pub fn next_cursor_targets<'a>(
        &'a self,
        input: Input,
        cursor: &'a Cursor,
    ) -> impl Iterator<Item = NodeId> + 'a {
        self.children().filter_map(move |(edge, node)| {
            // An edge without a device matches any device.
            let input_matches =
                edge.input() == input.id() && edge.device().is_none_or(|d| d == input.device());

            // An edge without a timeout never expires.
            let is_active = cursor.is_active(edge.timeout().unwrap_or(Duration::MAX));

            (input_matches && is_active).then_some(*node)
        })
    }

    pub fn get_max_delay(&self) -> Duration {
        self.children()
            .filter_map(|(edge, _)| edge.timeout())
            .max()
            .unwrap_or_else(|| Duration::ZERO)
    }
}

#[cfg(test)]
mod tests {
    use std::time::Duration;

    use googletest::{assert_that, prelude::eq};
    use tokio::time::advance;

    use crate::{
        detector::Detector,
        device::DeviceHandle,
        input::InputRef,
        signal::{ScheduleSignal, Until},
        tree::{Edge, KeyMapId, Leaf, Tree},
    };

    const TEST_DEVICE: DeviceHandle = DeviceHandle(0);
    const TIMEOUT: Duration = Duration::from_millis(500);

    fn input(name: char) -> InputRef {
        InputRef(name as u64)
    }

    impl Detector {
        #[track_caller]
        fn assert_down_signals(&mut self, input: InputRef, expected: &[ScheduleSignal]) {
            assert_that!(self.on_input_down(input, TEST_DEVICE), eq(expected));
        }

        /// The input is not part of any trigger.
        #[track_caller]
        fn assert_down_ignored(&mut self, input: InputRef) {
            self.assert_down_signals(input, &[]);
        }

        /// The input is part of a trigger but does not complete it.
        #[track_caller]
        fn assert_down_advances_trigger(&mut self, input: InputRef) {
            self.assert_down_signals(input, &[ScheduleSignal::TriggerAdvanced]);
        }

        /// The input completes the trigger of `key_map`, which is scheduled
        /// to run after `delay` until the input is released.
        #[track_caller]
        fn assert_down_schedules(&mut self, input: InputRef, key_map: KeyMapId, delay: Duration) {
            self.assert_down_signals(
                input,
                &[
                    ScheduleSignal::TriggerAdvanced,
                    ScheduleSignal::ScheduleKeyMap {
                        key_map,
                        delay,
                        until: vec![Until::Released {
                            input,
                            device: TEST_DEVICE,
                        }],
                    },
                ],
            );
        }

        #[track_caller]
        fn assert_up_releases(&mut self, input: InputRef) {
            assert_that!(
                self.on_input_up(input, TEST_DEVICE),
                eq(&[ScheduleSignal::Released {
                    input,
                    device: TEST_DEVICE,
                }])
            );
        }
    }

    /// Key map 0 = Trigger 'A'
    fn single_key_tree() -> Tree {
        let mut tree = Tree::new();
        let node = tree.insert_node(
            tree.root(),
            Edge::with_any_device(input('A'), &[KeyMapId(0)]),
        );
        tree.insert_leaf(node, Leaf::new(KeyMapId(0)));
        tree
    }

    /// Key map 0 = Trigger 'A -> B'
    fn two_key_sequence_tree(second_edge: Edge) -> Tree {
        let mut tree = Tree::new();
        let first = tree.insert_node(
            tree.root(),
            Edge::with_any_device(input('A'), &[KeyMapId(0)]),
        );
        let second = tree.insert_node(first, second_edge);
        tree.insert_leaf(second, Leaf::new(KeyMapId(0)));
        tree
    }

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

        detector.assert_down_schedules(input('A'), KeyMapId(0), Duration::ZERO);
        detector.assert_up_releases(input('A'));
    }

    #[tokio::test]
    async fn detect_single_key_trigger_multiple_times() {
        let mut detector = Detector::new(single_key_tree());

        for _ in 0..3 {
            detector.assert_down_schedules(input('A'), KeyMapId(0), Duration::ZERO);
            detector.assert_up_releases(input('A'));
        }
    }

    #[tokio::test]
    async fn detect_two_key_sequence_trigger_releasing_each_key() {
        let tree = two_key_sequence_tree(Edge::with_any_device(input('B'), &[KeyMapId(0)]));
        let mut detector = Detector::new(tree);

        detector.assert_down_advances_trigger(input('A'));
        detector.assert_up_releases(input('A'));

        detector.assert_down_schedules(input('B'), KeyMapId(0), Duration::ZERO);
        detector.assert_up_releases(input('B'));
    }

    #[tokio::test]
    async fn detect_two_key_sequence_trigger() {
        let tree = two_key_sequence_tree(Edge::with_any_device(input('B'), &[KeyMapId(0)]));
        let mut detector = Detector::new(tree);

        detector.assert_down_advances_trigger(input('A'));
        detector.assert_down_schedules(input('B'), KeyMapId(0), Duration::ZERO);

        // The key map runs until B is released, so releasing A
        // does not stop it. The scheduler decides that from `until`.
        detector.assert_up_releases(input('A'));
        detector.assert_up_releases(input('B'));
    }

    #[tokio::test(start_paused = true)]
    async fn two_key_sequence_trigger_times_out() {
        let tree = two_key_sequence_tree(Edge::with_timeout(input('B'), &[KeyMapId(0)], TIMEOUT));
        let mut detector = Detector::new(tree);

        detector.assert_down_advances_trigger(input('A'));

        advance(TIMEOUT + Duration::from_millis(100)).await;

        detector.assert_down_ignored(input('B'));
    }

    #[tokio::test]
    async fn delay_key_map_until_longer_overlapping_sequence_trigger_times_out() {
        let mut tree = Tree::new();
        let first = tree.insert_node(
            tree.root(),
            Edge::with_any_device(input('A'), &[KeyMapId(0)]),
        );
        let second = tree.insert_node(
            first,
            Edge::with_timeout(input('B'), &[KeyMapId(1)], TIMEOUT),
        );
        tree.insert_leaf(first, Leaf::new(KeyMapId(0)));
        tree.insert_leaf(second, Leaf::new(KeyMapId(1)));

        let mut detector = Detector::new(tree);

        // Key map 0 waits for A -> B to time out. Waiting is the
        // scheduler's job, so only the delay is checked here.
        detector.assert_down_schedules(input('A'), KeyMapId(0), TIMEOUT);
        detector.assert_down_schedules(input('B'), KeyMapId(1), Duration::ZERO);
    }
}
