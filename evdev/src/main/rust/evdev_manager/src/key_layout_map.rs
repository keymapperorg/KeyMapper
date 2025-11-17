//! Key layout map parser for Android key layout files.
//!
//! This module provides functionality to parse Android key layout (.kl) files
//! and map scan codes to Android key codes.
//!
//! AOSP keylayout files can be found at:
//! https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/data/keyboards/

use crate::android::android_codes::POLICY_FLAG_FUNCTION;
use crate::input_event_lookup::{get_axis_by_label, get_key_code_by_label, get_key_flag_by_label};
use crate::tokenizer::Tokenizer;
use std::collections::HashMap;
use std::fmt::format;

/// Describes a mapping from keyboard scan codes to Android key codes.
///
/// This object is immutable after it has been loaded.
pub struct KeyLayoutMap {
    keys_by_scan_code: HashMap<u32, KeyLayoutKey>,
    axes: HashMap<u32, KeyLayoutAxisInfo>,
}

/// Represents a key mapping entry.
#[derive(Debug, Clone)]
pub struct KeyLayoutKey {
    pub key_code: u32,
    flags: u32,
}

/// Represents axis information for joystick/gamepad axes.
#[derive(Debug, Clone)]
pub struct KeyLayoutAxisInfo {
    mode: KeyLayoutAxisMode,
    axis: u32,
    high_axis: Option<u32>,
    split_value: Option<i32>,
    flat_override: Option<i32>,
}

/// Axis mapping mode.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum KeyLayoutAxisMode {
    /// Axis value is reported directly.
    Normal,
    /// Axis value should be inverted before reporting.
    Invert,
    /// Axis value should be split into two axes.
    Split,
}

impl KeyLayoutMap {
    /// Load a key layout map from a file path.
    pub fn load_from_file(file_path: &str) -> Result<Self, String> {
        let tokenizer = Tokenizer::from_file(file_path)?;
        Self::load(tokenizer)
    }

    /// Load a key layout map from file contents (useful for testing).
    pub fn load_from_contents(contents: &str) -> Result<Self, String> {
        let tokenizer = Tokenizer::from_contents("", contents);
        Self::load(tokenizer)
    }

    fn load(mut tokenizer: Tokenizer) -> Result<Self, String> {
        let mut map = KeyLayoutMap {
            keys_by_scan_code: HashMap::new(),
            axes: HashMap::new(),
        };

        let mut parser = Parser::new(&mut map, &mut tokenizer);
        parser.parse()?;

        Ok(map)
    }

    /// Map a scan code to an Android key code.
    ///
    /// Returns `Ok((key_code, flags))` on success, or
    pub fn map_key(&self, scan_code: u32) -> Option<KeyLayoutKey> {
        self.keys_by_scan_code.get(&scan_code).cloned()
    }

    /// Map a scan code to axis information.
    ///
    /// Returns `Some(axis_info)` if the scan code maps to an axis, or `None` if not found.
    pub fn map_axis(&self, scan_code: u32) -> Option<KeyLayoutAxisInfo> {
        self.axes.get(&scan_code).cloned()
    }

    /// Find all scan codes that map to the given key code.
    ///
    /// Only considers keys without the FUNCTION flag.
    pub fn find_scan_codes_for_key(&self, key_code: u32) -> Vec<u32> {
        self.keys_by_scan_code
            .iter()
            .filter_map(|(scan_code, key)| {
                if key.key_code == key_code && (key.flags & POLICY_FLAG_FUNCTION) == 0 {
                    Some(*scan_code)
                } else {
                    None
                }
            })
            .collect()
    }
}

/// Parser for key layout map files.
struct Parser<'a> {
    map: &'a mut KeyLayoutMap,
    tokenizer: &'a mut Tokenizer,
}

impl<'a> Parser<'a> {
    fn new(map: &'a mut KeyLayoutMap, tokenizer: &'a mut Tokenizer) -> Self {
        Self { map, tokenizer }
    }

    fn parse(&mut self) -> Result<(), String> {
        const WHITESPACE: &str = " \t\r";

        while !self.tokenizer.is_eof() {
            self.tokenizer.skip_delimiters(WHITESPACE);

            if !self.tokenizer.is_eol() && self.tokenizer.peek_char() != '#' {
                let keyword_token = self.tokenizer.next_token(WHITESPACE);
                match keyword_token.as_str() {
                    "key" => {
                        self.tokenizer.skip_delimiters(WHITESPACE);
                        self.parse_key()?;
                    }
                    "axis" => {
                        self.tokenizer.skip_delimiters(WHITESPACE);
                        self.parse_axis()?;
                    }
                    "led" | "sensor" => {
                        // Skip LEDs and sensors, we don't need them
                        self.tokenizer.next_line();
                        continue;
                    }
                    "usage" => {
                        // Skip usage code entries - evdev only provides scan codes
                        self.tokenizer.next_line();
                        continue;
                    }
                    _ => {
                        return Err(format!(
                            "{}: Expected keyword, got '{}'.",
                            self.tokenizer.get_location(),
                            keyword_token
                        ));
                    }
                }

                self.tokenizer.skip_delimiters(WHITESPACE);

                if !self.tokenizer.is_eol() && self.tokenizer.peek_char() != '#' {
                    return Err(format!(
                        "{}: Expected end of line or trailing comment, got '{}'.",
                        self.tokenizer.get_location(),
                        self.tokenizer.peek_remainder_of_line()
                    ));
                }
            }

            self.tokenizer.next_line();
        }

        Ok(())
    }

