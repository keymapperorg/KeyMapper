//! Integration tests for key layout map parsing.
use evdev_manager_core::android::android_codes::{AKEYCODE_F4, AKEYCODE_FUNCTION};
use evdev_manager_core::android::keylayout::key_layout_map;
use evdev_manager_core::android::keylayout::key_layout_map::{KeyLayoutAxisMode, KeyLayoutMap};
use glob::glob;
#[cfg(test)]
use pretty_assertions::assert_eq;
use std::path::{Path, PathBuf};

fn get_test_data_dir() -> &'static str {
    env!("CARGO_MANIFEST_DIR")
}

fn get_test_data_path() -> String {
    format!("{}/tests/test_data", get_test_data_dir())
}

fn load_key_layout_map(file_name: &str) -> KeyLayoutMap {
    let file_path = PathBuf::new().join(get_test_data_path()).join(file_name);
    KeyLayoutMap::load_from_file(file_path).unwrap()
}

#[test]
fn test_parse_int() {
    assert_eq!(key_layout_map::parse_int("123"), Some(123));
    assert_eq!(key_layout_map::parse_int("-999"), Some(-999));
    assert_eq!(key_layout_map::parse_int("0x1a"), Some(26));
    assert_eq!(key_layout_map::parse_int("0XFF"), Some(255));
    assert_eq!(key_layout_map::parse_int("077"), Some(63)); // octal
    assert_eq!(key_layout_map::parse_int(""), None);
    assert_eq!(key_layout_map::parse_int("abc"), None);
}

#[test]
fn test_load_from_contents_simple() {
    let contents = "key 1 ESCAPE\nkey 2 1\n";
    let map = KeyLayoutMap::load_from_contents(contents).unwrap();

    let key_code = map.map_key(1).unwrap();
    assert_eq!(key_code, 111); // ESCAPE

    let key_code = map.map_key(2).unwrap();
    assert_eq!(key_code, 8); // KEYCODE_1
}

#[test]
fn test_load_from_contents_with_flags() {
    // Flags are parsed but ignored - we just verify the key code is correct
    let contents = "key 465 ESCAPE FUNCTION\n";
    let map = KeyLayoutMap::load_from_contents(contents).unwrap();

    let key_code = map.map_key(465).unwrap();
    assert_eq!(key_code, 111); // ESCAPE
}

#[test]
fn test_load_from_contents_axis() {
    let contents = "axis 0x00 X\naxis 0x01 Y\n";
    let map = KeyLayoutMap::load_from_contents(contents).unwrap();

    let axis_info = map.map_axis(0x00).unwrap();
    assert_eq!(axis_info.mode, KeyLayoutAxisMode::Normal);
    assert_eq!(axis_info.axis, 0); // X axis

    let axis_info = map.map_axis(0x01).unwrap();
    assert_eq!(axis_info.mode, KeyLayoutAxisMode::Normal);
    assert_eq!(axis_info.axis, 1); // Y axis
}

#[test]
fn test_function_key_line() {
    let content = "key 464   FUNCTION";
    let map = KeyLayoutMap::load_from_contents(content).unwrap();
    let key_code = map.map_key(464).unwrap();
    assert_eq!(key_code, AKEYCODE_FUNCTION);
}

#[test]
fn test_f4_key_line() {
    let content = "key 469   F4                FUNCTION";
    let map = KeyLayoutMap::load_from_contents(content).unwrap();
    let key_code = map.map_key(469).unwrap();
    assert_eq!(key_code, AKEYCODE_F4);
}

#[test]
fn test_brightness_usage_key_lines() {
    let content = "\
    key usage 0x0c0067 WINDOW
    key usage 0x0c006F BRIGHTNESS_UP
    key usage 0x0c0070 BRIGHTNESS_DOWN";

    KeyLayoutMap::load_from_contents(content).unwrap();
    // Just do not crash because it should be skipped.
}

#[test]
fn test_load_generic_kl() {
    let map = load_key_layout_map("Generic.kl");

    // Test some basic key mappings
    let key_code = map.map_key(1).expect("Scan code 1 should map to ESCAPE");
    assert_eq!(key_code, 111); // ESCAPE

    let key_code = map.map_key(2).expect("Scan code 2 should map to 1");
    assert_eq!(key_code, 8); // KEYCODE_1

    let key_code = map.map_key(15).expect("Scan code 15 should map to TAB");
    assert_eq!(key_code, 61); // TAB
}

#[test]
fn test_parse_all_key_entries() {
    let map = load_key_layout_map("Generic.kl");

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
        let key_code = map
            .map_key(scan_code)
            .unwrap_or_else(|| panic!("Scan code {} should be mapped", scan_code));
        assert_eq!(
            key_code, expected_key_code,
            "Scan code {} should map to key code {}",
            scan_code, expected_key_code
        );
    }
}

