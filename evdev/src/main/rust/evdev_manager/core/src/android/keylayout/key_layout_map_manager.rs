use crate::android::keylayout::generic_key_layout::GENERIC_KEY_LAYOUT_CONTENTS;
use crate::android::keylayout::key_layout_file_finder::{
    AndroidKeyLayoutFileFinder, KeyLayoutFileFinder,
};
use crate::android::keylayout::key_layout_map::KeyLayoutMap;
use crate::evdev_device_info::EvdevDeviceInfo;
use evdev::enums::{EventCode, EventType};
use evdev::util::int_to_event_code;
use libc::c_uint;
use log::{error, info};
use std::collections::HashMap;
use std::error::Error;
use std::path::PathBuf;
use std::sync::{Arc, Mutex, OnceLock};

static KEY_LAYOUT_MANAGER: OnceLock<Arc<KeyLayoutMapManager>> = OnceLock::new();
static GENERIC_KEY_LAYOUT_MAP: OnceLock<Arc<KeyLayoutMap>> = OnceLock::new();

/// Get the static generic KeyLayoutMap instance.
/// This is lazily initialized from hardcoded key mappings based on AOSP Generic.kl.
pub fn get_generic_key_layout_map() -> Arc<KeyLayoutMap> {
    Arc::clone(GENERIC_KEY_LAYOUT_MAP.get_or_init(|| {
        Arc::new(
            KeyLayoutMap::load_from_contents(GENERIC_KEY_LAYOUT_CONTENTS)
                .expect("Failed to parse hardcoded Generic key layout"),
        )
    }))
}

/// Manages KeyLayoutMap caching and key code mapping
/// This is the only file that directly interacts with KeyLayoutMap C bindings
/// and the only file that finds key layout file paths
pub struct KeyLayoutMapManager {
    /// KeyLayoutMap cache
    /// Maps device path to KeyLayoutMap handle. If the value is None then
    /// the key layout map could not be found or there was an error parsing,
    /// and it shouldn't be attempted again.
    pub key_layout_maps: Mutex<HashMap<EvdevDeviceInfo, Option<Arc<KeyLayoutMap>>>>,
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

    /// Map a raw evdev key code to Android key code.
    /// Returns the android keycode if the key is found in the device's map,
    /// falling back to the generic key layout if not found.
    pub fn map_key(
        &self,
        device_info: &EvdevDeviceInfo,
        scan_code: u32,
    ) -> Result<Option<u32>, Box<dyn Error>> {
        let device_map = self.get_key_layout_map_lazy(device_info)?;

        if let Some(map) = device_map {
            if let Some(key_code) = map.map_key(scan_code) {
                return Ok(Some(key_code));
            }
        }

        // Fall back to generic key layout
        Ok(get_generic_key_layout_map().map_key(scan_code))
    }

    /// Find the scan code for a given Android key code.
    /// Returns the scan code if found in the device's map,
    /// falling back to the generic key layout if not found.
    pub fn find_scan_code_for_key(
        &self,
        device_info: &EvdevDeviceInfo,
        key_code: u32,
    ) -> Result<Option<u32>, Box<dyn Error>> {
        let device_map = self.get_key_layout_map_lazy(device_info)?;

        if let Some(map) = device_map {
            if let Some(scan_code) = map.find_scan_code_for_key(key_code) {
                return Ok(Some(scan_code));
            }
        }

        // Fall back to generic key layout
        Ok(get_generic_key_layout_map().find_scan_code_for_key(key_code))
    }

    pub fn preload_key_layout_map(
        &self,
        device_info: &EvdevDeviceInfo,
    ) -> Result<Option<Arc<KeyLayoutMap>>, Box<dyn Error>> {
        self.get_key_layout_map_lazy(device_info)
            .inspect_err(|err| {
                error!(
                    "Error preloading key layout map for device {}: {}",
                    device_info.name, err
                )
            })
    }

    /// Get or load a key layout map for the given device identifier.
    /// This method is public for testing purposes.
    fn get_key_layout_map_lazy(
        &self,
        device_info: &EvdevDeviceInfo,
    ) -> Result<Option<Arc<KeyLayoutMap>>, Box<dyn Error>> {
        let mut key_layout_maps = self.key_layout_maps.lock().unwrap();

        if let Some(key_layout_map) = key_layout_maps.get(device_info) {
            return Ok(key_layout_map.clone());
        }

        let key_layout_map_paths = self.find_key_layout_files(device_info);
        info!(
            "Found key layout map files for device {}: {:?}",
            device_info.name, key_layout_map_paths
        );

        for path in key_layout_map_paths {
            match KeyLayoutMap::load_from_file(path) {
                Ok(key_layout_map) => {
                    let option = Some(Arc::new(key_layout_map));
                    key_layout_maps.insert(device_info.clone(), option.clone());
                    return Ok(option);
                }
                Err(e) => {
                    error!("Error parsing key layout map: {}", e);
                    // Continue to try the next file instead of failing immediately
                }
            }
        }

        // No key layout map files were found or parsed successfully.
        // Fall back to the hardcoded generic key layout map.
        info!(
            "No key layout files found for device {}, using hardcoded Generic fallback",
            device_info.name
        );
        let fallback = Some(get_generic_key_layout_map());
        key_layout_maps.insert(device_info.clone(), fallback.clone());
        Ok(fallback)
    }

    /// Find all the possible key layout files to use for a device ordered by their priority.
    /// A list is returned so there are fallback key layout files if one can't be parsed
    /// for whatever reason.
    /// Tries multiple naming schemes based on vendor/product/version, then device name, then Generic.
    /// It first tries searching the system for the file, and then does the search again
    /// in the files shipped with Key Mapper.
    ///
    /// See https://source.android.com/docs/core/interaction/input/key-layout-files#location
    pub fn find_key_layout_files(&self, device_info: &EvdevDeviceInfo) -> Vec<PathBuf> {
        let name = device_info.name.as_str();
        let vendor = device_info.vendor;
        let product = device_info.product;
        let version = device_info.version;

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

        paths
    }

    pub fn map_key_codes_to_event_codes(key_codes: &[u32]) -> Vec<EventCode> {
        let generic_key_layout = get_generic_key_layout_map();

        key_codes
            .iter()
            .filter_map(|key_code| generic_key_layout.find_scan_code_for_key(*key_code))
            .map(|scan_code| int_to_event_code(EventType::EV_KEY as c_uint, scan_code))
            .collect()
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
