//! Tests for KeyLayoutMapManager file finding logic.
use assertables::{assert_iter_eq, assert_some, assert_some_eq};
use evdev_manager_core::android::android_codes::{
    AKEYCODE_ESCAPE, AKEYCODE_HOME, AKEYCODE_MINUS, AKEYCODE_MOVE_HOME, AKEYCODE_SPACE,
};
use evdev_manager_core::android::keylayout::key_layout_map_manager::{
    get_generic_key_layout_map, KeyLayoutFileFinder, KeyLayoutMapManager,
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
fn test_preload_key_layout_map_uses_fallback_when_not_found() {
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

    // Should return the hardcoded fallback, not None
    assert_some!(manager.preload_key_layout_map(&device).unwrap());
    let cache = manager.key_layout_maps.lock().unwrap();
    assert!(cache.contains_key(&device));
    assert_some!(cache.get(&device).unwrap());
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

#[test]
fn test_get_generic_key_layout_map_returns_valid_map() {
    let generic_map = get_generic_key_layout_map();

    // Test some well-known key mappings from Generic.kl
    assert_eq!(generic_map.map_key(1), Some(AKEYCODE_ESCAPE)); // ESCAPE
    assert_eq!(generic_map.map_key(57), Some(AKEYCODE_SPACE)); // SPACE
    assert_eq!(generic_map.map_key(102), Some(AKEYCODE_MOVE_HOME)); // MOVE_HOME
}

#[test]
fn test_get_generic_key_layout_map_is_static() {
    // Calling get_generic_key_layout_map multiple times should return the same instance
    let map1 = get_generic_key_layout_map();
    let map2 = get_generic_key_layout_map();

    // They should be the same Arc (same pointer)
    assert!(Arc::ptr_eq(&map1, &map2));
}

#[test]
fn test_get_generic_key_layout_map_reverse_lookup() {
    let generic_map = get_generic_key_layout_map();

    let escape_scan_code = generic_map.find_scan_code_for_key(AKEYCODE_ESCAPE);
    assert_some_eq!(escape_scan_code, Some(1));
}

#[test]
fn test_fallback_to_hardcoded_generic_when_no_files_found() {
    // Create a mock file finder that returns no files at all
    let mock_finder = Arc::new(MockFileFinder::new());

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Unknown Device".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    // Should still return a valid key layout map (the hardcoded fallback)
    let result = manager.preload_key_layout_map(&device).unwrap();
    assert!(result.is_some(), "Should return hardcoded Generic fallback");

    // Verify the fallback map has the expected mappings
    let map = result.unwrap();
    assert_eq!(map.map_key(1), Some(AKEYCODE_ESCAPE)); // ESCAPE
    assert_eq!(map.map_key(57), Some(AKEYCODE_SPACE)); // SPACE
    assert_eq!(map.map_key(102), Some(AKEYCODE_MOVE_HOME)); // MOVE_HOME
}

#[test]
fn test_fallback_map_key_works_when_no_files_found() {
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

    // map_key should work using the hardcoded fallback
    let key_code = manager.map_key(&device, 1).unwrap();
    assert_eq!(key_code, Some(AKEYCODE_ESCAPE));

    let key_code = manager.map_key(&device, 57).unwrap();
    assert_eq!(key_code, Some(AKEYCODE_SPACE));

    // Unknown scan code should return None
    let key_code = manager.map_key(&device, 9999).unwrap();
    assert_eq!(key_code, None);
}

#[test]
fn test_fallback_is_cached() {
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

    // First call should use the fallback
    let result1 = manager.preload_key_layout_map(&device).unwrap().unwrap();

    // Second call should return the same cached instance
    let result2 = manager.preload_key_layout_map(&device).unwrap().unwrap();

    assert!(
        Arc::ptr_eq(&result1, &result2),
        "Fallback should be cached and return same Arc instance"
    );
}

#[test]
fn test_fallback_uses_static_generic_map() {
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

    // Get the fallback via manager
    let manager_map = manager.preload_key_layout_map(&device).unwrap().unwrap();

    // Get the static generic map directly
    let static_map = get_generic_key_layout_map();

    // They should be the same Arc instance
    assert!(
        Arc::ptr_eq(&manager_map, &static_map),
        "Manager fallback should use the static generic map"
    );
}

#[test]
fn test_map_key_falls_back_to_generic_for_missing_scan_code() {
    // gpio-keys.kl only has a few keys (like HOME on scan code 102)
    // It doesn't have ESCAPE (scan code 1), so it should fall back to Generic
    let mock_finder = Arc::new(
        MockFileFinder::new()
            .add_system_file("gpio-keys", get_test_data_path().join("6t/gpio-keys.kl")),
    );

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x0000,
        product: 0x0000,
        version: 0x0000,
    };

    // Scan code 102 is in gpio-keys.kl and maps to HOME
    let key_code = manager.map_key(&device, 102).unwrap();
    assert_eq!(key_code, Some(AKEYCODE_HOME));

    // Scan code 1 (ESCAPE) is NOT in gpio-keys.kl, so it should fall back to Generic
    let key_code = manager.map_key(&device, 1).unwrap();
    assert_eq!(key_code, Some(AKEYCODE_ESCAPE));

    // Scan code 57 (SPACE) is NOT in gpio-keys.kl, so it should fall back to Generic
    let key_code = manager.map_key(&device, 57).unwrap();
    assert_eq!(key_code, Some(AKEYCODE_SPACE));
}

#[test]
fn test_find_scan_code_for_key_falls_back_to_generic_for_missing_key_code() {
    // gpio-keys.kl only has a few keys
    // It doesn't have ESCAPE, so reverse lookup should fall back to Generic
    let mock_finder = Arc::new(
        MockFileFinder::new()
            .add_system_file("gpio-keys", get_test_data_path().join("6t/gpio-keys.kl")),
    );

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "gpio-keys".to_string(),
        bus: 0x0003,
        vendor: 0x0000,
        product: 0x0000,
        version: 0x0000,
    };

    // HOME is in gpio-keys.kl, should return scan code 102
    let scan_code = manager
        .find_scan_code_for_key(&device, AKEYCODE_HOME)
        .unwrap();
    assert_eq!(scan_code, Some(102));

    // ESCAPE is NOT in gpio-keys.kl, should fall back to Generic (scan code 1)
    let scan_code = manager
        .find_scan_code_for_key(&device, AKEYCODE_ESCAPE)
        .unwrap();
    assert_eq!(scan_code, Some(1));

    // SPACE is NOT in gpio-keys.kl, should fall back to Generic (scan code 57)
    let scan_code = manager
        .find_scan_code_for_key(&device, AKEYCODE_SPACE)
        .unwrap();
    assert_eq!(scan_code, Some(57));
}

