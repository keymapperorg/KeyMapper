use crate::android::keylayout::key_layout_map::{KeyLayoutKey, KeyLayoutMap};
use crate::device_identifier::DeviceIdentifier;
use log::{debug, error, info};
use std::collections::HashMap;
use std::env;
use std::error::Error;
use std::fs;
use std::fs::File;
use std::io::ErrorKind;
use std::path::PathBuf;
use std::sync::{Arc, Mutex, OnceLock};

static KEY_LAYOUT_MANAGER: OnceLock<Arc<KeyLayoutMapManager>> = OnceLock::new();

/// Manages KeyLayoutMap caching and key code mapping
/// This is the only file that directly interacts with KeyLayoutMap C bindings
/// and the only file that finds key layout file paths
pub struct KeyLayoutMapManager {
    /// KeyLayoutMap cache
    /// Maps device path to KeyLayoutMap handle. If the value is None then
    /// the key layout map could not be found or there was an error parsing,
    /// and it shouldn't be attempted again.
    pub key_layout_maps: Mutex<HashMap<DeviceIdentifier, Option<Arc<KeyLayoutMap>>>>,
    /// File finder for locating key layout files
    file_finder: Arc<dyn KeyLayoutFileFinder>,
}

impl KeyLayoutMapManager {
    pub fn get() -> Arc<Self> {
        Arc::clone(KEY_LAYOUT_MANAGER.get_or_init(|| Arc::new(Self::new())))
    }

    fn new() -> Self {
        Self {
            key_layout_maps: Mutex::new(HashMap::with_capacity(32)),
            file_finder: Arc::new(AndroidKeyLayoutFileFinder),
        }
    }

    /// Create a new instance with a custom file finder.
    /// This is primarily useful for testing.
    pub fn with_file_finder(file_finder: Arc<dyn KeyLayoutFileFinder>) -> Self {
        Self {
            key_layout_maps: Mutex::new(HashMap::with_capacity(32)),
            file_finder,
        }
    }

    /// Map a raw evdev key code to Android key code
    /// Returns the android keycode and flags if the key is found in the map, otherwise, `None`.
    pub fn map_key(
        &self,
        device_identifier: &DeviceIdentifier,
        scan_code: u32,
    ) -> Result<Option<Arc<KeyLayoutKey>>, Box<dyn Error>> {
        self.get_key_layout_map_lazy(device_identifier)
            .map(|map| map?.map_key(scan_code))
    }

    pub fn preload_key_layout_map(
        &self,
        device_identifier: &DeviceIdentifier,
    ) -> Result<Option<Arc<KeyLayoutMap>>, Box<dyn Error>> {
        self.get_key_layout_map_lazy(device_identifier)
            .inspect_err(|err| {
                error!(
                    "Error preloading key layout map for device {}: {}",
                    device_identifier.name, err
                )
            })
    }

    /// Get or load a key layout map for the given device identifier.
    /// This method is public for testing purposes.
    fn get_key_layout_map_lazy(
        &self,
        device_identifier: &DeviceIdentifier,
    ) -> Result<Option<Arc<KeyLayoutMap>>, Box<dyn Error>> {
        let mut key_layout_maps = self.key_layout_maps.lock().unwrap();

        if let Some(key_layout_map) = key_layout_maps.get(device_identifier) {
            return Ok(key_layout_map.clone());
        }

        let key_layout_map_paths = self.find_key_layout_files(device_identifier);
        info!(
            "Found key layout map files for device {}: {:?}",
            device_identifier.name, key_layout_map_paths
        );

        for path in key_layout_map_paths {
            return match KeyLayoutMap::load_from_file(path) {
                Ok(key_layout_map) => {
                    let option = Some(Arc::new(key_layout_map));

                    key_layout_maps.insert(device_identifier.clone(), option.clone());

                    Ok(option)
                }

                Err(e) => {
                    error!("Error parsing key layout map: {}", e);
                    Err(Box::from(e))
                }
            };
        }

        // No key layout map files were found or parsed successfully if this point is reached.
        key_layout_maps.insert(device_identifier.clone(), None);
        Ok(None)
    }

    /// Find all the possible key layout files to use for a device ordered by their priority.
    /// A list is returned so there are fallback key layout files if one can't be parsed
    /// for whatever reason.
    /// Tries multiple naming schemes based on vendor/product/version, then device name, then Generic.
    /// It first tries searching the system for the file, and then does the search again
    /// in the files shipped with Key Mapper.
    ///
    /// See https://source.android.com/docs/core/interaction/input/key-layout-files#location
    pub fn find_key_layout_files(&self, device_identifier: &DeviceIdentifier) -> Vec<PathBuf> {
        let name = device_identifier.name.as_str();
        let vendor = device_identifier.vendor;
        let product = device_identifier.product;
        let version = device_identifier.version;

        let mut paths: Vec<PathBuf> = Vec::new();

        // Try vendor/product/version path first
        if vendor != 0 && product != 0 {
            if version != 0 {
                let version_name = format!(
                    "Vendor_{:04x}_Product_{:04x}_Version_{:04x}",
                    vendor, product, version
                );
                if let Some(path) = self
                    .file_finder
                    .find_system_key_layout_file_by_name(&version_name)
                {
                    paths.push(path);
                }
            }

            // Try vendor/product
            let product_name = format!("Vendor_{:04x}_Product_{:04x}", vendor, product);
            if let Some(path) = self
                .file_finder
                .find_system_key_layout_file_by_name(&product_name)
            {
                paths.push(path);
            }
        }

        // Try device name (canonical)
        let canonical_name = get_canonical_name(name);
        if let Some(path) = self
            .file_finder
            .find_system_key_layout_file_by_name(&canonical_name)
        {
            paths.push(path);
        }

        // Try system generic
        if let Some(path) = self
            .file_finder
            .find_system_key_layout_file_by_name("Generic")
        {
            paths.push(path);
        }

        // TODO use Key Mapper key layout files

        paths
    }
}

/// Trait for finding key layout files.
/// This allows dependency injection for testing purposes.
pub trait KeyLayoutFileFinder: Send + Sync {
    /// Find a key layout file in the system by its name.
    fn find_system_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf>;

    /// Find a key layout file shipped with Key Mapper by its name.
    fn find_key_mapper_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf>;
}

/// Default implementation that uses the real file system.
/// This searches the standard Android key layout file locations.
pub struct AndroidKeyLayoutFileFinder;

impl KeyLayoutFileFinder for AndroidKeyLayoutFileFinder {
    fn find_system_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf> {
        // See https://source.android.com/docs/core/interaction/input/key-layout-files#location
        let path_prefixes = vec![
            "/odm/usr/".to_string(),
            "/vendor/usr/".to_string(),
            "/system/usr/".to_string(),
            "/data/system/devices/".to_string(),
        ];

        for prefix in &path_prefixes {
            let path = PathBuf::new()
                .join(prefix)
                .join("keylayout")
                .join(format!("{}.kl", name));

            match fs::metadata(&path) {
                Ok(metadata) if metadata.is_file() => {
                    if let Ok(_) = File::open(&path) {
                        return Some(path);
                    }
                }
                Err(e) if e.kind() != ErrorKind::NotFound => {
                    debug!("Error accessing {:?}: {}", path, e);
                }
                _ => {}
            }
        }

        None
    }

    fn find_key_mapper_key_layout_file_by_name(&self, _name: &str) -> Option<PathBuf> {
        None
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
