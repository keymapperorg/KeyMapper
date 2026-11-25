use std::time::Duration;

use tokio::time::{self, Instant};

use crate::{
    input::{Input, InputRef},
    tree::NodeId,
};

#[derive(Clone, Debug)]
pub struct Cursor {
    node: NodeId,
    /// The down time of the first input in this cursor.
    first_down_time: Instant,
    inputs: Vec<Input>,
}

impl Cursor {
    pub fn new(node: NodeId) -> Self {
        Self {
            node,
            first_down_time: Instant::now(),
            inputs: Vec::new(),
        }
    }

    /// Advance this cursor to a new node.
    ///
    pub fn advance(&mut self, next_node: NodeId, input: Input) {
        self.node = next_node;
        self.inputs.push(input);
    }

    /// Create a new cursor that is advanced from this one.
    /// This should be called when the
    pub fn branch(&self, next_node: NodeId, input: Input) -> Self {
        let mut clone = self.clone();

        clone.advance(next_node, input);

        clone
    }

    pub fn first_down_time(&self) -> Instant {
        self.first_down_time
    }

    pub fn node(&self) -> NodeId {
        self.node
    }

    pub fn is_active(&self, timeout: Duration) -> bool {
        !self.is_expired(timeout)
    }

    pub fn is_expired(&self, timeout: Duration) -> bool {
        Instant::now() >= self.first_down_time + timeout
    }
}
