use std::{
    collections::{HashMap, HashSet},
    time::Duration,
};

use tokio::sync::mpsc::UnboundedSender;

use crate::{
    cursor::Cursor,
    cursor_cleanup::cleanup_cursors,
    device::{DeviceHandle, DeviceSelector},
    input::{Input, InputRef},
    signal::Signal,
    tree::{Edge, Node, NodeId, Tree},
};

struct Algorithm {
    tree: Tree,
    cursors: Vec<Cursor>,
    output: UnboundedSender<Signal>,
    /// Stores which inputs are being held down, and whether that down
    /// event was consumed.
    active_inputs: HashMap<(InputRef, DeviceHandle), bool>,
}

impl Algorithm {
    fn new(tree: Tree, output: UnboundedSender<Signal>) -> Self {
        // TODO support multiple trees
        Self {
            tree,
            cursors: Vec::new(),
            output,
            active_inputs: HashMap::new(),
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

    /// Returns whether to consume the input.
    fn on_input_down(&mut self, input_ref: InputRef, device: DeviceHandle) -> bool {
        // TODO do not process an input ref that is already held down
        if self.active_inputs.contains_key(&(input_ref, device)) {
            return false;
        }

        let input = Input::now(input_ref, device);

        // Add a cursor pointing to the root so when iterating over the cursors
        // it will also check if any edges can be followed.
        self.cursors.push(Cursor::new(self.tree.root()));

        let mut consume = false;
        let mut new_cursors: Vec<Cursor> = Vec::new();

        for cursor in self.cursors.iter_mut() {
            if let Some(node) = self.tree.get(cursor.node()) {
                let next_nodes: Vec<NodeId> = node.next_nodes_for_cursor(input, cursor).collect();

                match next_nodes.as_slice() {
                    [] => continue,
                    [next] => {
                        cursor.advance(*next, input);
                        consume = true;
                    }
                    next_nodes => {
                        for next in next_nodes {
                            new_cursors.push(cursor.branch(*next, input));
                        }
                        consume = true;
                    }
                }
            }
        }

        self.cursors.extend(new_cursors);

        for cursor in self.cursors.iter_mut() {
            if let Some(node) = self.tree.get(cursor.node()) {
                for leaf in node.leaves() {
                    let signal = Signal::ActionDown {
                        key_map: leaf.key_map(),
                    };

                    consume |= self.output.send(signal).is_ok();

                    cursor.cancel();
                }
            }
        }

        cleanup_cursors(&mut self.cursors);

        self.active_inputs.insert((input_ref, device), consume);

        consume
    }

    // Returns whether to consume the input.
    fn on_input_up(&mut self, input_ref: InputRef, device: DeviceHandle) -> bool {
        if !self.active_inputs.contains_key(&(input_ref, device)) {
            return false;
        }

        // Always consume the up event if the down was also consumed.
        self.active_inputs
            .remove(&(input_ref, device))
            .unwrap_or(false)
    }
}

impl Node {
    // TODO rename?
    pub fn next_nodes_for_cursor<'a>(
        &'a self,
        input: Input,
        cursor: &'a Cursor,
    ) -> impl Iterator<Item = NodeId> + '_ {
        self.children().filter_map(move |(edge, node)| {
            if edge.can_cursor_follow(input, cursor) {
                Some(*node)
            } else {
                None
            }
        })
    }
}

impl Edge {
    // TODO rename
    pub fn can_cursor_follow(&self, input: Input, cursor: &Cursor) -> bool {
        self.matches_input(input.id(), input.device()) && cursor.is_active(self.timeout())
    }
}

#[cfg(test)]
mod tests {
    use googletest::{
        assert_that,
        prelude::{eq, err, is_false, is_true},
    };
    use tokio::sync::mpsc::{self, error::TryRecvError, UnboundedReceiver};

    use crate::{
        algorithm::Algorithm,
        device::DeviceHandle,
        input::InputRef,
        signal::Signal,
        tree::{Edge, KeyMapId, Leaf, Tree},
    };

    const TEST_DEVICE: DeviceHandle = DeviceHandle(0);

    fn create_algorithm(tree: Tree) -> (Algorithm, UnboundedReceiver<Signal>) {
        let (tx, rx) = mpsc::unbounded_channel();
        (Algorithm::new(tree, tx), rx)
    }

