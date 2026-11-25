use crate::tree::KeyMapId;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum Signal {
    DispatchKeyMap { key_map: KeyMapId },
    CancelKeyMap { key_map: KeyMapId },
}
