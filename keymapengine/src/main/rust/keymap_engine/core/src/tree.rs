use std::{collections::HashMap, fmt, sync::mpsc::Iter, time::Duration};

use slotmap::{new_key_type, SlotMap};

use crate::{
    device::{DeviceHandle, DeviceSelector},
    input::InputRef,
};

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

    pub fn root_node(&self) -> &Node {
        self.get(self.root).unwrap()
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

    fn fmt_node(&self, f: &mut fmt::Formatter<'_>, id: NodeId, depth: usize) -> fmt::Result {
        let node = &self.nodes[id];
        let pad = "    ".repeat(depth);

        for leaf in &node.leaves {
            writeln!(f, "{pad}leaf {:?}", leaf.key_map)?;
        }

        for (edge, child) in &node.children {
            writeln!(f, "{pad}{edge:?}")?;
            self.fmt_node(f, *child, depth + 1)?;
        }

        Ok(())
    }
}

/// Prints the tree as an indented outline instead of dumping the SlotMap,
/// so failed assert_eq! diffs only show structural differences.
impl fmt::Debug for Tree {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        writeln!(f, "Tree")?;
        self.fmt_node(f, self.root, 1)
    }
}

#[derive(Debug, PartialEq, Eq)]
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

    pub fn children(&self) -> impl Iterator<Item = &(Edge, NodeId)> + '_ {
        self.children.iter()
    }

    pub fn children_len(&self) -> usize {
        self.children.len()
    }

    pub fn leaves(&self) -> impl Iterator<Item = &Leaf> + '_ {
        self.leaves.iter()
    }

    fn insert_child(&mut self, edge: Edge, node: NodeId) {
        // TODO check that the same edge does not already exist.
        //
        // TODO can multiple edges point to the same node? I think no. Look at my short, long, double press example on paper.
        self.children.push((edge, node));
    }

    fn insert_leaf(&mut self, leaf: Leaf) {
        // TODO check that the same edge does not already exist.
        self.leaves.push(leaf);
    }
}

#[derive(Clone, Debug, PartialEq, Eq)]
pub struct Edge {
    input: InputRef,
    /// If None then this edge can match any device.
    device: Option<DeviceHandle>,
    /// These are used to check whether constraints are satisfied.
    key_maps: Vec<KeyMapId>,
    /// How long the input needs to be long pressed for this edge
    /// to be followed.
    hold_duration: Duration,
    /// This is used for sequence trigger timeouts. A cursor
    /// can only follow this edge if this amount of time has not passed
    /// since the first input in the cursor went down.
    timeout: Option<Duration>,
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
            hold_duration: Duration::ZERO,
            timeout: None,
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
            hold_duration: Duration::ZERO,
            timeout: None,
        }
    }

    pub fn with_long_press(
        input: InputRef,
        hold_duration: Duration,
        key_maps: &[KeyMapId],
    ) -> Self {
        if key_maps.is_empty() {
            panic!("An edge must correspond to a key map");
        }

        Self {
            input,
            device: None,
            key_maps: key_maps.into(),
            hold_duration,
            timeout: None,
        }
    }

    pub fn with_long_press_timeout(
        input: InputRef,
        hold_duration: Duration,
        timeout: Duration,
        key_maps: &[KeyMapId],
    ) -> Self {
        if key_maps.is_empty() {
            panic!("An edge must correspond to a key map");
        }

        Self {
            input,
            device: None,
            key_maps: key_maps.into(),
            hold_duration,
            timeout: Some(timeout),
        }
    }

    pub fn with_timeout(input: InputRef, key_maps: &[KeyMapId], timeout: Duration) -> Self {
        if key_maps.is_empty() {
            panic!("An edge must correspond to a key map");
        }

        Self {
            input,
            device: None,
            key_maps: key_maps.into(),
            hold_duration: Duration::ZERO,
            timeout: Some(timeout),
        }
    }

    pub fn key_maps(&self) -> impl Iterator<Item = &KeyMapId> {
        self.key_maps.iter()
    }

    pub fn hold_duration(&self) -> Duration {
        self.hold_duration
    }

    pub fn timeout(&self) -> Option<Duration> {
        self.timeout
    }

    pub fn input(&self) -> InputRef {
        self.input
    }

    pub fn device(&self) -> Option<DeviceHandle> {
        self.device
    }
}

#[derive(Copy, Clone, Debug, PartialEq, Eq)]
pub struct Leaf {
    key_map: KeyMapId,
}

impl Leaf {
    pub fn new(key_map: KeyMapId) -> Self {
        Self { key_map }
    }

    pub fn key_map(&self) -> KeyMapId {
        self.key_map
    }
}

new_key_type! {
    pub struct NodeId;
}

#[derive(Copy, Clone, Debug, PartialEq, Eq, Hash,PartialOrd, Ord)]
pub struct KeyMapId(pub u64);

#[cfg(test)]
mod tests {
    use crate::{
        test_utils::edge_any,
        tree::{Node, NodeId, Tree},
    };

    #[test]
    fn use_existing_edge_() {
        let tree = Tree::build(|root| {});
        todo!();
    }
}