    #[track_caller]
    fn assert_down_consumed(algorithm: &mut Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_down(input, TEST_DEVICE), is_true());
    }

    #[track_caller]
    fn assert_down_not_consumed(algorithm: &mut Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_down(input, TEST_DEVICE), is_false());
    }

    #[track_caller]
    fn assert_up_consumed(algorithm: &mut Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_up(input, TEST_DEVICE), is_true());
    }

    #[track_caller]
    fn assert_up_not_consumed(algorithm: &mut Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_up(input, TEST_DEVICE), is_false());
    }

    #[track_caller]
    fn assert_no_signal(rx: &mut UnboundedReceiver<Signal>) {
        assert_that!(rx.try_recv(), err(eq(&TryRecvError::Empty)));
    }

    #[track_caller]
    fn assert_signal(rx: &mut UnboundedReceiver<Signal>, expected: Signal) {
        assert_that!(rx.try_recv(), eq(&Ok(expected)));
    }

    fn input(name: char) -> InputRef {
        InputRef(name as u64)
    }

    #[tokio::test]
    async fn consume_up_event_if_down_was_consumed() {
        let (mut algorithm, _) = create_algorithm(Tree::new());

        algorithm
            .active_inputs
            .insert((input('A'), DeviceHandle(0)), true);

        assert_up_consumed(&mut algorithm, input('A'));
    }

    #[tokio::test]
    async fn do_nothing_with_empty_tree() {
        let (mut algorithm, mut rx) = create_algorithm(Tree::new());

        assert_down_not_consumed(&mut algorithm, input('A'));
        assert_up_not_consumed(&mut algorithm, input('A'));

        assert_no_signal(&mut rx);
    }

    #[tokio::test]
    async fn do_nothing_if_input_not_in_tree() {
        let mut tree = Tree::new();

        let edge = Edge::with_any_device(input('A'), &[KeyMapId(0)]);
        tree.insert_node(tree.root(), edge);

        let (mut algorithm, mut rx) = create_algorithm(tree);

        assert_down_not_consumed(&mut algorithm, input('B'));
        assert_up_not_consumed(&mut algorithm, input('B'));

        assert_no_signal(&mut rx);
    }

    #[tokio::test]
    async fn do_nothing_if_duplicate_down_event_received() {
        let mut tree = Tree::new();

        let edge = Edge::with_any_device(input('A'), &[KeyMapId(0)]);
        let node_id = tree.insert_node(tree.root(), edge);
        tree.insert_leaf(node_id, Leaf::new(KeyMapId(0)));

        let (mut algorithm, mut rx) = create_algorithm(tree);

        assert_down_consumed(&mut algorithm, input('A'));
        assert_signal(
            &mut rx,
            Signal::ActionDown {
                key_map: KeyMapId(0),
            },
        );

        assert_down_not_consumed(&mut algorithm, input('A'));
        assert_no_signal(&mut rx);
    }

    #[tokio::test]
    async fn do_nothing_if_up_event_received_with_no_corresponding_down_event() {
        todo!();
    }

    // TODO these tests should be higher level and not build a tree manually, so the key maps are easier to read.
    #[tokio::test]
    async fn detect_single_key_trigger() {
        let mut tree = Tree::new();

        let edge = Edge::with_any_device(input('A'), &[KeyMapId(0)]);
        let node_id = tree.insert_node(tree.root(), edge);
        tree.insert_leaf(node_id, Leaf::new(KeyMapId(0)));

        let (mut algorithm, mut rx) = create_algorithm(tree);

        assert_down_consumed(&mut algorithm, input('A'));

        assert_signal(
            &mut rx,
            Signal::ActionDown {
                key_map: KeyMapId(0),
            },
        );

        assert_up_consumed(&mut algorithm, input('A'));

        assert_signal(&mut rx, Signal::ActionUp);
    }

    #[tokio::test]
    async fn detect_single_key_trigger_multiple_times() {
        let mut tree = Tree::new();

        let edge = Edge::with_any_device(input('A'), &[KeyMapId(0)]);
        let node_id = tree.insert_node(tree.root(), edge);
        tree.insert_leaf(node_id, Leaf::new(KeyMapId(0)));

        let (mut algorithm, mut rx) = create_algorithm(tree);

        assert_down_consumed(&mut algorithm, input('A'));
        assert_signal(
            &mut rx,
            Signal::ActionDown {
                key_map: KeyMapId(0),
            },
        );

        assert_up_consumed(&mut algorithm, input('A'));
        assert_signal(&mut rx, Signal::ActionUp);

        assert_down_consumed(&mut algorithm, input('A'));
        assert_signal(
            &mut rx,
            Signal::ActionDown {
                key_map: KeyMapId(0),
            },
        );

        assert_up_consumed(&mut algorithm, input('A'));
        assert_signal(&mut rx, Signal::ActionUp);

        assert_down_consumed(&mut algorithm, input('A'));
        assert_signal(
            &mut rx,
            Signal::ActionDown {
                key_map: KeyMapId(0),
            },
        );
        assert_up_consumed(&mut algorithm, input('A'));
        assert_signal(&mut rx, Signal::ActionUp);
    }

    #[tokio::test]
    async fn detect_two_key_sequence_trigger_releasing_each_key() {
        let mut tree = Tree::new();

        let edge = Edge::with_any_device(input('A'), &[KeyMapId(0)]);
        let first_node = tree.insert_node(tree.root(), edge);

        let edge = Edge::with_any_device(input('B'), &[KeyMapId(0)]);
        let second_node = tree.insert_node(first_node, edge);

        tree.insert_leaf(second_node, Leaf::new(KeyMapId(0)));

        let (mut algorithm, mut rx) = create_algorithm(tree);

        assert_down_consumed(&mut algorithm, input('A'));
        assert_up_consumed(&mut algorithm, input('A'));

        assert_down_consumed(&mut algorithm, input('B'));
        assert_signal(
            &mut rx,
            Signal::ActionDown {
                key_map: KeyMapId(0),
            },
        );

        assert_up_consumed(&mut algorithm, input('B'));
        assert_signal(&mut rx, Signal::ActionUp);
    }

    #[tokio::test]
    async fn detect_two_key_sequence_trigger() {
        let mut tree = Tree::new();

        let edge = Edge::with_any_device(input('A'), &[KeyMapId(0)]);
        let first_node = tree.insert_node(tree.root(), edge);

        let edge = Edge::with_any_device(input('B'), &[KeyMapId(0)]);
        let second_node = tree.insert_node(first_node, edge);

        tree.insert_leaf(second_node, Leaf::new(KeyMapId(0)));

        let (mut algorithm, mut rx) = create_algorithm(tree);

        assert_down_consumed(&mut algorithm, input('A'));

        assert_down_consumed(&mut algorithm, input('B'));

        assert_signal(
            &mut rx,
            Signal::ActionDown {
                key_map: KeyMapId(0),
            },
        );

        assert_up_consumed(&mut algorithm, input('A'));
        // The action should only be cancelled when the last
        // key in the sequence is released.
        assert_no_signal(&mut rx);

        assert_up_consumed(&mut algorithm, input('B'));
        assert_signal(&mut rx, Signal::ActionUp);
    }
}
