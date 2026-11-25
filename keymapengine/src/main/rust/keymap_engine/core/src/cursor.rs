use std::time::Duration;

use tokio::time::{self, Instant};

use crate::{
    input::{Input, InputRef},
    tree::NodeId,
};

#[derive(Clone, Debug)]
pub struct Cursor {
    node: NodeId,
    inputs: Vec<Input>,
    is_canceled: bool,
}

impl Cursor {
    pub fn new(node: NodeId) -> Self {
        Self {
            node,
            inputs: Vec::new(),
            is_canceled: false,
        }
    }

    /// Advance this cursor to a new node.
    pub fn advance(&mut self, next_node: NodeId, input: Input) {
        self.node = next_node;
        self.inputs.push(input);
    }

    /// Create a new cursor that is advanced from this one.
    /// This should be called when there are multiple nodes
    /// a cursor can advance to at once.
    pub fn branch(&self, next_node: NodeId, input: Input) -> Self {
        let mut clone = self.clone();

        clone.advance(next_node, input);

        clone
    }

    pub fn first_down_time(&self) -> Option<Instant> {
        self.inputs.first().map(Input::time)
    }

    pub fn node(&self) -> NodeId {
        self.node
    }

    pub fn cancel(&mut self) {
        self.is_canceled = true;
    }

    pub fn is_canceled(&self) -> bool {
        self.is_canceled
    }

    pub fn is_active(&self, timeout: Duration) -> bool {
        !self.is_canceled && !self.is_expired(timeout)
    }

    fn is_expired(&self, timeout: Duration) -> bool {
        if timeout == Duration::MAX {
            return false;
        }

        match self.first_down_time() {
            Some(down_time) => Instant::now() >= down_time + timeout,
            None => false,
        }
    }
}
