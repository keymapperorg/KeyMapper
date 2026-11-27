use std::time::Duration;

use crate::{
    device::DeviceHandle,
    input::InputRef,
    tree::{Edge, KeyMapId, Leaf, NodeId, Tree},
};

pub fn input(name: char) -> InputRef {
    InputRef(name as u64)
}

pub fn edge_any(name: char, key_maps: &[KeyMapId]) -> Edge {
    Edge::with_any_device(input(name), key_maps)
}

pub fn edge_device(name: char, device: DeviceHandle, key_maps: &[KeyMapId]) -> Edge {
    Edge::with_device(input(name), device, key_maps)
}

pub fn edge_long_press(name: char, hold_duration: Duration, key_maps: &[KeyMapId]) -> Edge {
    Edge::with_long_press(input(name), hold_duration, key_maps)
}

pub fn edge_long_press_timeout(
    name: char,
    hold_duration: Duration,
    timeout: Duration,
    key_maps: &[KeyMapId],
) -> Edge {
    Edge::with_long_press_timeout(input(name), hold_duration, timeout, key_maps)
}

/// Key map 0 = Trigger 'A'
pub fn single_key_tree() -> Tree {
    Tree::build(|root| {
        root.edge(edge_any('A', &[KeyMapId(0)]), |n| {
            n.leaf(KeyMapId(0));
        });
    })
}

impl Tree {
    pub fn build(f: impl FnOnce(&mut NodeBuilder)) -> Tree {
        let mut tree = Tree::new();
        let root = tree.root();
        f(&mut NodeBuilder {
            tree: &mut tree,
            id: root,
        });
        tree
    }
}

pub struct NodeBuilder<'a> {
    tree: &'a mut Tree,
    id: NodeId,
}

impl NodeBuilder<'_> {
    pub fn edge(&mut self, edge: Edge, f: impl FnOnce(&mut NodeBuilder)) -> &mut Self {
        let id = self.tree.insert_node(self.id, edge);
        f(&mut NodeBuilder {
            tree: self.tree,
            id,
        });
        self
    }

    pub fn leaf(&mut self, km: KeyMapId) -> &mut Self {
        self.tree.insert_leaf(self.id, Leaf::new(km));
        self
    }
}