    fn parse_key(&mut self) -> Result<(), String> {
        const WHITESPACE: &str = " \t\r";

        let code_token = self.tokenizer.next_token(WHITESPACE);

        // Skip "usage" entries - we only support scan codes
        if code_token == "usage" {
            self.tokenizer.next_line();
            return Ok(());
        }

        let scan_code = parse_int(&code_token).ok_or_else(|| {
            format!(
                "{}: Expected key scan code number, got '{}'.",
                self.tokenizer.get_location(),
                code_token
            )
        })?;

        if scan_code < 0 {
            return Err(format!(
                "{} is not a valid key scan code. Negative numbers are not allowed.",
                scan_code
            ));
        }

        if self.map.keys_by_scan_code.contains_key(&(scan_code as u32)) {
            return Err(format!(
                "{}: Duplicate entry for key scan code '{}'.",
                self.tokenizer.get_location(),
                code_token
            ));
        }

        self.tokenizer.skip_delimiters(WHITESPACE);
        let key_code_token = self.tokenizer.next_token(WHITESPACE);
        let key_code = get_key_code_by_label(&key_code_token);

        let mut flags = 0u32;
        loop {
            self.tokenizer.skip_delimiters(WHITESPACE);
            if self.tokenizer.is_eol() || self.tokenizer.peek_char() == '#' {
                break;
            }

            let flag_token = self.tokenizer.next_token(WHITESPACE);
            let flag = get_key_flag_by_label(&flag_token).ok_or_else(|| {
                format!(
                    "{}: Expected key flag label, got '{}'.",
                    self.tokenizer.get_location(),
                    flag_token
                )
            })?;

            if (flags & flag) != 0 {
                return Err(format!(
                    "{}: Duplicate key flag '{}'.",
                    self.tokenizer.get_location(),
                    flag_token
                ));
            }
            flags |= flag;
        }

        // Only insert if the key code is known
        if let Some(key_code) = key_code {
            let key = KeyLayoutKey { key_code, flags };
            self.map.keys_by_scan_code.insert(scan_code as u32, key);
        }

        Ok(())
    }

