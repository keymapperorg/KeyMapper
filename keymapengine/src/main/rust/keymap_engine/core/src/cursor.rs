use std::time::Instant;

use crate::{input::Input, tree::NodeId};

pub struct Cursor {
    node: NodeId,
    start_time: Instant,
    inputs: Vec<Input>,
}
