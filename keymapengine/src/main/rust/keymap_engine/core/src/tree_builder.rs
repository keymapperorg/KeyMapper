use crate::{tree::Tree, trigger::Trigger};

pub struct TreeBuilder {}

impl TreeBuilder {
    pub fn from_key_maps(triggers: &[Trigger]) -> Tree {
        // One key map is allowed to span multiple triggers because
        // any-order combo triggers are allowed. The scheduler must not
        // perform a key map while it is already running.

        // TODO merge key maps with the same trigger into the same branch, and add multiple leaves.

        Tree::new()
    }
}

#[cfg(test)]
mod tests {
    use crate::{tree::Tree, tree_builder::TreeBuilder};

    #[test]
    fn build_empty_tree() {
        // assert_eq!(TreeBuilder::from_key_maps(&[]), Tree::new());
    }
}
