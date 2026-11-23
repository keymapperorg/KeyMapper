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
        prelude::{is_false, is_true},
    };
    use tokio::sync::mpsc::{self, error::TryRecvError, UnboundedReceiver};

    use crate::{
        algorithm::Algorithm, device::DeviceHandle, input::InputRef, signal::Signal, tree::Tree,
    };

    const TEST_DEVICE: DeviceHandle = DeviceHandle(0);

    fn create_algorithm(tree: Tree) -> (Algorithm, UnboundedReceiver<Signal>) {
        let (tx, rx) = mpsc::unbounded_channel();
        (Algorithm::new(tree, tx), rx)
    }

    #[tokio::test]
    async fn do_nothing_with_empty_tree() {
        let (algorithm, mut rx) = create_algorithm(Tree::new());

        assert_that!(
            algorithm.on_input_down(InputRef(0), TEST_DEVICE),
            is_false()
        );

        assert_that!(algorithm.on_input_up(InputRef(0), TEST_DEVICE), is_false());

        assert_eq!(rx.try_recv(), Err(TryRecvError::Empty));
    }

    #[tokio::test]
    async fn do_nothing_if_input_not_in_tree() {
        todo!();
    }
}
