use crate::tree::KeyMapId;

pub trait Performer: Send + Sync {
    fn on_activate_key_map(&self, key_map: KeyMapId);
    fn on_deactivate_key_map(&self, key_map: KeyMapId);
}
