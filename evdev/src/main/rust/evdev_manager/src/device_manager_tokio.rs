use crate::device_manager::DeviceContext;
use crate::device_task;
use crate::observer::EvdevEventNotifier;
use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex};
use tokio::task::JoinHandle;

/// Manages device tasks using Tokio
pub struct DeviceTaskManager {
    /// Map of device paths to their Tokio task handles
    tasks: Arc<Mutex<HashMap<String, JoinHandle<()>>>>,
    /// Map of device paths to their device contexts (for uinput access and event writing)
    devices: Arc<Mutex<HashMap<String, Arc<DeviceContext>>>>,
    /// Event notifier for observers
    notifier: Arc<EvdevEventNotifier>,
}

impl DeviceTaskManager {
    pub fn new(notifier: Arc<EvdevEventNotifier>) -> Self {
        Self {
            tasks: Arc::new(Mutex::new(HashMap::new())),
            devices: Arc::new(Mutex::new(HashMap::new())),
            notifier,
        }
    }

    /// Add a device and spawn its task
    pub fn add_device(
        &self,
        device_path: String,
        device: Arc<DeviceContext>,
    ) -> Result<(), String> {
        let mut tasks = self.tasks.lock().unwrap();
        let mut devices = self.devices.lock().unwrap();

        if tasks.contains_key(&device_path) {
            return Err(format!("Device {} is already being handled", device_path));
        }

        // Store device context
        devices.insert(device_path.clone(), Arc::clone(&device));

        // Spawn the device task
        let handle = device_task::spawn_device_task(
            device_path.clone(),
            device,
            Arc::clone(&self.notifier),
        )
        .map_err(|e| format!("Failed to spawn device task: {}", e))?;

        tasks.insert(device_path, handle);

        Ok(())
    }

    /// Remove a device and cancel its task
    pub fn remove_device(&self, device_path: &str) -> Result<(), String> {
        let mut tasks = self.tasks.lock().unwrap();
        let mut devices = self.devices.lock().unwrap();

        if let Some(handle) = tasks.remove(device_path) {
            // Abort the task
            handle.abort();
            // Remove device context
            devices.remove(device_path);
            Ok(())
        } else {
            Err(format!("Device {} not found", device_path))
        }
    }

    /// Remove all devices and cancel their tasks
    pub fn remove_all_devices(&self) {
        let mut tasks = self.tasks.lock().unwrap();
        let mut devices = self.devices.lock().unwrap();

        for (_, handle) in tasks.drain() {
            handle.abort();
        }
        devices.clear();
    }

    /// Check if a device is being handled
    pub fn is_device_grabbed(&self, device_path: &str) -> bool {
        let tasks = self.tasks.lock().unwrap();
        tasks.contains_key(device_path)
    }

    /// Get all device paths
    pub fn get_device_paths(&self) -> Vec<String> {
        let tasks = self.tasks.lock().unwrap();
        tasks.keys().cloned().collect()
    }

    /// Get uinput device paths from all devices
    pub fn get_uinput_device_paths(&self) -> HashSet<String> {
        let devices = self.devices.lock().unwrap();
        devices
            .values()
            .filter_map(|d| d.uinput.devnode())
            .collect()
    }

    /// Write an event to a device (if it exists)
    pub fn write_event_to_device(
        &self,
        device_path: &str,
        event_type: u32,
        code: u32,
        value: i32,
    ) -> Result<(), String> {
        let devices = self.devices.lock().unwrap();
        if let Some(device) = devices.get(device_path) {
            device
                .write_event(event_type, code, value)
                .map_err(|e| format!("Failed to write event: {}", e))
        } else {
            Err(format!("Device {} not found", device_path))
        }
    }
}