#[test]
fn test_find_scan_code_for_key_reads_from_cache() {
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
    let cached_scan_code = cached_key_layout_map
        .find_scan_code_for_key(AKEYCODE_MINUS)
        .unwrap();

    // Verify the cached value is returned
    let scan_code_result = manager
        .find_scan_code_for_key(&device, AKEYCODE_MINUS)
        .unwrap()
        .unwrap();

    assert_eq!(
        cached_scan_code, scan_code_result,
        "Scan codes should be equal"
    );
}

#[test]
fn test_find_scan_code_for_key_saves_to_cache() {
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

    // First call loads and caches the map
    let first_result = manager
        .find_scan_code_for_key(&device, AKEYCODE_MINUS)
        .unwrap()
        .unwrap();
    // Second call should use the cache
    let second_result = manager
        .find_scan_code_for_key(&device, AKEYCODE_MINUS)
        .unwrap()
        .unwrap();

    assert_eq!(first_result, second_result, "Scan codes should be equal");
}

#[test]
fn test_find_scan_code_for_key_finds_file_if_cache_miss() {
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

    // MINUS key code should map to scan code 12 in Generic.kl
    let scan_code_result = manager
        .find_scan_code_for_key(&device, AKEYCODE_MINUS)
        .unwrap()
        .unwrap();

    assert_eq!(scan_code_result, 12);
}

#[test]
fn test_find_scan_code_for_key_reads_first_found_path() {
    // gpio-keys.kl has HOME -> 102, Generic.kl has HOME -> 172
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

    // Device-specific file (gpio-keys.kl) should take priority
    // In gpio-keys.kl, HOME maps to scan code 102
    let scan_code_result = manager
        .find_scan_code_for_key(&device, AKEYCODE_HOME)
        .unwrap()
        .unwrap();

    assert_eq!(scan_code_result, 102);
}

#[test]
fn test_find_scan_code_for_key_reads_generic_if_device_not_found() {
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

    // No device-specific file found, fallback to Generic.kl
    // In Generic.kl, MOVE_HOME maps to scan code 102
    let scan_code_result = manager
        .find_scan_code_for_key(&device, AKEYCODE_MOVE_HOME)
        .unwrap()
        .unwrap();

    assert_eq!(scan_code_result, 102);
}

#[test]
fn test_find_scan_code_for_key_returns_none_for_unknown_key() {
    let mock_finder = Arc::new(MockFileFinder::new());

    let manager = KeyLayoutMapManager::with_file_finder(mock_finder);

    let device = DeviceIdentifier {
        name: "Unknown Device".to_string(),
        bus: 0x0003,
        vendor: 0x9999,
        product: 0x8888,
        version: 0x0001,
    };

    // Unknown key code should return None
    let scan_code = manager.find_scan_code_for_key(&device, 99999).unwrap();
    assert_eq!(scan_code, None);
}
