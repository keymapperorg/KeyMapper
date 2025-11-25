use crate::android::keylayout::key_layout_map::{KeyLayoutKey, KeyLayoutMap};
use crate::device_identifier::DeviceIdentifier;
use std::collections::HashMap;
use std::error::Error;
use std::fs;
use std::fs::File;
use std::io::ErrorKind;
use std::sync::{Arc, Mutex, OnceLock};
use std::{env, io};

static KEY_LAYOUT_MANAGER: OnceLock<KeyLayoutMapManager> = OnceLock::new();

/// Manages KeyLayoutMap caching and key code mapping
/// This is the only file that directly interacts with KeyLayoutMap C bindings
/// and the only file that finds key layout file paths
pub struct KeyLayoutMapManager {
    /// KeyLayoutMap cache
    /// Maps device path to KeyLayoutMap handle
    key_layout_maps: Mutex<HashMap<DeviceIdentifier, Arc<KeyLayoutMap>>>,
}

impl KeyLayoutMapManager {
    pub fn get() -> &'static Self {
        KEY_LAYOUT_MANAGER.get_or_init(Self::new)
    }

    fn new() -> Self {
        Self {
            key_layout_maps: Mutex::new(HashMap::with_capacity(32)),
        }
    }

    /// Map a raw evdev key code to Android key code
    /// Returns the android keycode and flags if the key is found in the map, otherwise, `None`.
    pub fn map_key(
        &self,
        device_identifier: &DeviceIdentifier,
        scan_code: u32,
    ) -> Result<Option<KeyLayoutKey>, Box<dyn Error>> {
        self.get_key_layout_map_lazy(device_identifier)
            .map(|map| map.map_key(scan_code))
    }

    pub fn preload_key_layout_map(
        &self,
        device_identifier: &DeviceIdentifier,
    ) -> Result<(), Box<dyn Error>> {
        self.get_key_layout_map_lazy(device_identifier).map(|_| ())
    }

    fn get_key_layout_map_lazy(
        &self,
        device_identifier: &DeviceIdentifier,
    ) -> Result<Arc<KeyLayoutMap>, Box<dyn Error>> {
        let mut key_layout_maps = self.key_layout_maps.lock().unwrap();

        if let Some(key_layout_map) = key_layout_maps.get(device_identifier) {
            return Ok(key_layout_map.clone());
        }

        let file_path = match self.find_key_layout_file_by_device_identifier(device_identifier) {
            None => {
                let error = io::Error::new(
                    ErrorKind::NotFound,
                    format!(
                        "Key layout map file not found for device {:?}",
                        device_identifier
                    ),
                );

                return Err(error.into());
            }
            Some(path) => path,
        };

        let key_layout_map = Arc::new(KeyLayoutMap::load_from_file(file_path.as_str())?);

        key_layout_maps.insert(device_identifier.clone(), key_layout_map.clone());

        Ok(key_layout_map)
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
        device_identifier: &DeviceIdentifier,
    ) -> Option<String> {
        let name = device_identifier.name.as_str();
        let vendor = device_identifier.vendor;
        let product = device_identifier.product;
        let version = device_identifier.version;

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
