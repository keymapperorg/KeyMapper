//! Tests for KeyLayoutMapManager file finding logic.
use assertables::assert_iter_eq;
use evdev_manager_core::android::keylayout::key_layout_map_manager::{
    KeyLayoutFileFinder, KeyLayoutMapManager,
};
use evdev_manager_core::device_identifier::DeviceIdentifier;
#[cfg(test)]
use pretty_assertions::assert_eq;
use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;

/// Mock file finder that returns predefined paths for testing.
struct MockFileFinder {
    system_files: HashMap<String, PathBuf>,
    key_mapper_files: HashMap<String, PathBuf>,
}

impl MockFileFinder {
    fn new() -> Self {
        Self {
            system_files: HashMap::new(),
            key_mapper_files: HashMap::new(),
        }
    }

    fn add_system_file(mut self, name: &str, path: PathBuf) -> Self {
        self.system_files.insert(name.to_string(), path);
        self
    }
}

impl KeyLayoutFileFinder for MockFileFinder {
    fn find_system_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf> {
        self.system_files.get(name).cloned()
    }

    fn find_key_mapper_key_layout_file_by_name(&self, name: &str) -> Option<PathBuf> {
        self.key_mapper_files.get(name).cloned()
    }
}

#[test]
fn test_find_key_layout_files_priority_order() {
    // Create a mock file finder that has files at different priority levels
    let mock_finder = Arc::new(
        MockFileFinder::new()
            .add_system_file(
                "Vendor_1234_Product_5678_Version_0001",
                PathBuf::from("/system/usr/keylayout/Vendor_1234_Product_5678_Version_0001.kl"),
            )
            .add_system_file(
                "Vendor_1234_Product_5678",
                PathBuf::from("/system/usr/keylayout/Vendor_1234_Product_5678.kl"),
            )
            .add_system_file(
                "gpio-keys",
                PathBuf::from("/system/usr/keylayout/gpio-keys.kl"),
            )
            .add_system_file("Generic", PathBuf::from("/system/usr/keylayout/Generic.kl")),
    );

    // Create a device identifier with vendor, product, and version
    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x1234,
        product: 0x5678,
        version: 0x0001,
    };

    // Get the list of key layout files in priority order
    let files: Vec<String> = find_key_layout_files_str(mock_finder, &device);

    let expected = [
        "/system/usr/keylayout/Vendor_1234_Product_5678_Version_0001.kl",
        "/system/usr/keylayout/Vendor_1234_Product_5678.kl",
        "/system/usr/keylayout/gpio-keys.kl",
        "/system/usr/keylayout/Generic.kl",
    ];

    assert_iter_eq!(files, expected);
}

fn find_key_layout_files_str(
    mock_file_finder: Arc<MockFileFinder>,
    device: &DeviceIdentifier,
) -> Vec<String> {
    let manager = KeyLayoutMapManager::with_file_finder(mock_file_finder);

    manager
        .find_key_layout_files(&device)
        .iter()
        .map(|path| path.to_str().unwrap().to_string())
        .collect()
}
