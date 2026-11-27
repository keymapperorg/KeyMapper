use std::{future::pending, time::Duration};

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
    /// The cursor needs to wait for this deadline
    /// before it can advance. This is used for long presses
    /// in a sequence trigger. The cursor can only advance
    /// if it has been long pressed.
    pending_deadline: Option<Instant>,
}

impl Cursor {
    pub fn new(node: NodeId) -> Self {
        Self {
            node,
            inputs: Vec::new(),
            is_canceled: false,
            pending_deadline: None,
        }
    }

    fn set_pending(&mut self, timeout: Duration) {
        if timeout.is_zero() {
            return;
        }

        self.pending_deadline = Some(Instant::now() + timeout)
    }

    pub fn is_pending(&self) -> bool {
        self.pending_deadline
            .is_some_and(|deadline| deadline > Instant::now())
    }

    pub fn pending_time_remaining(&self) -> Option<Duration> {
        self.pending_deadline
            .map(|deadline| deadline - Instant::now())
    }

    /// Create a new cursor that is advanced from this one.
    /// The caller can decide whether to keep or discard the old
    /// cursor.
    pub fn advance(&self, next_node: NodeId, input: Input, pending_timeout: Duration) -> Self {
        let mut clone = self.clone();

        clone.node = next_node;
        clone.inputs.push(input);
        clone.set_pending(pending_timeout);

        clone
    }

    pub fn first_down_time(&self) -> Option<Instant> {
        self.inputs.first().map(Input::time)
    }

    pub fn time_since_first_down(&self) -> Duration {
        match self.first_down_time() {
            Some(down_time) => Instant::now() - down_time,
            None => Duration::ZERO,
        }
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
