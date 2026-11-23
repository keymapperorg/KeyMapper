use slotmap::{new_key_type, SlotMap};

use crate::device::Device;

pub struct Tree {
    nodes: SlotMap<NodeId, Node>,
}

impl Tree {
    pub fn new() -> Self {
        Self {
            nodes: SlotMap::default(),
        }
    }

    pub fn get(&self, id: NodeId) -> Option<&Node> {
        self.nodes.get(id)
    }
}

pub struct Node {
    id: NodeId,
    children: Vec<NodeId>,
    leaves: Vec<Leaf>,
    /// If None then this node can match any device.
    device: Option<Device>,
    /// These are used to check whether constraints are satisfied.
    key_maps: Vec<KeyMapId>,
}

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
