//! Input event label lookup functionality.
//!
//! Provides lookup functions for converting between string labels and numeric values
//! for Android key codes, flags, and axes.
//!
//! Uses Android NDK constants (AKEYCODE_*, AMOTION_EVENT_AXIS_*) from the generated bindings.
//! POLICY_FLAG_* constants are defined locally as they come from the local Input.h header.

use crate::bindings;
use std::collections::HashMap;
use std::sync::OnceLock;

// Policy flags from android/input/Input.h
// These are defined as an enum in the C++ header, so we define them here
const POLICY_FLAG_WAKE: u32 = 0x00000001;
const POLICY_FLAG_VIRTUAL: u32 = 0x00000002;
const POLICY_FLAG_FUNCTION: u32 = 0x00000004;
const POLICY_FLAG_GESTURE: u32 = 0x00000008;
const POLICY_FLAG_FALLBACK_USAGE_MAPPING: u32 = 0x00000010;

// Motion event axes from android/input.h
// These are defined in the NDK header but not accessible via bindgen in C mode
// Values from frameworks/base/core/jni/android_view_MotionEvent.cpp
const AMOTION_EVENT_AXIS_X: i32 = 0;
const AMOTION_EVENT_AXIS_Y: i32 = 1;
const AMOTION_EVENT_AXIS_PRESSURE: i32 = 2;
const AMOTION_EVENT_AXIS_SIZE: i32 = 3;
const AMOTION_EVENT_AXIS_TOUCH_MAJOR: i32 = 4;
const AMOTION_EVENT_AXIS_TOUCH_MINOR: i32 = 5;
const AMOTION_EVENT_AXIS_TOOL_MAJOR: i32 = 6;
const AMOTION_EVENT_AXIS_TOOL_MINOR: i32 = 7;
const AMOTION_EVENT_AXIS_ORIENTATION: i32 = 8;
const AMOTION_EVENT_AXIS_VSCROLL: i32 = 9;
const AMOTION_EVENT_AXIS_HSCROLL: i32 = 10;
const AMOTION_EVENT_AXIS_Z: i32 = 11;
const AMOTION_EVENT_AXIS_RX: i32 = 12;
const AMOTION_EVENT_AXIS_RY: i32 = 13;
const AMOTION_EVENT_AXIS_RZ: i32 = 14;
const AMOTION_EVENT_AXIS_HAT_X: i32 = 15;
const AMOTION_EVENT_AXIS_HAT_Y: i32 = 16;
const AMOTION_EVENT_AXIS_LTRIGGER: i32 = 17;
const AMOTION_EVENT_AXIS_RTRIGGER: i32 = 18;
const AMOTION_EVENT_AXIS_THROTTLE: i32 = 19;
const AMOTION_EVENT_AXIS_RUDDER: i32 = 20;
const AMOTION_EVENT_AXIS_WHEEL: i32 = 21;
const AMOTION_EVENT_AXIS_GAS: i32 = 22;
const AMOTION_EVENT_AXIS_BRAKE: i32 = 23;
const AMOTION_EVENT_AXIS_DISTANCE: i32 = 24;
const AMOTION_EVENT_AXIS_TILT: i32 = 25;
const AMOTION_EVENT_AXIS_SCROLL: i32 = 26;
const AMOTION_EVENT_AXIS_RELATIVE_X: i32 = 27;
const AMOTION_EVENT_AXIS_GENERIC_1: i32 = 32;
const AMOTION_EVENT_AXIS_GENERIC_2: i32 = 33;
const AMOTION_EVENT_AXIS_GENERIC_3: i32 = 34;
const AMOTION_EVENT_AXIS_GENERIC_4: i32 = 35;
const AMOTION_EVENT_AXIS_GENERIC_5: i32 = 36;
const AMOTION_EVENT_AXIS_GENERIC_6: i32 = 37;
const AMOTION_EVENT_AXIS_GENERIC_7: i32 = 38;
const AMOTION_EVENT_AXIS_GENERIC_8: i32 = 39;
const AMOTION_EVENT_AXIS_GENERIC_9: i32 = 40;
const AMOTION_EVENT_AXIS_GENERIC_10: i32 = 41;
const AMOTION_EVENT_AXIS_GENERIC_11: i32 = 42;
const AMOTION_EVENT_AXIS_GENERIC_12: i32 = 43;
const AMOTION_EVENT_AXIS_GENERIC_13: i32 = 44;
const AMOTION_EVENT_AXIS_GENERIC_14: i32 = 45;
const AMOTION_EVENT_AXIS_GENERIC_15: i32 = 46;
const AMOTION_EVENT_AXIS_GENERIC_16: i32 = 47;
const AMOTION_EVENT_AXIS_GESTURE_X_OFFSET: i32 = 48;
const AMOTION_EVENT_AXIS_GESTURE_Y_OFFSET: i32 = 49;
const AMOTION_EVENT_AXIS_GESTURE_SCROLL_X_DISTANCE: i32 = 50;
const AMOTION_EVENT_AXIS_GESTURE_SCROLL_Y_DISTANCE: i32 = 51;
const AMOTION_EVENT_AXIS_GESTURE_PINCH_SCALE_FACTOR: i32 = 52;
const AMOTION_EVENT_AXIS_GESTURE_SWIPE_FINGER_COUNT: i32 = 53;

