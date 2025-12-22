use std::path::{Path, PathBuf};
use std::sync::{
    atomic::{AtomicBool, Ordering},
    mpsc, Arc, Mutex, RwLock,
};

use notify::{EventKind, RecommendedWatcher, Watcher};
use tokio::task::JoinHandle;

use crate::evdev_error::{EvdevError, EvdevErrorCode};
use crate::runtime::get_runtime;

/// Callback for when inotify events occur
pub trait InotifyCallback: Send + Sync {
    fn on_inotify_dev_input(&self, paths: &[PathBuf]);
}

pub struct EvdevDevicesWatcher {
    watcher: Arc<Mutex<Option<RecommendedWatcher>>>,
    inotify_handle: RwLock<Option<JoinHandle<()>>>,
    enabled: Arc<AtomicBool>,
}

impl EvdevDevicesWatcher {
    pub fn new() -> Self {
        Self {
            watcher: Arc::new(Mutex::new(None)),
            inotify_handle: RwLock::new(None),
            enabled: Arc::new(AtomicBool::new(true)),
        }
    }

    /// Start the thread to watch /dev/input for device changes
    pub fn start(&self, callback: Arc<dyn InotifyCallback>) -> Result<(), EvdevError> {
        let is_running = { self.inotify_handle.read().unwrap().is_some() };

        if is_running {
            info!("Inotify watcher is already running");
            return Ok(());
        }

        // Create the channel and watcher
        let (tx, rx) = mpsc::channel::<notify::Result<notify::Event>>();

        let mut watcher = notify::recommended_watcher(tx).map_err(|e| {
            error!("Failed to create inotify watcher: {}", e);
            EvdevError::from_enum(EvdevErrorCode::IoError)
        })?;

        watcher
            .watch(Path::new("/dev/input"), notify::RecursiveMode::Recursive)
            .map_err(|e| {
                error!("Failed to watch /dev/input: {}", e);
                EvdevError::from_enum(EvdevErrorCode::IoError)
            })?;

        // Store the watcher in Arc
        {
            let mut watcher_guard = self.watcher.lock().unwrap();
            *watcher_guard = Some(watcher);
        }

        // Start the event processing loop
        let callback_clone = callback.clone();
        let enabled_clone = self.enabled.clone();

        let handle = get_runtime().spawn(async move {
            for event_result in rx {
                // Skip processing if disabled
                if !enabled_clone.load(Ordering::Relaxed) {
                    continue;
                }

                match event_result {
                    Ok(event) => {
                        if event.kind == EventKind::Create(notify::event::CreateKind::File)
                            || event.kind == EventKind::Remove(notify::event::RemoveKind::File)
                        {
                            callback_clone.on_inotify_dev_input(&event.paths);
                        }
                    }
                    Err(err) => {
                        error!("Failed to receive inotify event: {}", err);
                    }
                }
            }
        });

        self.inotify_handle.write().unwrap().replace(handle);

        Ok(())
    }

    /// Stop the thread watching /dev/input for device changes
    pub fn stop(&self) -> Result<(), EvdevError> {
        self.enabled.store(false, Ordering::Relaxed);

        let handle_option = self.inotify_handle.write().unwrap().take();

        if let Some(handle) = handle_option {
            handle.abort();
        }

        // Clear the watcher
        let mut watcher_guard = self.watcher.lock().unwrap();
        *watcher_guard = None;

        Ok(())
    }

    /// Enable processing of inotify events
    pub fn enable(&self) {
        self.enabled.store(true, Ordering::Relaxed);
    }

    /// Disable processing of inotify events (temporarily skip events)
    pub fn disable(&self) {
        self.enabled.store(false, Ordering::Relaxed);
    }
}

impl Default for EvdevDevicesWatcher {
    fn default() -> Self {
        Self::new()
    }
}
