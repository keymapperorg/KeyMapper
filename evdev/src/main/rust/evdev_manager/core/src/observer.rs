use evdev::InputEvent;
use std::sync::{Arc, Mutex};

/// Trait for observers that receive evdev events
/// Returns true if the event was consumed, false otherwise
pub trait EvdevEventObserver: Send + Sync {
    fn on_event(&self, device_path: &str, event: &InputEvent) -> bool;
}

/// Manages multiple observers and notifies them of events
pub struct EvdevEventNotifier {
    observers: Arc<Mutex<Vec<Box<dyn EvdevEventObserver>>>>,
}

impl EvdevEventNotifier {
    pub fn new() -> Self {
        Self {
            observers: Arc::new(Mutex::new(Vec::new())),
        }
    }

    /// Register a new observer
    pub fn register(&self, observer: Box<dyn EvdevEventObserver>) {
        let mut observers = self.observers.lock().unwrap();
        observers.push(observer);
    }

    /// Unregister all observers
    pub fn clear(&self) {
        let mut observers = self.observers.lock().unwrap();
        observers.clear();
    }

    /// Notify all observers of an event
    /// Returns true if any observer consumed the event, false otherwise
    pub fn notify(&self, device_path: &str, event: &InputEvent) -> bool {
        let observers = self.observers.lock().unwrap();
        let mut consumed = false;
        
        // Always call all observers
        for observer in observers.iter() {
            if observer.on_event(device_path, event) {
                consumed = true;
            }
        }
        
        consumed
    }

    /// Get a clone of the observers Arc for sharing
    pub fn observers(&self) -> Arc<Mutex<Vec<Box<dyn EvdevEventObserver>>>> {
        Arc::clone(&self.observers)
    }
}

impl Default for EvdevEventNotifier {
    fn default() -> Self {
        Self::new()
    }
}

