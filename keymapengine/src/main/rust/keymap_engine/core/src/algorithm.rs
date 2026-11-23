use crate::{cursor::Cursor, device::Device, input::Input, tree::Tree};

struct Algorithm {
    tree: Tree,
    cursors: Vec<Cursor>,
}

impl Algorithm {
    fn new(tree: Tree) -> Self {
        Self {
            tree,
            cursors: Vec::new(),
        }
    }

    fn on_input_down(&self, id: u64, device: Device) {}

    fn on_input_up(&self, id: u64, device: Device) {}
}

#[cfg(test)]
mod tests {
    use crate::{algorithm::Algorithm, device::Device, tree::Tree};

    fn test_device() -> Device {
        Device::InputDevice {
            descriptor: "device0".to_string(),
            name: "Test device".to_string(),
        }
    }

    #[test]
    fn do_nothing_with_empty_tree() {
        let algorithm = Algorithm::new(Tree::new());

        algorithm.on_input_down(0, test_device());

        algorithm.on_input_up(0, test_device());
    }

    #[test]
    fn do_nothing_if_input_not_in_tree() {
        todo!();
    }
}
