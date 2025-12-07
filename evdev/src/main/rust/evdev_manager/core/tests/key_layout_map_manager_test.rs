//! Tests for KeyLayoutMapManager file finding logic.
use assertables::{assert_iter_eq, assert_none, assert_ok, assert_result_ok, assert_some};
use evdev_manager_core::android::android_codes::{
    AKEYCODE_HOME, AKEYCODE_MINUS, AKEYCODE_MOVE_HOME,
};
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

fn get_test_data_path() -> PathBuf {
    let test_data_dir = env!("CARGO_MANIFEST_DIR");
    PathBuf::from(test_data_dir).join("tests").join("test_data")
}

#[test]
fn test_find_no_files() {
    let mock_finder = Arc::new(MockFileFinder::new());

    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x1234,
        product: 0x5678,
        version: 0x0001,
    };

    let files: Vec<String> = find_key_layout_files_str(mock_finder, &device);

    assert_eq!(files.len(), 0)
}

#[test]
fn test_only_find_generic_file() {
    let mock_finder = Arc::new(
        MockFileFinder::new()
            .add_system_file("Generic", PathBuf::from("/system/usr/keylayout/Generic.kl")),
    );

    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x1234,
        product: 0x5678,
        version: 0x0001,
    };

    let files: Vec<String> = find_key_layout_files_str(mock_finder, &device);

    let expected = ["/system/usr/keylayout/Generic.kl"];

    assert_iter_eq!(files, expected);
}

#[test]
fn test_find_key_layout_files_priority_order() {
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

    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x1234,
        product: 0x5678,
        version: 0x0001,
    };

    let files: Vec<String> = find_key_layout_files_str(mock_finder, &device);

    let expected = [
        "/system/usr/keylayout/Vendor_1234_Product_5678_Version_0001.kl",
        "/system/usr/keylayout/Vendor_1234_Product_5678.kl",
        "/system/usr/keylayout/gpio-keys.kl",
        "/system/usr/keylayout/Generic.kl",
    ];

    assert_iter_eq!(files, expected);
}

#[test]
fn test_preload_key_layout_map_returns_cached_result() {
    // Create a mock file finder that returns a path to a valid key layout file
    let test_kl_path = get_test_data_path().join("Generic.kl");
    let mock_finder =
        Arc::new(MockFileFinder::new().add_system_file("Generic", test_kl_path.clone()));

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Test Device".to_string(),
        bus: 0x0003,
        vendor: 0x0000,
        product: 0x0000,
        version: 0x0000,
    };

    let result1 = manager.preload_key_layout_map(&device).unwrap();
    // Verify the cached value is returned by comparing the pointers
    let result2 = manager.preload_key_layout_map(&device).unwrap();

    assert!(
        Arc::ptr_eq(&result1.unwrap(), &result2.unwrap()),
        "Second call should return the same cached Arc instance"
    );
}

#[test]
fn test_preload_key_layout_map_saves_some_when_found() {
    // Create a mock file finder that returns no files
    let test_kl_path = get_test_data_path().join("Generic.kl");
    let mock_finder =
        Arc::new(MockFileFinder::new().add_system_file("Generic", test_kl_path.clone()));

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Test Device".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    assert_some!(manager.preload_key_layout_map(&device).unwrap());
    let cache = manager.key_layout_maps.lock().unwrap();
    assert!(cache.contains_key(&device));
    assert_some!(cache.get(&device).unwrap());
}

#[test]
fn test_preload_key_layout_map_saves_none_when_not_found() {
    // Create a mock file finder that returns no files
    let mock_finder = Arc::new(MockFileFinder::new());

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Unknown Device".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    assert_none!(manager.preload_key_layout_map(&device).unwrap());
    let cache = manager.key_layout_maps.lock().unwrap();
    assert!(cache.contains_key(&device));
    assert_none!(cache.get(&device).unwrap());
}

#[test]
fn test_map_key_reads_from_cache() {
    // Create a mock file finder that returns no files
    let test_kl_path = get_test_data_path().join("Generic.kl");
    let mock_finder =
        Arc::new(MockFileFinder::new().add_system_file("Generic", test_kl_path.clone()));

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Test Device".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    let cached_key_layout_map = manager.preload_key_layout_map(&device).unwrap().unwrap();
    let cached_key = cached_key_layout_map.map_key(12).unwrap();

    // Verify the cached value is returned
    let map_key_result = manager.map_key(&device, 12).unwrap().unwrap();

    assert_eq!(cached_key, map_key_result, "Key codes should be equal");
}

#[test]
fn test_map_key_saves_to_cache() {
    // Create a mock file finder that returns no files
    let test_kl_path = get_test_data_path().join("Generic.kl");
    let mock_finder =
        Arc::new(MockFileFinder::new().add_system_file("Generic", test_kl_path.clone()));

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Test Device".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    // Verify the cached value is returned
    let cached_key = manager.map_key(&device, 12).unwrap().unwrap();
    let map_key = manager.map_key(&device, 12).unwrap().unwrap();

    assert_eq!(cached_key, map_key, "Key codes should be equal");
}

#[test]
fn test_map_key_finds_file_if_cache_miss() {
    // Create a mock file finder that returns no files
    let test_kl_path = get_test_data_path().join("Generic.kl");
    let mock_finder =
        Arc::new(MockFileFinder::new().add_system_file("Generic", test_kl_path.clone()));

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Test Device".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    // Verify the key is found and has the correct key code
    let map_key_result = manager.map_key(&device, 12).unwrap().unwrap();

    assert_eq!(map_key_result, AKEYCODE_MINUS);
}

#[test]
fn test_map_key_reads_first_found_path() {
    // Create a mock file finder that returns no files
    let mock_finder = MockFileFinder::new()
        .add_system_file("Generic", get_test_data_path().join("Generic.kl"))
        .add_system_file("gpio-keys", get_test_data_path().join("6t/gpio-keys.kl"));

    let manager = KeyLayoutMapManager::with_file_finder(Arc::new(mock_finder));

    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    // Verify the correct key code is returned (device-specific file takes priority)
    let map_key_result = manager.map_key(&device, 102).unwrap().unwrap();

    // In gpio-keys.kl this is HOME and in Generic.kl this is MOVE_HOME
    assert_eq!(map_key_result, AKEYCODE_HOME);
}

#[test]
fn test_map_key_reads_generic_if_device_specific_not_found() {
    // Create a mock file finder that returns no files
    let mock_finder =
        MockFileFinder::new().add_system_file("Generic", get_test_data_path().join("Generic.kl"));

    let manager = KeyLayoutMapManager::with_file_finder(Arc::new(mock_finder));

    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    // Verify the fallback to Generic.kl works
    let map_key_result = manager.map_key(&device, 102).unwrap().unwrap();

    // In gpio-keys.kl this is HOME and in Generic.kl this is MOVE_HOME
    assert_eq!(map_key_result, AKEYCODE_MOVE_HOME);
}
