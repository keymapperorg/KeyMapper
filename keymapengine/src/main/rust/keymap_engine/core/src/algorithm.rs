use tokio::sync::mpsc::UnboundedSender;

use crate::{cursor::Cursor, device::Device, input::Input, signal::Signal, tree::Tree};

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

    fn on_input_down(&self, id: u64, device: Device) {}

    fn on_input_up(&self, id: u64, device: Device) {}
}

#[cfg(test)]
mod tests {
    use tokio::sync::mpsc::{self, error::TryRecvError, UnboundedReceiver};

    use crate::{algorithm::Algorithm, device::Device, signal::Signal, tree::Tree};

    fn test_device() -> Device {
        Device::InputDevice {
            descriptor: "device0".to_string(),
            name: "Test device".to_string(),
        }
    }

    fn create_algorithm(tree: Tree) -> (Algorithm, UnboundedReceiver<Signal>) {
        let (tx, rx) = mpsc::unbounded_channel();
        (Algorithm::new(tree, tx), rx)
    }

    #[tokio::test]
    async fn do_nothing_with_empty_tree() {
        let (algorithm, mut rx) = create_algorithm(Tree::new());

        algorithm.on_input_down(0, test_device());

        algorithm.on_input_up(0, test_device());

        assert_eq!(rx.try_recv(), Err(TryRecvError::Empty));
    }

    #[tokio::test]
    async fn do_nothing_if_input_not_in_tree() {
        todo!();
    }
}