#[test]
fn test_parse_axis_entries() {
    let map = load_key_layout_map("Generic.kl");

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
    let map = load_key_layout_map("Generic.kl");

    // Test mapping various scan codes
    let key_code = map.map_key(1).unwrap();
    assert_eq!(key_code, 111); // ESCAPE

    // Test a key with FUNCTION flag (flags are now ignored)
    let key_code = map.map_key(465).unwrap();
    assert_eq!(key_code, 111); // ESCAPE

    // Test that non-existent scan code returns None
    assert!(map.map_key(9999).is_none());
}

#[test]
fn test_map_axis_functionality() {
    let map = load_key_layout_map("Generic.kl");

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
fn test_find_scan_code_for_key() {
    let map = load_key_layout_map("Generic.kl");

    // Find scan code for ESCAPE - returns first scan code (1, not 465 with FUNCTION flag)
    let scan_code = map.find_scan_code_for_key(111); // ESCAPE
    assert_eq!(scan_code, Some(1), "Should find scan code 1 for ESCAPE");

    // Find scan code for a common key
    let scan_code = map.find_scan_code_for_key(8); // KEYCODE_1
    assert_eq!(scan_code, Some(2), "Should find scan code 2 for KEYCODE_1");

    // Non-existent key code should return None
    assert!(map.find_scan_code_for_key(9999).is_none());
}

#[test]
fn test_reverse_mapping_basic() {
    let contents = "key 1 ESCAPE\nkey 2 1\nkey 100 ESCAPE\n";
    let map = KeyLayoutMap::load_from_contents(contents).unwrap();

    // ESCAPE (111) returns the first scan code (1)
    let scan_code = map.find_scan_code_for_key(111);
    assert_eq!(
        scan_code,
        Some(1),
        "Should return first scan code for ESCAPE"
    );

    // KEYCODE_1 (8) should have scan code 2
    let scan_code = map.find_scan_code_for_key(8);
    assert_eq!(
        scan_code,
        Some(2),
        "Should return scan code 2 for KEYCODE_1"
    );

    // Non-existent key code should return None
    assert!(
        map.find_scan_code_for_key(9999).is_none(),
        "Non-existent key code should return None"
    );
}

#[test]
fn test_reverse_mapping_keeps_first_scan_code() {
    // When multiple scan codes map to the same key code, we keep the first one
    let contents = "key 1 ESCAPE\nkey 465 ESCAPE FUNCTION\n";
    let map = KeyLayoutMap::load_from_contents(contents).unwrap();

    // Should return the first scan code (1), not 465
    let scan_code = map.find_scan_code_for_key(111);
    assert_eq!(
        scan_code,
        Some(1),
        "Should return first scan code for ESCAPE"
    );
}

#[test]
fn test_bidirectional_mapping() {
    let contents = "key 1 ESCAPE\nkey 2 1\nkey 28 ENTER\n";
    let map = KeyLayoutMap::load_from_contents(contents).unwrap();

    // Forward mapping
    assert_eq!(map.map_key(1), Some(111)); // ESCAPE
    assert_eq!(map.map_key(2), Some(8)); // KEYCODE_1
    assert_eq!(map.map_key(28), Some(66)); // ENTER

    // Reverse mapping
    assert_eq!(map.find_scan_code_for_key(111), Some(1)); // ESCAPE -> 1
    assert_eq!(map.find_scan_code_for_key(8), Some(2)); // KEYCODE_1 -> 2
    assert_eq!(map.find_scan_code_for_key(66), Some(28)); // ENTER -> 28
}

#[test]
fn test_load_all_files_in_test_data() {
    let test_data_path = get_test_data_path();
    let test_data_dir = Path::new(&test_data_path);
    if !test_data_dir.exists() {
        panic!("Test data directory does not exist: {}", test_data_path);
    }

    // Use glob pattern to recursively find all .kl files
    let pattern = format!("{}/**/*.kl", test_data_path);
    let entries = glob(&pattern).expect("Failed to read glob pattern");

    for entry in entries {
        let path = entry.expect("Failed to read glob entry");
        println!("Testing file: {:?}", path.clone());

        let result = KeyLayoutMap::load_from_file(path.clone());
        assert!(
            result.is_ok(),
            "Failed to load key layout file: {:?} - {:?}",
            path.clone(),
            result.err()
        );

        // At least verify it's not empty
        result.unwrap();
        // We can't check exact counts as files may vary
        println!("  Loaded successfully");
    }
}