    fn parse_axis(&mut self) -> Result<(), String> {
        const WHITESPACE: &str = " \t\r";

        let scan_code_token = self.tokenizer.next_token(WHITESPACE);
        let scan_code = parse_int(&scan_code_token).ok_or_else(|| {
            format!(
                "{}: Expected axis scan code number, got '{}'.",
                self.tokenizer.get_location(),
                scan_code_token
            )
        })?;

        if scan_code < 0 {
            return Err(format!(
                "{} is not a valid key scan code for an axis. Negative numbers are not allowed.",
                scan_code
            ));
        }

        if self.map.axes.contains_key(&(scan_code as u32)) {
            return Err(format!(
                "{}: Duplicate entry for axis scan code '{}'.",
                self.tokenizer.get_location(),
                scan_code_token
            ));
        }

        let mut axis_mode: KeyLayoutAxisMode = KeyLayoutAxisMode::Normal;
        let axis: u32;
        let mut split_value: Option<i32> = None;
        let mut high_axis: Option<u32> = None;
        let mut flat_override: Option<i32> = None;

        self.tokenizer.skip_delimiters(WHITESPACE);
        let token = self.tokenizer.next_token(WHITESPACE);

        if token == "invert" {
            axis_mode = KeyLayoutAxisMode::Invert;

            self.tokenizer.skip_delimiters(WHITESPACE);

            let axis_token = self.tokenizer.next_token(WHITESPACE);

            axis = get_axis_by_label(&axis_token).ok_or_else(|| {
                format!(
                    "{}: Expected inverted axis label, got '{}'.",
                    self.tokenizer.get_location(),
                    axis_token
                )
            })?;
        } else if token == "split" {
            axis_mode = KeyLayoutAxisMode::Split;

            self.tokenizer.skip_delimiters(WHITESPACE);
            let split_token = self.tokenizer.next_token(WHITESPACE);
            let split_value_raw = parse_int(&split_token).ok_or_else(|| {
                format!(
                    "{}: Expected split value, got '{}'.",
                    self.tokenizer.get_location(),
                    split_token
                )
            })?;
            split_value = Some(split_value_raw);

            self.tokenizer.skip_delimiters(WHITESPACE);
            let low_axis_token = self.tokenizer.next_token(WHITESPACE);
            axis = get_axis_by_label(&low_axis_token).ok_or_else(|| {
                format!(
                    "{}: Expected low axis label, got '{}'.",
                    self.tokenizer.get_location(),
                    low_axis_token
                )
            })?;

            self.tokenizer.skip_delimiters(WHITESPACE);
            let high_axis_token = self.tokenizer.next_token(WHITESPACE);
            let high_axis_raw = get_axis_by_label(&high_axis_token).ok_or_else(|| {
                format!(
                    "{}: Expected high axis label, got '{}'.",
                    self.tokenizer.get_location(),
                    high_axis_token
                )
            })?;
            high_axis = Some(high_axis_raw);
        } else {
            axis = get_axis_by_label(&token).ok_or_else(|| {
                format!(
                    "{}: Expected axis label, 'split' or 'invert', got '{}'.",
                    self.tokenizer.get_location(),
                    token
                )
            })?;
        }

        loop {
            self.tokenizer.skip_delimiters(WHITESPACE);
            if self.tokenizer.is_eol() || self.tokenizer.peek_char() == '#' {
                break;
            }

            let keyword_token = self.tokenizer.next_token(WHITESPACE);
            if keyword_token == "flat" {
                self.tokenizer.skip_delimiters(WHITESPACE);
                let flat_token = self.tokenizer.next_token(WHITESPACE);
                let flat_override_raw = parse_int(&flat_token).ok_or_else(|| {
                    format!(
                        "{}: Expected flat value, got '{}'.",
                        self.tokenizer.get_location(),
                        flat_token
                    )
                })?;
                flat_override = Some(flat_override_raw);
            } else {
                return Err(format!(
                    "{}: Expected keyword 'flat', got '{}'.",
                    self.tokenizer.get_location(),
                    keyword_token
                ));
            }
        }

        let axis_info: KeyLayoutAxisInfo = KeyLayoutAxisInfo {
            mode: axis_mode,
            axis,
            high_axis,
            split_value,
            flat_override,
        };

        self.map.axes.insert(scan_code as u32, axis_info);
        Ok(())
    }
}

/// Parse an integer from a string (supports decimal, hex with 0x prefix, and octal with 0 prefix).
fn parse_int(s: &str) -> Option<i32> {
    if s.is_empty() {
        return None;
    }

    // Handle hex (0x prefix) and octal (0 prefix)
    if s.starts_with("0x") || s.starts_with("0X") {
        i32::from_str_radix(&s[2..], 16).ok()
    } else if s.starts_with('0') && s.len() > 1 {
        i32::from_str_radix(&s[1..], 8).ok()
    } else {
        s.parse::<i32>().ok()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_int() {
        assert_eq!(parse_int("123"), Some(123));
        assert_eq!(parse_int("-999"), Some(-999));
        assert_eq!(parse_int("0x1a"), Some(26));
        assert_eq!(parse_int("0XFF"), Some(255));
        assert_eq!(parse_int("077"), Some(63)); // octal
        assert_eq!(parse_int(""), None);
        assert_eq!(parse_int("abc"), None);
    }

    #[test]
    fn test_load_from_contents_simple() {
        let contents = "key 1 ESCAPE\nkey 2 1\n";
        let map = KeyLayoutMap::load_from_contents(contents).unwrap();

        let key = map.map_key(1).unwrap();
        assert_eq!(key.key_code, 111); // ESCAPE
        assert_eq!(key.flags, 0);

        let key = map.map_key(2).unwrap();
        assert_eq!(key.key_code, 8); // KEYCODE_1
        assert_eq!(key.flags, 0);
    }

    #[test]
    fn test_load_from_contents_with_flags() {
        let contents = "key 465 ESCAPE FUNCTION\n";
        let map = KeyLayoutMap::load_from_contents(contents).unwrap();

        let key = map.map_key(465).unwrap();
        assert_eq!(key.key_code, 111); // ESCAPE
        assert_eq!(key.flags, 0x00000004); // FUNCTION flag
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
    fn test_find_scan_codes_for_key() {
        let contents = "key 1 ESCAPE\nkey 465 ESCAPE FUNCTION\n";
        let map = KeyLayoutMap::load_from_contents(contents).unwrap();

        let scan_codes = map.find_scan_codes_for_key(111); // ESCAPE
        assert_eq!(scan_codes, vec![1]); // Only scan code 1, not 465 (has FUNCTION flag)
    }
}
