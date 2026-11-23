use std::collections::HashMap;

use slotmap::{new_key_type, SlotMap};

use crate::{
    device::{DeviceHandle, DeviceSelector},
    input::InputRef,
};

#[derive(Debug)]
pub struct Tree {
    root: NodeId,
    nodes: SlotMap<NodeId, Node>,
}

impl Tree {
    pub fn new() -> Self {
        let root = Node::new();
        let mut nodes: SlotMap<NodeId, Node> = SlotMap::default();

        let root_key = nodes.insert(root);

        Self {
            root: root_key,
            nodes,
        }
    }

    pub fn root(&self) -> &Node {
        self.get(self.root)
            .expect("The root node must always exist")
    }

    pub fn get(&self, id: NodeId) -> Option<&Node> {
        self.nodes.get(id)
    }
}

#[derive(Debug)]
pub struct Node {
    /// A list of child nodes and the input as the edge.
    children: Vec<(Edge, NodeId)>,
    leaves: Vec<Leaf>,
}

#[derive(Debug)]
struct Edge {
    input: InputRef,
    /// If None then this edge can match any device.
    device: Option<DeviceHandle>,
    /// These are used to check whether constraints are satisfied.
    key_maps: Vec<KeyMapId>,
}

impl Node {
    fn new() -> Self {
        Self {
            children: Vec::new(),
            leaves: Vec::new(),
        }
    }
}

#[derive(Copy, Clone, Debug)]
pub struct Leaf {
    id: NodeId,
    key_map: KeyMapId,
}

new_key_type! {
    pub struct NodeId;
}

new_key_type! {
    pub struct KeyMapId;
}