// Macro to define keycode entry using AKEYCODE_* constant
macro_rules! define_keycode {
    ($name:ident) => {
        (stringify!($name).to_string(), bindings::$name as i32)
    };
}

// Macro to define flag entry using POLICY_FLAG_* constant
macro_rules! define_flag {
    ($name:ident) => {
        (stringify!($name).to_string(), $name as u32)
    };
}

// Macro to define axis entry using AMOTION_EVENT_AXIS_* constant
macro_rules! define_axis {
    ($name:ident) => {
        (stringify!($name).to_string(), $name as i32)
    };
}

// Build the keycodes map
fn build_keycodes_map() -> HashMap<String, i32> {
    let mut map = HashMap::new();
    
    // Helper macro to insert keycode
    macro_rules! insert_keycode {
        ($name:ident) => {
            let (name, value) = define_keycode!($name);
            map.insert(name, value);
        };
    }
    
    // Add numeric keycodes (0-9, 11, 12) - these map to AKEYCODE_0, AKEYCODE_1, etc.
    insert_keycode!(AKEYCODE_0);
    insert_keycode!(AKEYCODE_1);
    insert_keycode!(AKEYCODE_2);
    insert_keycode!(AKEYCODE_3);
    insert_keycode!(AKEYCODE_4);
    insert_keycode!(AKEYCODE_5);
    insert_keycode!(AKEYCODE_6);
    insert_keycode!(AKEYCODE_7);
    insert_keycode!(AKEYCODE_8);
    insert_keycode!(AKEYCODE_9);
    
    // Add all other keycodes
    insert_keycode!(AKEYCODE_UNKNOWN);
    insert_keycode!(AKEYCODE_SOFT_LEFT);
    insert_keycode!(AKEYCODE_SOFT_RIGHT);
    insert_keycode!(AKEYCODE_HOME);
    insert_keycode!(AKEYCODE_BACK);
    insert_keycode!(AKEYCODE_CALL);
    insert_keycode!(AKEYCODE_ENDCALL);
    insert_keycode!(AKEYCODE_STAR);
    insert_keycode!(AKEYCODE_POUND);
    insert_keycode!(AKEYCODE_DPAD_UP);
    insert_keycode!(AKEYCODE_DPAD_DOWN);
    insert_keycode!(AKEYCODE_DPAD_LEFT);
    insert_keycode!(AKEYCODE_DPAD_RIGHT);
    insert_keycode!(AKEYCODE_DPAD_CENTER);
    insert_keycode!(AKEYCODE_VOLUME_UP);
    insert_keycode!(AKEYCODE_VOLUME_DOWN);
    insert_keycode!(AKEYCODE_POWER);
    insert_keycode!(AKEYCODE_CAMERA);
    insert_keycode!(AKEYCODE_CLEAR);
    insert_keycode!(AKEYCODE_A);
    insert_keycode!(AKEYCODE_B);
    insert_keycode!(AKEYCODE_C);
    insert_keycode!(AKEYCODE_D);
    insert_keycode!(AKEYCODE_E);
    insert_keycode!(AKEYCODE_F);
    insert_keycode!(AKEYCODE_G);
    insert_keycode!(AKEYCODE_H);
    insert_keycode!(AKEYCODE_I);
    insert_keycode!(AKEYCODE_J);
    insert_keycode!(AKEYCODE_K);
    insert_keycode!(AKEYCODE_L);
    insert_keycode!(AKEYCODE_M);
    insert_keycode!(AKEYCODE_N);
    insert_keycode!(AKEYCODE_O);
    insert_keycode!(AKEYCODE_P);
    insert_keycode!(AKEYCODE_Q);
    insert_keycode!(AKEYCODE_R);
    insert_keycode!(AKEYCODE_S);
    insert_keycode!(AKEYCODE_T);
    insert_keycode!(AKEYCODE_U);
    insert_keycode!(AKEYCODE_V);
    insert_keycode!(AKEYCODE_W);
    insert_keycode!(AKEYCODE_X);
    insert_keycode!(AKEYCODE_Y);
    insert_keycode!(AKEYCODE_Z);
    insert_keycode!(AKEYCODE_COMMA);
    insert_keycode!(AKEYCODE_PERIOD);
    insert_keycode!(AKEYCODE_ALT_LEFT);
    insert_keycode!(AKEYCODE_ALT_RIGHT);
    insert_keycode!(AKEYCODE_SHIFT_LEFT);
    insert_keycode!(AKEYCODE_SHIFT_RIGHT);
    insert_keycode!(AKEYCODE_TAB);
    insert_keycode!(AKEYCODE_SPACE);
    insert_keycode!(AKEYCODE_SYM);
    insert_keycode!(AKEYCODE_EXPLORER);
    insert_keycode!(AKEYCODE_ENVELOPE);
    insert_keycode!(AKEYCODE_ENTER);
    insert_keycode!(AKEYCODE_DEL);
    insert_keycode!(AKEYCODE_GRAVE);
    insert_keycode!(AKEYCODE_MINUS);
    insert_keycode!(AKEYCODE_EQUALS);
    insert_keycode!(AKEYCODE_LEFT_BRACKET);
    insert_keycode!(AKEYCODE_RIGHT_BRACKET);
    insert_keycode!(AKEYCODE_BACKSLASH);
    insert_keycode!(AKEYCODE_SEMICOLON);
    insert_keycode!(AKEYCODE_APOSTROPHE);
    insert_keycode!(AKEYCODE_SLASH);
    insert_keycode!(AKEYCODE_AT);
    insert_keycode!(AKEYCODE_NUM);
    insert_keycode!(AKEYCODE_HEADSETHOOK);
    insert_keycode!(AKEYCODE_FOCUS);
    insert_keycode!(AKEYCODE_PLUS);
    insert_keycode!(AKEYCODE_MENU);
    insert_keycode!(AKEYCODE_NOTIFICATION);
    insert_keycode!(AKEYCODE_SEARCH);
    insert_keycode!(AKEYCODE_MEDIA_PLAY_PAUSE);
    insert_keycode!(AKEYCODE_MEDIA_STOP);
    insert_keycode!(AKEYCODE_MEDIA_NEXT);
    insert_keycode!(AKEYCODE_MEDIA_PREVIOUS);
    insert_keycode!(AKEYCODE_MEDIA_REWIND);
    insert_keycode!(AKEYCODE_MEDIA_FAST_FORWARD);
    insert_keycode!(AKEYCODE_MUTE);
    insert_keycode!(AKEYCODE_PAGE_UP);
    insert_keycode!(AKEYCODE_PAGE_DOWN);
    insert_keycode!(AKEYCODE_PICTSYMBOLS);
    insert_keycode!(AKEYCODE_SWITCH_CHARSET);
    insert_keycode!(AKEYCODE_BUTTON_A);
    insert_keycode!(AKEYCODE_BUTTON_B);
    insert_keycode!(AKEYCODE_BUTTON_C);
    insert_keycode!(AKEYCODE_BUTTON_X);
    insert_keycode!(AKEYCODE_BUTTON_Y);
    insert_keycode!(AKEYCODE_BUTTON_Z);
    insert_keycode!(AKEYCODE_BUTTON_L1);
    insert_keycode!(AKEYCODE_BUTTON_R1);
    insert_keycode!(AKEYCODE_BUTTON_L2);
    insert_keycode!(AKEYCODE_BUTTON_R2);
    insert_keycode!(AKEYCODE_BUTTON_THUMBL);
    insert_keycode!(AKEYCODE_BUTTON_THUMBR);
    insert_keycode!(AKEYCODE_BUTTON_START);
    insert_keycode!(AKEYCODE_BUTTON_SELECT);
    insert_keycode!(AKEYCODE_BUTTON_MODE);
    insert_keycode!(AKEYCODE_ESCAPE);
    insert_keycode!(AKEYCODE_FORWARD_DEL);
    insert_keycode!(AKEYCODE_CTRL_LEFT);
    insert_keycode!(AKEYCODE_CTRL_RIGHT);
    insert_keycode!(AKEYCODE_CAPS_LOCK);
    insert_keycode!(AKEYCODE_SCROLL_LOCK);
    insert_keycode!(AKEYCODE_META_LEFT);
    insert_keycode!(AKEYCODE_META_RIGHT);
    insert_keycode!(AKEYCODE_FUNCTION);
    insert_keycode!(AKEYCODE_SYSRQ);
    insert_keycode!(AKEYCODE_BREAK);
    insert_keycode!(AKEYCODE_MOVE_HOME);
    insert_keycode!(AKEYCODE_MOVE_END);
    insert_keycode!(AKEYCODE_INSERT);
    insert_keycode!(AKEYCODE_FORWARD);
    insert_keycode!(AKEYCODE_MEDIA_PLAY);
    insert_keycode!(AKEYCODE_MEDIA_PAUSE);
    insert_keycode!(AKEYCODE_MEDIA_CLOSE);
    insert_keycode!(AKEYCODE_MEDIA_EJECT);
    insert_keycode!(AKEYCODE_MEDIA_RECORD);
    insert_keycode!(AKEYCODE_F1);
    insert_keycode!(AKEYCODE_F2);
    insert_keycode!(AKEYCODE_F3);
    insert_keycode!(AKEYCODE_F4);
    insert_keycode!(AKEYCODE_F5);
    insert_keycode!(AKEYCODE_F6);
    insert_keycode!(AKEYCODE_F7);
    insert_keycode!(AKEYCODE_F8);
    insert_keycode!(AKEYCODE_F9);
    insert_keycode!(AKEYCODE_F10);
    insert_keycode!(AKEYCODE_F11);
    insert_keycode!(AKEYCODE_F12);
    insert_keycode!(AKEYCODE_NUM_LOCK);
    insert_keycode!(AKEYCODE_NUMPAD_0);
    insert_keycode!(AKEYCODE_NUMPAD_1);
    insert_keycode!(AKEYCODE_NUMPAD_2);
    insert_keycode!(AKEYCODE_NUMPAD_3);
    insert_keycode!(AKEYCODE_NUMPAD_4);
    insert_keycode!(AKEYCODE_NUMPAD_5);
    insert_keycode!(AKEYCODE_NUMPAD_6);
    insert_keycode!(AKEYCODE_NUMPAD_7);
    insert_keycode!(AKEYCODE_NUMPAD_8);
    insert_keycode!(AKEYCODE_NUMPAD_9);
    insert_keycode!(AKEYCODE_NUMPAD_DIVIDE);
    insert_keycode!(AKEYCODE_NUMPAD_MULTIPLY);
    insert_keycode!(AKEYCODE_NUMPAD_SUBTRACT);
    insert_keycode!(AKEYCODE_NUMPAD_ADD);
    insert_keycode!(AKEYCODE_NUMPAD_DOT);
    insert_keycode!(AKEYCODE_NUMPAD_COMMA);
    insert_keycode!(AKEYCODE_NUMPAD_ENTER);
    insert_keycode!(AKEYCODE_NUMPAD_EQUALS);
    insert_keycode!(AKEYCODE_NUMPAD_LEFT_PAREN);
    insert_keycode!(AKEYCODE_NUMPAD_RIGHT_PAREN);
    insert_keycode!(AKEYCODE_VOLUME_MUTE);
    insert_keycode!(AKEYCODE_INFO);
    insert_keycode!(AKEYCODE_CHANNEL_UP);
    insert_keycode!(AKEYCODE_CHANNEL_DOWN);
    insert_keycode!(AKEYCODE_ZOOM_IN);
    insert_keycode!(AKEYCODE_ZOOM_OUT);
    insert_keycode!(AKEYCODE_TV);
    insert_keycode!(AKEYCODE_WINDOW);
    insert_keycode!(AKEYCODE_GUIDE);
    insert_keycode!(AKEYCODE_DVR);
    insert_keycode!(AKEYCODE_BOOKMARK);
    insert_keycode!(AKEYCODE_CAPTIONS);
    insert_keycode!(AKEYCODE_SETTINGS);
    insert_keycode!(AKEYCODE_TV_POWER);
    insert_keycode!(AKEYCODE_TV_INPUT);
    insert_keycode!(AKEYCODE_STB_POWER);
    insert_keycode!(AKEYCODE_STB_INPUT);
    insert_keycode!(AKEYCODE_AVR_POWER);
    insert_keycode!(AKEYCODE_AVR_INPUT);
    insert_keycode!(AKEYCODE_PROG_RED);
    insert_keycode!(AKEYCODE_PROG_GREEN);
    insert_keycode!(AKEYCODE_PROG_YELLOW);
    insert_keycode!(AKEYCODE_PROG_BLUE);
    insert_keycode!(AKEYCODE_APP_SWITCH);
    insert_keycode!(AKEYCODE_BUTTON_1);
    insert_keycode!(AKEYCODE_BUTTON_2);
    insert_keycode!(AKEYCODE_BUTTON_3);
    insert_keycode!(AKEYCODE_BUTTON_4);
    insert_keycode!(AKEYCODE_BUTTON_5);
    insert_keycode!(AKEYCODE_BUTTON_6);
    insert_keycode!(AKEYCODE_BUTTON_7);
    insert_keycode!(AKEYCODE_BUTTON_8);
    insert_keycode!(AKEYCODE_BUTTON_9);
    insert_keycode!(AKEYCODE_BUTTON_10);
    insert_keycode!(AKEYCODE_BUTTON_11);
    insert_keycode!(AKEYCODE_BUTTON_12);
    insert_keycode!(AKEYCODE_BUTTON_13);
    insert_keycode!(AKEYCODE_BUTTON_14);
    insert_keycode!(AKEYCODE_BUTTON_15);
    insert_keycode!(AKEYCODE_BUTTON_16);
    insert_keycode!(AKEYCODE_LANGUAGE_SWITCH);
    insert_keycode!(AKEYCODE_MANNER_MODE);
    insert_keycode!(AKEYCODE_3D_MODE);
    insert_keycode!(AKEYCODE_CONTACTS);
    insert_keycode!(AKEYCODE_CALENDAR);
    insert_keycode!(AKEYCODE_MUSIC);
    insert_keycode!(AKEYCODE_CALCULATOR);
    insert_keycode!(AKEYCODE_ZENKAKU_HANKAKU);
    insert_keycode!(AKEYCODE_EISU);
    insert_keycode!(AKEYCODE_MUHENKAN);
    insert_keycode!(AKEYCODE_HENKAN);
    insert_keycode!(AKEYCODE_KATAKANA_HIRAGANA);
    insert_keycode!(AKEYCODE_YEN);
    insert_keycode!(AKEYCODE_RO);
    insert_keycode!(AKEYCODE_KANA);
    insert_keycode!(AKEYCODE_ASSIST);
    insert_keycode!(AKEYCODE_BRIGHTNESS_DOWN);
    insert_keycode!(AKEYCODE_BRIGHTNESS_UP);
    insert_keycode!(AKEYCODE_MEDIA_AUDIO_TRACK);
    insert_keycode!(AKEYCODE_SLEEP);
    insert_keycode!(AKEYCODE_WAKEUP);
    insert_keycode!(AKEYCODE_PAIRING);
    insert_keycode!(AKEYCODE_MEDIA_TOP_MENU);
    insert_keycode!(AKEYCODE_LAST_CHANNEL);
    insert_keycode!(AKEYCODE_TV_DATA_SERVICE);
    insert_keycode!(AKEYCODE_VOICE_ASSIST);
    insert_keycode!(AKEYCODE_TV_RADIO_SERVICE);
    insert_keycode!(AKEYCODE_TV_TELETEXT);
    insert_keycode!(AKEYCODE_TV_NUMBER_ENTRY);
    insert_keycode!(AKEYCODE_TV_TERRESTRIAL_ANALOG);
    insert_keycode!(AKEYCODE_TV_TERRESTRIAL_DIGITAL);
    insert_keycode!(AKEYCODE_TV_SATELLITE);
    insert_keycode!(AKEYCODE_TV_SATELLITE_BS);
    insert_keycode!(AKEYCODE_TV_SATELLITE_CS);
    insert_keycode!(AKEYCODE_TV_SATELLITE_SERVICE);
    insert_keycode!(AKEYCODE_TV_NETWORK);
    insert_keycode!(AKEYCODE_TV_ANTENNA_CABLE);
    insert_keycode!(AKEYCODE_TV_INPUT_HDMI_1);
    insert_keycode!(AKEYCODE_TV_INPUT_HDMI_2);
    insert_keycode!(AKEYCODE_TV_INPUT_HDMI_3);
    insert_keycode!(AKEYCODE_TV_INPUT_HDMI_4);
    insert_keycode!(AKEYCODE_TV_INPUT_COMPOSITE_1);
    insert_keycode!(AKEYCODE_TV_INPUT_COMPOSITE_2);
    insert_keycode!(AKEYCODE_TV_INPUT_COMPONENT_1);
    insert_keycode!(AKEYCODE_TV_INPUT_COMPONENT_2);
    insert_keycode!(AKEYCODE_TV_INPUT_VGA_1);
    insert_keycode!(AKEYCODE_TV_AUDIO_DESCRIPTION);
    insert_keycode!(AKEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP);
    insert_keycode!(AKEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN);
    insert_keycode!(AKEYCODE_TV_ZOOM_MODE);
    insert_keycode!(AKEYCODE_TV_CONTENTS_MENU);
    insert_keycode!(AKEYCODE_TV_MEDIA_CONTEXT_MENU);
    insert_keycode!(AKEYCODE_TV_TIMER_PROGRAMMING);
    insert_keycode!(AKEYCODE_HELP);
    insert_keycode!(AKEYCODE_NAVIGATE_PREVIOUS);
    insert_keycode!(AKEYCODE_NAVIGATE_NEXT);
    insert_keycode!(AKEYCODE_NAVIGATE_IN);
    insert_keycode!(AKEYCODE_NAVIGATE_OUT);
    insert_keycode!(AKEYCODE_STEM_PRIMARY);
    insert_keycode!(AKEYCODE_STEM_1);
    insert_keycode!(AKEYCODE_STEM_2);
    insert_keycode!(AKEYCODE_STEM_3);
    insert_keycode!(AKEYCODE_DPAD_UP_LEFT);
    insert_keycode!(AKEYCODE_DPAD_DOWN_LEFT);
    insert_keycode!(AKEYCODE_DPAD_UP_RIGHT);
    insert_keycode!(AKEYCODE_DPAD_DOWN_RIGHT);
    insert_keycode!(AKEYCODE_MEDIA_SKIP_FORWARD);
    insert_keycode!(AKEYCODE_MEDIA_SKIP_BACKWARD);
    insert_keycode!(AKEYCODE_MEDIA_STEP_FORWARD);
    insert_keycode!(AKEYCODE_MEDIA_STEP_BACKWARD);
    insert_keycode!(AKEYCODE_SOFT_SLEEP);
    insert_keycode!(AKEYCODE_CUT);
    insert_keycode!(AKEYCODE_COPY);
    insert_keycode!(AKEYCODE_PASTE);
    insert_keycode!(AKEYCODE_SYSTEM_NAVIGATION_UP);
    insert_keycode!(AKEYCODE_SYSTEM_NAVIGATION_DOWN);
    insert_keycode!(AKEYCODE_SYSTEM_NAVIGATION_LEFT);
    insert_keycode!(AKEYCODE_SYSTEM_NAVIGATION_RIGHT);
    insert_keycode!(AKEYCODE_ALL_APPS);
    insert_keycode!(AKEYCODE_REFRESH);
    insert_keycode!(AKEYCODE_THUMBS_UP);
    insert_keycode!(AKEYCODE_THUMBS_DOWN);
    insert_keycode!(AKEYCODE_PROFILE_SWITCH);
    insert_keycode!(AKEYCODE_VIDEO_APP_1);
    insert_keycode!(AKEYCODE_VIDEO_APP_2);
    insert_keycode!(AKEYCODE_VIDEO_APP_3);
    insert_keycode!(AKEYCODE_VIDEO_APP_4);
    insert_keycode!(AKEYCODE_VIDEO_APP_5);
    insert_keycode!(AKEYCODE_VIDEO_APP_6);
    insert_keycode!(AKEYCODE_VIDEO_APP_7);
    insert_keycode!(AKEYCODE_VIDEO_APP_8);
    insert_keycode!(AKEYCODE_FEATURED_APP_1);
    insert_keycode!(AKEYCODE_FEATURED_APP_2);
    insert_keycode!(AKEYCODE_FEATURED_APP_3);
    insert_keycode!(AKEYCODE_FEATURED_APP_4);
    insert_keycode!(AKEYCODE_DEMO_APP_1);
    insert_keycode!(AKEYCODE_DEMO_APP_2);
    insert_keycode!(AKEYCODE_DEMO_APP_3);
    insert_keycode!(AKEYCODE_DEMO_APP_4);
    insert_keycode!(AKEYCODE_KEYBOARD_BACKLIGHT_DOWN);
    insert_keycode!(AKEYCODE_KEYBOARD_BACKLIGHT_UP);
    insert_keycode!(AKEYCODE_KEYBOARD_BACKLIGHT_TOGGLE);
    insert_keycode!(AKEYCODE_STYLUS_BUTTON_PRIMARY);
    insert_keycode!(AKEYCODE_STYLUS_BUTTON_SECONDARY);
    insert_keycode!(AKEYCODE_STYLUS_BUTTON_TERTIARY);
    insert_keycode!(AKEYCODE_STYLUS_BUTTON_TAIL);
    insert_keycode!(AKEYCODE_RECENT_APPS);
    insert_keycode!(AKEYCODE_MACRO_1);
    insert_keycode!(AKEYCODE_MACRO_2);
    insert_keycode!(AKEYCODE_MACRO_3);
    insert_keycode!(AKEYCODE_MACRO_4);
    
    map
}

