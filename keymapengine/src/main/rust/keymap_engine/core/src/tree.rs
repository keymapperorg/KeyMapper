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

    pub fn root(&self) -> NodeId {
        self.root
    }

    pub fn get(&self, id: NodeId) -> Option<&Node> {
        self.nodes.get(id)
    }

    pub fn insert_node(&mut self, parent: NodeId, edge: Edge) -> NodeId {
        let child = Node::new();
        let node_id = self.nodes.insert(child);

        let parent_node = self.nodes.get_mut(parent).expect("Parent node not found");
        parent_node.insert_child(edge, node_id);

        node_id
    }

    pub fn insert_leaf(&mut self, parent: NodeId, leaf: Leaf) {
        let parent_node = self.nodes.get_mut(parent).expect("Parent node not found");
        parent_node.insert_leaf(leaf);
    }
}

#[derive(Debug)]
pub struct Node {
    /// A list of child nodes and the input as the edge.
    children: Vec<(Edge, NodeId)>,
    leaves: Vec<Leaf>,
}

impl Node {
    fn new() -> Self {
        Self {
            children: Vec::new(),
            leaves: Vec::new(),
        }
    }

    fn insert_child(&mut self, edge: Edge, node: NodeId) {
        // TODO check that the same edge does not already exist.
        self.children.push((edge, node));
    }

    fn insert_leaf(&mut self, leaf: Leaf) {
        // TODO check that the same edge does not already exist.
        self.leaves.push(leaf);
    }
}

#[derive(Debug, PartialEq)]
pub struct Edge {
    input: InputRef,
    /// If None then this edge can match any device.
    device: Option<DeviceHandle>,
    /// These are used to check whether constraints are satisfied.
    key_maps: Vec<KeyMapId>,
}

impl Edge {
    pub fn with_device(input: InputRef, device: DeviceHandle, key_maps: &[KeyMapId]) -> Self {
        if key_maps.is_empty() {
            panic!("An edge must correspond to a key map");
        }

        Self {
            input,
            device: Some(device),
            key_maps: key_maps.into(),
        }
    }
    pub fn with_any_device(input: InputRef, key_maps: &[KeyMapId]) -> Self {
        if key_maps.is_empty() {
            panic!("An edge must correspond to a key map");
        }

        Self {
            input,
            device: None,
            key_maps: key_maps.into(),
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

#[derive(Copy, Clone, Debug, PartialEq)]
pub struct KeyMapId(pub u64);
