use crate::{
    tree::{KeyMapId, Tree},
    trigger::Trigger,
};

pub struct TreeBuilder {}

impl TreeBuilder {
    pub fn from_key_maps(triggers: &[Trigger]) -> Tree {
        // One key map is allowed to span multiple triggers because
        // any-order combo triggers are allowed. The scheduler must not
        // perform a key map while it is already running.

        let mut tree = Tree::new();

        for trigger in triggers {
            for input in trigger.inputs.iter() {

            }
        }

        tree
    }
}

#[cfg(test)]
mod tests {
    use pretty_assertions::assert_eq;

    use crate::{
        device::DeviceHandle,
        test_utils::{edge_any, edge_device, input},
        tree::{KeyMapId, NodeId, Tree},
        tree_builder::TreeBuilder,
        trigger::{Trigger, TriggerInput, TriggerMode},
    };

    /// Structural equality for asserting in tests. NodeId values are ignored.
    impl PartialEq for Tree {
        fn eq(&self, other: &Self) -> bool {
            subtree_eq(self, self.root(), other, other.root())
        }
    }

    fn subtree_eq(a: &Tree, a_id: NodeId, b: &Tree, b_id: NodeId) -> bool {
        let (x, y) = (a.get(a_id).unwrap(), b.get(b_id).unwrap());

        x.leaves().eq(y.leaves())
            && x.children_len() == y.children_len()
            && x.children()
                .zip(y.children())
                .all(|((ea, ca), (eb, cb))| ea == eb && subtree_eq(a, *ca, b, *cb))
    }

    fn sequence(key_map: KeyMapId, inputs: &[TriggerInput]) -> Trigger {
        Trigger {
            key_map,
            inputs: Vec::from(inputs),
            mode: TriggerMode::Sequence,
        }
    }

    fn input_any_device(input_char: char) -> TriggerInput {
        TriggerInput {
            input: input(input_char),
            device: None,
        }
    }

    fn input_device(input_char: char, device: DeviceHandle) -> TriggerInput {
        TriggerInput {
            input: input(input_char),
            device: Some(device),
        }
    }

    const DEVICE_A: DeviceHandle = DeviceHandle(0);
    const DEVICE_B: DeviceHandle = DeviceHandle(1);
    const DEVICE_C: DeviceHandle = DeviceHandle(2);

    #[test]
    fn empty_tree() {
        assert_eq!(TreeBuilder::from_key_maps(&[]), Tree::new());
    }

    #[test]
    fn single_key_trigger() {
        let trigger = sequence(KeyMapId(0), &[input_any_device('A')]);

        let expected = Tree::build(|root| {
            root.edge(edge_any('A', &[KeyMapId(0)]), |n| {
                n.leaf(KeyMapId(0));
            });
        });

        assert_eq!(TreeBuilder::from_key_maps(&[trigger]), expected);
    }

    #[test]
    fn merge_duplicate_one_key_triggers_into_same_branch() {
        let triggers = &[
            sequence(KeyMapId(0), &[input_any_device('A')]),
            sequence(KeyMapId(1), &[input_any_device('A')]),
            sequence(KeyMapId(2), &[input_any_device('A')]),
        ];

        let expected = Tree::build(|root| {
            root.edge(
                edge_any('A', &[KeyMapId(0), KeyMapId(1), KeyMapId(2)]),
                |n| {
                    n.leaf(KeyMapId(0));
                    n.leaf(KeyMapId(1));
                    n.leaf(KeyMapId(2));
                },
            );
        });

        assert_eq!(TreeBuilder::from_key_maps(triggers), expected);
    }

    #[test]
    fn separate_duplicate_one_key_triggers_with_different_devices() {
        let triggers = &[
            sequence(KeyMapId(0), &[input_device('A', DEVICE_A)]),
            sequence(KeyMapId(1), &[input_device('A', DEVICE_B)]),
            sequence(KeyMapId(2), &[input_any_device('A')]),
        ];

        let expected = Tree::build(|root| {
            root.edge(edge_device('A', DEVICE_A, &[KeyMapId(0)]), |n| {
                n.leaf(KeyMapId(0));
            });

            root.edge(edge_device('A', DEVICE_B, &[KeyMapId(1)]), |n| {
                n.leaf(KeyMapId(1));
            });

            root.edge(edge_any('A', &[KeyMapId(2)]), |n| {
                n.leaf(KeyMapId(2));
            });
        });

        assert_eq!(TreeBuilder::from_key_maps(triggers), expected);
    }

    #[test]
    fn merge_duplicate_two_key_triggers_into_same_branch() {
        let triggers = &[
            sequence(KeyMapId(0), &[input_any_device('A'), input_any_device('B')]),
            sequence(KeyMapId(1), &[input_any_device('A'), input_any_device('B')]),
            sequence(KeyMapId(2), &[input_any_device('A'), input_any_device('B')]),
        ];

        let expected = Tree::build(|root| {
            root.edge(
                edge_any('A', &[KeyMapId(0), KeyMapId(1), KeyMapId(2)]),
                |n| {
                    n.edge(
                        edge_any('B', &[KeyMapId(0), KeyMapId(1), KeyMapId(2)]),
                        |n| {
                            n.leaf(KeyMapId(0));
                            n.leaf(KeyMapId(1));
                            n.leaf(KeyMapId(2));
                        },
                    );
                },
            );
        });

        assert_eq!(TreeBuilder::from_key_maps(triggers), expected);
    }
}