// Build the flags map
fn build_flags_map() -> HashMap<String, u32> {
    let mut map = HashMap::new();
    macro_rules! insert_flag {
        ($name:ident) => {
            let (name, value) = define_flag!($name);
            map.insert(name, value);
        };
    }
    insert_flag!(POLICY_FLAG_VIRTUAL);
    insert_flag!(POLICY_FLAG_FUNCTION);
    insert_flag!(POLICY_FLAG_GESTURE);
    insert_flag!(POLICY_FLAG_WAKE);
    insert_flag!(POLICY_FLAG_FALLBACK_USAGE_MAPPING);
    map
}

// Build the axes map
fn build_axes_map() -> HashMap<String, i32> {
    let mut map = HashMap::new();
    macro_rules! insert_axis {
        ($name:ident) => {
            let (name, value) = define_axis!($name);
            map.insert(name, value);
        };
    }
    insert_axis!(AMOTION_EVENT_AXIS_X);
    insert_axis!(AMOTION_EVENT_AXIS_Y);
    insert_axis!(AMOTION_EVENT_AXIS_PRESSURE);
    insert_axis!(AMOTION_EVENT_AXIS_SIZE);
    insert_axis!(AMOTION_EVENT_AXIS_TOUCH_MAJOR);
    insert_axis!(AMOTION_EVENT_AXIS_TOUCH_MINOR);
    insert_axis!(AMOTION_EVENT_AXIS_TOOL_MAJOR);
    insert_axis!(AMOTION_EVENT_AXIS_TOOL_MINOR);
    insert_axis!(AMOTION_EVENT_AXIS_ORIENTATION);
    insert_axis!(AMOTION_EVENT_AXIS_VSCROLL);
    insert_axis!(AMOTION_EVENT_AXIS_HSCROLL);
    insert_axis!(AMOTION_EVENT_AXIS_Z);
    insert_axis!(AMOTION_EVENT_AXIS_RX);
    insert_axis!(AMOTION_EVENT_AXIS_RY);
    insert_axis!(AMOTION_EVENT_AXIS_RZ);
    insert_axis!(AMOTION_EVENT_AXIS_HAT_X);
    insert_axis!(AMOTION_EVENT_AXIS_HAT_Y);
    insert_axis!(AMOTION_EVENT_AXIS_LTRIGGER);
    insert_axis!(AMOTION_EVENT_AXIS_RTRIGGER);
    insert_axis!(AMOTION_EVENT_AXIS_THROTTLE);
    insert_axis!(AMOTION_EVENT_AXIS_RUDDER);
    insert_axis!(AMOTION_EVENT_AXIS_WHEEL);
    insert_axis!(AMOTION_EVENT_AXIS_GAS);
    insert_axis!(AMOTION_EVENT_AXIS_BRAKE);
    insert_axis!(AMOTION_EVENT_AXIS_DISTANCE);
    insert_axis!(AMOTION_EVENT_AXIS_TILT);
    insert_axis!(AMOTION_EVENT_AXIS_SCROLL);
    insert_axis!(AMOTION_EVENT_AXIS_RELATIVE_X);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_1);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_2);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_3);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_4);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_5);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_6);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_7);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_8);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_9);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_10);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_11);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_12);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_13);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_14);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_15);
    insert_axis!(AMOTION_EVENT_AXIS_GENERIC_16);
    insert_axis!(AMOTION_EVENT_AXIS_GESTURE_X_OFFSET);
    insert_axis!(AMOTION_EVENT_AXIS_GESTURE_Y_OFFSET);
    insert_axis!(AMOTION_EVENT_AXIS_GESTURE_SCROLL_X_DISTANCE);
    insert_axis!(AMOTION_EVENT_AXIS_GESTURE_SCROLL_Y_DISTANCE);
    insert_axis!(AMOTION_EVENT_AXIS_GESTURE_PINCH_SCALE_FACTOR);
    insert_axis!(AMOTION_EVENT_AXIS_GESTURE_SWIPE_FINGER_COUNT);
    map
}

// Static lookup tables (lazily initialized)
static KEYCODES: OnceLock<HashMap<String, i32>> = OnceLock::new();
static FLAGS: OnceLock<HashMap<String, u32>> = OnceLock::new();
static AXES: OnceLock<HashMap<String, i32>> = OnceLock::new();

fn get_keycodes() -> &'static HashMap<String, i32> {
    KEYCODES.get_or_init(build_keycodes_map)
}

fn get_flags() -> &'static HashMap<String, u32> {
    FLAGS.get_or_init(build_flags_map)
}

fn get_axes() -> &'static HashMap<String, i32> {
    AXES.get_or_init(build_axes_map)
}

/// Look up a key code by its label.
pub fn get_key_code_by_label(label: &str) -> Option<i32> {
    get_keycodes().get(label).copied()
}

/// Look up a key flag by its label.
pub fn get_key_flag_by_label(label: &str) -> Option<u32> {
    get_flags().get(label).copied()
}

/// Look up an axis by its label.
pub fn get_axis_by_label(label: &str) -> Option<i32> {
    get_axes().get(label).copied()
}
