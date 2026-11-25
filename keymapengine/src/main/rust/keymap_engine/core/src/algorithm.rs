use std::time::Duration;

use tokio::sync::mpsc::UnboundedSender;

use crate::{
    cursor::Cursor,
    device::{DeviceHandle, DeviceSelector},
    input::{Input, InputRef},
    signal::Signal,
    tree::{NodeId, Tree},
};

struct Algorithm {
    tree: Tree,
    cursors: Vec<Cursor>,
    output: UnboundedSender<Signal>,
}

impl Algorithm {
    fn new(tree: Tree, output: UnboundedSender<Signal>) -> Self {
        // TODO support multiple trees
        Self {
            tree,
            cursors: Vec::new(),
            output,
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
        let input = Input::now(input_ref, device);

        let mut matching_nodes: Vec<NodeId> = Vec::new();
        let root_node = self.tree.root_node();

        matching_nodes.extend(root_node.children_for_input(input_ref, device));

        for cursor in self.cursors.iter() {
            if let Some(node) = self.tree.get(cursor.node()) {
                matching_nodes.extend(node.children_for_input(input_ref, device));
            }
        }

        self.cursors
            .extend(matching_nodes.iter().map(|node| Cursor::new(*node)));

        // Deduplicate the cursors. If multiple point to the same node then keep the
        // most recent one.
        self.cursors.sort_by_key(|c| c.first_down_time());
        self.cursors.dedup_by_key(|c| c.node());

        let mut consume = false;

        for cursor in self.cursors.iter() {
            if let Some(node) = self.tree.get(cursor.node()) {
                for leaf in node.leaves() {
                    let signal = Signal::ActionDown {
                        key_map: leaf.key_map(),
                    };

                    consume |= self.output.send(signal).is_ok();
                }
            }
        }

        // TODO process the nodes and perform any actions. These cursors need to be cancelled.
        // TODO assert that the number of cursors is never greater than number of nodes.

        consume
    }

    // Returns whether to consume the input.
    fn on_input_up(&mut self, input_ref: InputRef, device: DeviceHandle) -> bool {
        // TODO do not consume an up event that never had a corresponding down event.
        // The world may have reloaded while a button was held down.

        false
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
}
