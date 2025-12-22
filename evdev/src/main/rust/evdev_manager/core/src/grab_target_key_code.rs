#[derive(Debug)]
pub struct GrabTargetKeyCode {
    pub name: String,
    pub bus: u16,
    pub vendor: u16,
    pub product: u16,
    pub extra_key_codes: Vec<u32>,
}
