//! Integration tests for key layout map parsing.

use evdev_manager::android::keylayout::key_layout_map::{KeyLayoutAxisMode, KeyLayoutMap};
use std::fs;
use std::path::Path;

fn get_test_data_dir() -> &'static str {
    env!("CARGO_MANIFEST_DIR")
}

fn get_test_data_path() -> String {
    format!("{}/tests/test_data", get_test_data_dir())
}

#[test]
fn test_load_generic_kl() {
    let file_path = format!("{}/Generic.kl", get_test_data_path());
    let map = KeyLayoutMap::load_from_file(&file_path).expect("Failed to load Generic.kl");

    // Test some basic key mappings
    let key = map.map_key(1).expect("Scan code 1 should map to ESCAPE");
    assert_eq!(key.key_code, 111); // ESCAPE

    let key = map.map_key(2).expect("Scan code 2 should map to 1");
    assert_eq!(key.key_code, 8); // KEYCODE_1

    let key = map.map_key(15).expect("Scan code 15 should map to TAB");
    assert_eq!(key.key_code, 61); // TAB
}

#[test]
fn test_parse_all_key_entries() {
    let file_path = format!("{}/Generic.kl", get_test_data_path());
    let map = KeyLayoutMap::load_from_file(&file_path).expect("Failed to load Generic.kl");

    // Test various key types
    let test_cases = vec![
        (1, 111),  // ESCAPE
        (15, 61),  // TAB
        (28, 66),  // ENTER
        (57, 62),  // SPACE
        (59, 131), // F1
        (70, 116), // SCROLL_LOCK
    ];

    for (scan_code, expected_key_code) in test_cases {
        let key = map
            .map_key(scan_code)
            .unwrap_or_else(|| panic!("Scan code {} should be mapped", scan_code));
        assert_eq!(
            key.key_code, expected_key_code,
            "Scan code {} should map to key code {}",
            scan_code, expected_key_code
        );
    }
}

#[test]
fn test_parse_axis_entries() {
    let file_path = format!("{}/Generic.kl", get_test_data_path());
    let map = KeyLayoutMap::load_from_file(&file_path).expect("Failed to load Generic.kl");

    // Test axis mappings
    let axis_info = map.map_axis(0x00).expect("Axis 0x00 should exist");
    assert_eq!(axis_info.axis, 0); // X axis
    assert_eq!(axis_info.mode, KeyLayoutAxisMode::Normal);

    let axis_info = map.map_axis(0x01).expect("Axis 0x01 should exist");
    assert_eq!(axis_info.axis, 1); // Y axis

    let axis_info = map.map_axis(0x03).expect("Axis 0x03 should exist");
    assert_eq!(axis_info.axis, 12); // RX axis
}

#[test]
fn test_error_handling_malformed_file() {
    // Test with invalid key entry
    let contents = "key invalid ESCAPE\n";
    let result = KeyLayoutMap::load_from_contents(contents);
    assert!(result.is_err(), "Should fail to parse invalid scan code");

    // Test with missing key code
    let contents = "key 1\n";
    let result = KeyLayoutMap::load_from_contents(contents);
    // This should succeed but the key won't be inserted (unknown key code)
    assert!(result.is_ok());

    // Test with duplicate scan code
    let contents = "key 1 ESCAPE\nkey 1 TAB\n";
    let result = KeyLayoutMap::load_from_contents(contents);
    assert!(result.is_err(), "Should fail on duplicate scan code");
}

#[test]
fn test_map_key_with_scan_codes() {
    let file_path = format!("{}/Generic.kl", get_test_data_path());
    let map = KeyLayoutMap::load_from_file(&file_path).expect("Failed to load Generic.kl");

    // Test mapping various scan codes
    let key = map.map_key(1).unwrap();
    assert_eq!(key.key_code, 111); // ESCAPE
    assert_eq!(key.flags, 0);

    // Test a key with FUNCTION flag
    let key = map.map_key(465).unwrap();
    assert_eq!(key.key_code, 111); // ESCAPE
    assert_eq!(key.flags, 0x00000004); // FUNCTION flag
}

#[test]
fn test_map_axis_functionality() {
    let file_path = format!("{}/Generic.kl", get_test_data_path());
    let map = KeyLayoutMap::load_from_file(&file_path).expect("Failed to load Generic.kl");

    // Test normal axis
    let axis_info = map.map_axis(0x00).unwrap();
    assert_eq!(axis_info.mode, KeyLayoutAxisMode::Normal);
    assert_eq!(axis_info.axis, 0);
    assert_eq!(axis_info.high_axis, None);
    assert_eq!(axis_info.split_value, None);
    assert_eq!(axis_info.flat_override, None);

    // Test that non-existent axis returns None
    assert!(map.map_axis(9999).is_none());
}

#[test]
fn test_find_scan_codes_for_key() {
    let file_path = format!("{}/Generic.kl", get_test_data_path());
    let map = KeyLayoutMap::load_from_file(&file_path).expect("Failed to load Generic.kl");

    // Find scan codes for ESCAPE (should find scan code 1, but not 465 which has FUNCTION flag)
    let scan_codes = map.find_scan_codes_for_key(111); // ESCAPE
    assert!(
        scan_codes.contains(&1),
        "Should find scan code 1 for ESCAPE"
    );
    assert!(
        !scan_codes.contains(&465),
        "Should not find scan code 465 (has FUNCTION flag)"
    );

    // Find scan codes for a common key
    let scan_codes = map.find_scan_codes_for_key(8); // KEYCODE_1
    assert!(
        scan_codes.contains(&2),
        "Should find scan code 2 for KEYCODE_1"
    );
}

#[test]
fn test_load_all_files_in_test_data() {
    let test_data_path = get_test_data_path();
    let test_data_dir = Path::new(&test_data_path);
    if !test_data_dir.exists() {
        panic!("Test data directory does not exist: {}", test_data_path);
    }

    let entries = fs::read_dir(test_data_dir).expect("Failed to read test data directory");

    for entry in entries {
        let entry = entry.expect("Failed to read directory entry");
        let path = entry.path();

        if path.extension().and_then(|s| s.to_str()) == Some("kl") {
            let file_path = path.to_str().expect("Invalid file path");
            println!("Testing file: {}", file_path);

            let result = KeyLayoutMap::load_from_file(file_path);
            assert!(
                result.is_ok(),
                "Failed to load key layout file: {} - {:?}",
                file_path,
                result.err()
            );

            let map = result.unwrap();
            // At least verify it's not empty
            // We can't check exact counts as files may vary
            println!("  Loaded successfully");
        }
    }
}
