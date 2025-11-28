//! Integration tests for key layout map parsing.

use evdev_manager_core::android::android_codes::{
    AKEYCODE_A, AKEYCODE_FUNCTION, AMOTION_EVENT_AXIS_X, POLICY_FLAG_VIRTUAL,
};
use evdev_manager_core::android::keylayout::input_event_lookup;

#[test]
fn test_function_key_lookup() {
    let key_code = input_event_lookup::get_key_code_by_label("FUNCTION");
    assert_eq!(key_code, Some(AKEYCODE_FUNCTION));
}

#[test]
fn test_letter_key_lookup() {
    let key_code = input_event_lookup::get_key_code_by_label("A");
    assert_eq!(key_code, Some(AKEYCODE_A));
}

#[test]
fn test_policy_flag_lookup() {
    let code = input_event_lookup::get_key_flag_by_label("VIRTUAL");
    assert_eq!(code, Some(POLICY_FLAG_VIRTUAL));
}

#[test]
fn test_axis_lookup() {
    let code = input_event_lookup::get_axis_by_label("X");
    assert_eq!(code, Some(AMOTION_EVENT_AXIS_X));
}
