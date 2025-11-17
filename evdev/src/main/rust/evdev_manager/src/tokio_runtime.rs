use std::sync::OnceLock;
use tokio::runtime::Runtime;

/// Global Tokio runtime
/// We need to keep this alive so spawned tasks continue running
static RUNTIME: OnceLock<Runtime> = OnceLock::new();

/// Initialize the Tokio runtime if not already initialized
/// Returns the runtime handle
pub fn init_runtime() -> Result<tokio::runtime::Handle, String> {
    let runtime =
        RUNTIME.get_or_init(|| Runtime::new().expect("Failed to initialize tokio runtime"));

    Ok(runtime.handle().clone())
}

/// Get the Tokio runtime handle
/// Returns None if runtime is not initialized
pub fn get_runtime_handle() -> Option<tokio::runtime::Handle> {
    RUNTIME.get().map(|rt| rt.handle().clone())
}