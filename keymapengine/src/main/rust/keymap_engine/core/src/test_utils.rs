use crate::{
    input::InputRef,
    tree::{Edge, KeyMapId, Leaf, Tree},
};

pub fn input(name: char) -> InputRef {
    InputRef(name as u64)
}

/// Key map 0 = Trigger 'A'
pub fn single_key_tree() -> Tree {
    let mut tree = Tree::new();
    let node = tree.insert_node(
        tree.root(),
        Edge::with_any_device(input('A'), &[KeyMapId(0)]),
    );
    tree.insert_leaf(node, Leaf::new(KeyMapId(0)));
    tree
}
