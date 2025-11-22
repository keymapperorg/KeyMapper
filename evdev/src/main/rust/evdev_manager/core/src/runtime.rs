use std::sync::{Mutex, OnceLock, PoisonError, RwLock, RwLockWriteGuard};
use std::time::Duration;
use tokio::runtime::Runtime;

static RUNTIME: OnceLock<Runtime> = OnceLock::new();

pub fn get_runtime() -> &'static Runtime {
    RUNTIME.get_or_init(|| {
        tokio::runtime::Builder::new_multi_thread()
            .worker_threads(2) // Optional: limit threads to save resources on Android
            .enable_all()
            .build()
            .expect("Failed to create Tokio runtime")
    })
}
