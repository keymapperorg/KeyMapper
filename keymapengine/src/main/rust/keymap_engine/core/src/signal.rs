use crate::tree::KeyMapId;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Signal {
    ActionDown{
        key_map: KeyMapId
    },
    ActionUp,
}
