use crate::{KeyLayoutKey, KeyLayoutMap};
use std::collections::HashMap;
use std::env;
use std::fs;
use std::fs::File;
use std::io::ErrorKind;
use std::sync::{Arc, Mutex};

/// Manages KeyLayoutMap caching and key code mapping
/// This is the only file that directly interacts with KeyLayoutMap C bindings
/// and the only file that finds key layout file paths
pub struct KeyLayoutMapManager {
    /// KeyLayoutMap cache
    /// Maps device path to KeyLayoutMap handle
    key_layout_maps: Arc<Mutex<HashMap<String, KeyLayoutMap>>>,
}

impl KeyLayoutMapManager {
    pub fn new() -> Self {
        Self {
            key_layout_maps: Arc::new(Mutex::new(HashMap::new())),
        }
    }

    /// Find key layout file path by name
    /// Searches system repository and user repository
    /// Returns None if not found
    ///
    /// This code is translated from AOSP frameworks/native/libs/input/InputDevice.cpp
    fn find_key_layout_file_by_name(&self, name: &str) -> Option<String> {
        // Search system repository
        let mut path_prefixes = vec![
            "/product/usr/".to_string(),
            "/system_ext/usr/".to_string(),
            "/odm/usr/".to_string(),
            "/vendor/usr/".to_string(),
            "/system/usr/".to_string(),
        ];

        // ANDROID_ROOT may not be set on host
        if let Ok(android_root) = env::var("ANDROID_ROOT") {
            path_prefixes.push(format!("{}/usr/", android_root));
        }

        // Try each system path prefix
        for prefix in &path_prefixes {
            let path = format!("{}keylayout/{}.kl", prefix, name);

            match fs::metadata(&path) {
                Ok(metadata) if metadata.is_file() => {
                    if let Ok(_) = File::open(&path) {
                        return Some(path);
                    }
                }
                Err(e) if e.kind() != ErrorKind::NotFound => {
                    debug!("Error accessing {}: {}", path, e);
                }
                _ => {}
            }
        }

        // Search user repository
        if let Ok(android_data) = env::var("ANDROID_DATA") {
            let path = format!("{}/system/devices/keylayout/{}.kl", android_data, name);

            match fs::metadata(&path) {
                Ok(metadata) if metadata.is_file() => {
                    if let Ok(_) = File::open(&path) {
                        return Some(path);
                    }
                }
                Err(e) if e.kind() != ErrorKind::NotFound => {
                    warn!("Error accessing user config file {}: {}", path, e);
                }
                _ => {}
            }
        }

        None
    }

    /// Find key layout file path by device identifier
    /// Tries multiple naming schemes based on vendor/product/version, then device name, then Generic
    /// Returns None if not found
    ///
    /// This code is translated from AOSP frameworks/native/libs/input/InputDevice.cpp
    fn find_key_layout_file_by_device_identifier(
        &self,
        name: &str,
        vendor: u16,
        product: u16,
        version: u16,
    ) -> Option<String> {
        // Try vendor/product/version path first
        if vendor != 0 && product != 0 {
            if version != 0 {
                let version_name = format!(
                    "Vendor_{:04x}_Product_{:04x}_Version_{:04x}",
                    vendor, product, version
                );
                if let Some(path) = self.find_key_layout_file_by_name(&version_name) {
                    info!("Found key layout map by version path {}", path);
                    return Some(path);
                }
            }

            // Try vendor/product
            let product_name = format!("Vendor_{:04x}_Product_{:04x}", vendor, product);
            if let Some(path) = self.find_key_layout_file_by_name(&product_name) {
                info!("Found key layout map by product path {}", path);
                return Some(path);
            }
        }

        // Try device name (canonical)
        let canonical_name = get_canonical_name(name);
        if let Some(path) = self.find_key_layout_file_by_name(&canonical_name) {
            info!("Found key layout map by name path {}", path);
            return Some(path);
        }

        // As a last resort, try Generic
        self.find_key_layout_file_by_name("Generic")
    }

    /// Register a device and load its KeyLayoutMap
    /// This should be called when a device is grabbed
    pub fn register_device(
        &self,
        device_path: &str,
        name: &str,
        bus: u16,
        vendor: u16,
        product: u16,
        version: u16,
    ) {
        let layout_file_path = self
            .find_key_layout_file_by_device_identifier(&name, vendor, product, version)
            .expect("Failed to find key layout map");

        let key_layout_map: KeyLayoutMap = KeyLayoutMap::load_from_file(&layout_file_path).unwrap();

        let mut key_layout_maps = self.key_layout_maps.lock().unwrap();
        key_layout_maps.insert(String::from(device_path), key_layout_map);
    }

    /// Unregister a device and cleanup its KeyLayoutMap
    /// This should be called when a device is ungrabbed
    pub fn unregister_device(&self, device_path: &str) {
        let mut key_layout_maps = self.key_layout_maps.lock().unwrap();
        key_layout_maps.remove(device_path);
    }

    pub fn unregister_all(&self) {
        self.key_layout_maps.lock().unwrap().clear();
    }

    /// Map a raw evdev key code to Android key code
    /// Returns (android_keycode, flags) or (0, 0) if mapping not found
    pub fn map_key(&self, device_path: &str, scan_code: u32) -> Option<KeyLayoutKey> {
        let key_layout_maps = self.key_layout_maps.lock().unwrap();

        key_layout_maps.get(device_path)?.map_key(scan_code)
    }
}

impl Default for KeyLayoutMapManager {
    fn default() -> Self {
        Self::new()
    }
}

/// Get canonical name with all invalid characters replaced by underscores
fn get_canonical_name(name: &str) -> String {
    name.chars()
        .map(|ch| {
            if ch.is_ascii_alphanumeric() || ch == '-' || ch == '_' {
                ch
            } else {
                '_'
            }
        })
        .collect()
}
