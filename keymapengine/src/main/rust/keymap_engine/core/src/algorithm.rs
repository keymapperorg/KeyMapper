use tokio::sync::mpsc::UnboundedSender;

use crate::{
    cursor::Cursor,
    device::{DeviceHandle, DeviceSelector},
    input::{Input, InputRef},
    signal::Signal,
    tree::Tree,
};

struct Algorithm {
    tree: Tree,
    cursors: Vec<Cursor>,
    output: UnboundedSender<Signal>,
}

impl Algorithm {
    fn new(tree: Tree, output: UnboundedSender<Signal>) -> Self {
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
    fn on_input_down(&self, input: InputRef, device: DeviceHandle) -> bool {
        false
    }

    // Returns whether to consume the input.
    fn on_input_up(&self, input: InputRef, device: DeviceHandle) -> bool {
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
        tree::{Edge, KeyMapId, Tree},
    };

    const TEST_DEVICE: DeviceHandle = DeviceHandle(0);

    fn create_algorithm(tree: Tree) -> (Algorithm, UnboundedReceiver<Signal>) {
        let (tx, rx) = mpsc::unbounded_channel();
        (Algorithm::new(tree, tx), rx)
    }

    #[track_caller]
    fn assert_down_consumed(algorithm: &Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_down(input, TEST_DEVICE), is_true());
    }

    #[track_caller]
    fn assert_down_not_consumed(algorithm: &Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_down(input, TEST_DEVICE), is_false());
    }

    #[track_caller]
    fn assert_up_consumed(algorithm: &Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_up(input, TEST_DEVICE), is_true());
    }

    #[track_caller]
    fn assert_up_not_consumed(algorithm: &Algorithm, input: InputRef) {
        assert_that!(algorithm.on_input_up(input, TEST_DEVICE), is_false());
    }

    #[track_caller]
    fn assert_no_signal(rx: &mut UnboundedReceiver<Signal>) {
        assert_that!(rx.try_recv(), err(eq(&TryRecvError::Empty)));
    }

    #[tokio::test]
    async fn do_nothing_with_empty_tree() {
        let (algorithm, mut rx) = create_algorithm(Tree::new());

        assert_down_not_consumed(&algorithm, InputRef(0));
        assert_up_not_consumed(&algorithm, InputRef(0));

        assert_no_signal(&mut rx);
    }

    #[tokio::test]
    async fn do_nothing_if_input_not_in_tree() {
        let mut tree = Tree::new();

        let edge = Edge::with_any_device(InputRef(0), &[KeyMapId(0)]);
        tree.insert_node(tree.root(), edge);

        let (algorithm, mut rx) = create_algorithm(tree);

        assert_down_not_consumed(&algorithm, InputRef(0));
        assert_up_not_consumed(&algorithm, InputRef(0));

        assert_no_signal(&mut rx);
    }
}
