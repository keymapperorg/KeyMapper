/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "InputEventLabels.h"

#include <linux/input-event-codes.h>
#include <linux/input.h>
#include <strings.h>

#define DEFINE_KEYCODE(key) { #key, AKEYCODE_##key }
#define DEFINE_AXIS(axis) { #axis, AMOTION_EVENT_AXIS_##axis }
#define DEFINE_LED(led) { #led, ALED_##led }
#define DEFINE_FLAG(flag) { #flag, POLICY_FLAG_##flag }

namespace android {

// clang-format off

// NOTE: If you add a new keycode here you must also add it to several other files.
//       Refer to frameworks/base/core/java/android/view/KeyEvent.java for the full list.
#define KEYCODES_SEQUENCE \
    DEFINE_KEYCODE(UNKNOWN), \
    DEFINE_KEYCODE(SOFT_LEFT), \
    DEFINE_KEYCODE(SOFT_RIGHT), \
    DEFINE_KEYCODE(HOME), \
    DEFINE_KEYCODE(BACK), \
    DEFINE_KEYCODE(CALL), \
    DEFINE_KEYCODE(ENDCALL), \
    DEFINE_KEYCODE(0), \
    DEFINE_KEYCODE(1), \
    DEFINE_KEYCODE(2), \
    DEFINE_KEYCODE(3), \
    DEFINE_KEYCODE(4), \
    DEFINE_KEYCODE(5), \
    DEFINE_KEYCODE(6), \
    DEFINE_KEYCODE(7), \
    DEFINE_KEYCODE(8), \
    DEFINE_KEYCODE(9), \
    DEFINE_KEYCODE(STAR), \
    DEFINE_KEYCODE(POUND), \
    DEFINE_KEYCODE(DPAD_UP), \
    DEFINE_KEYCODE(DPAD_DOWN), \
    DEFINE_KEYCODE(DPAD_LEFT), \
    DEFINE_KEYCODE(DPAD_RIGHT), \
    DEFINE_KEYCODE(DPAD_CENTER), \
    DEFINE_KEYCODE(VOLUME_UP), \
    DEFINE_KEYCODE(VOLUME_DOWN), \
    DEFINE_KEYCODE(POWER), \
    DEFINE_KEYCODE(CAMERA), \
    DEFINE_KEYCODE(CLEAR), \
    DEFINE_KEYCODE(A), \
    DEFINE_KEYCODE(B), \
    DEFINE_KEYCODE(C), \
    DEFINE_KEYCODE(D), \
    DEFINE_KEYCODE(E), \
    DEFINE_KEYCODE(F), \
    DEFINE_KEYCODE(G), \
    DEFINE_KEYCODE(H), \
    DEFINE_KEYCODE(I), \
    DEFINE_KEYCODE(J), \
    DEFINE_KEYCODE(K), \
    DEFINE_KEYCODE(L), \
    DEFINE_KEYCODE(M), \
    DEFINE_KEYCODE(N), \
    DEFINE_KEYCODE(O), \
    DEFINE_KEYCODE(P), \
    DEFINE_KEYCODE(Q), \
    DEFINE_KEYCODE(R), \
    DEFINE_KEYCODE(S), \
    DEFINE_KEYCODE(T), \
    DEFINE_KEYCODE(U), \
    DEFINE_KEYCODE(V), \
    DEFINE_KEYCODE(W), \
    DEFINE_KEYCODE(X), \
    DEFINE_KEYCODE(Y), \
    DEFINE_KEYCODE(Z), \
    DEFINE_KEYCODE(COMMA), \
    DEFINE_KEYCODE(PERIOD), \
    DEFINE_KEYCODE(ALT_LEFT), \
    DEFINE_KEYCODE(ALT_RIGHT), \
    DEFINE_KEYCODE(SHIFT_LEFT), \
    DEFINE_KEYCODE(SHIFT_RIGHT), \
    DEFINE_KEYCODE(TAB), \
    DEFINE_KEYCODE(SPACE), \
    DEFINE_KEYCODE(SYM), \
    DEFINE_KEYCODE(EXPLORER), \
    DEFINE_KEYCODE(ENVELOPE), \
    DEFINE_KEYCODE(ENTER), \
    DEFINE_KEYCODE(DEL), \
    DEFINE_KEYCODE(GRAVE), \
    DEFINE_KEYCODE(MINUS), \
    DEFINE_KEYCODE(EQUALS), \
    DEFINE_KEYCODE(LEFT_BRACKET), \
    DEFINE_KEYCODE(RIGHT_BRACKET), \
    DEFINE_KEYCODE(BACKSLASH), \
    DEFINE_KEYCODE(SEMICOLON), \
    DEFINE_KEYCODE(APOSTROPHE), \
    DEFINE_KEYCODE(SLASH), \
    DEFINE_KEYCODE(AT), \
    DEFINE_KEYCODE(NUM), \
    DEFINE_KEYCODE(HEADSETHOOK), \
    DEFINE_KEYCODE(FOCUS), \
    DEFINE_KEYCODE(PLUS), \
    DEFINE_KEYCODE(MENU), \
    DEFINE_KEYCODE(NOTIFICATION), \
    DEFINE_KEYCODE(SEARCH), \
    DEFINE_KEYCODE(MEDIA_PLAY_PAUSE), \
    DEFINE_KEYCODE(MEDIA_STOP), \
    DEFINE_KEYCODE(MEDIA_NEXT), \
    DEFINE_KEYCODE(MEDIA_PREVIOUS), \
    DEFINE_KEYCODE(MEDIA_REWIND), \
    DEFINE_KEYCODE(MEDIA_FAST_FORWARD), \
    DEFINE_KEYCODE(MUTE), \
    DEFINE_KEYCODE(PAGE_UP), \
    DEFINE_KEYCODE(PAGE_DOWN), \
    DEFINE_KEYCODE(PICTSYMBOLS), \
    DEFINE_KEYCODE(SWITCH_CHARSET), \
    DEFINE_KEYCODE(BUTTON_A), \
    DEFINE_KEYCODE(BUTTON_B), \
    DEFINE_KEYCODE(BUTTON_C), \
    DEFINE_KEYCODE(BUTTON_X), \
    DEFINE_KEYCODE(BUTTON_Y), \
    DEFINE_KEYCODE(BUTTON_Z), \
    DEFINE_KEYCODE(BUTTON_L1), \
    DEFINE_KEYCODE(BUTTON_R1), \
    DEFINE_KEYCODE(BUTTON_L2), \
    DEFINE_KEYCODE(BUTTON_R2), \
    DEFINE_KEYCODE(BUTTON_THUMBL), \
    DEFINE_KEYCODE(BUTTON_THUMBR), \
    DEFINE_KEYCODE(BUTTON_START), \
    DEFINE_KEYCODE(BUTTON_SELECT), \
    DEFINE_KEYCODE(BUTTON_MODE), \
    DEFINE_KEYCODE(ESCAPE), \
    DEFINE_KEYCODE(FORWARD_DEL), \
    DEFINE_KEYCODE(CTRL_LEFT), \
    DEFINE_KEYCODE(CTRL_RIGHT), \
    DEFINE_KEYCODE(CAPS_LOCK), \
    DEFINE_KEYCODE(SCROLL_LOCK), \
    DEFINE_KEYCODE(META_LEFT), \
    DEFINE_KEYCODE(META_RIGHT), \
    DEFINE_KEYCODE(FUNCTION), \
    DEFINE_KEYCODE(SYSRQ), \
    DEFINE_KEYCODE(BREAK), \
    DEFINE_KEYCODE(MOVE_HOME), \
    DEFINE_KEYCODE(MOVE_END), \
    DEFINE_KEYCODE(INSERT), \
    DEFINE_KEYCODE(FORWARD), \
    DEFINE_KEYCODE(MEDIA_PLAY), \
    DEFINE_KEYCODE(MEDIA_PAUSE), \
    DEFINE_KEYCODE(MEDIA_CLOSE), \
    DEFINE_KEYCODE(MEDIA_EJECT), \
    DEFINE_KEYCODE(MEDIA_RECORD), \
    DEFINE_KEYCODE(F1), \
    DEFINE_KEYCODE(F2), \
    DEFINE_KEYCODE(F3), \
    DEFINE_KEYCODE(F4), \
    DEFINE_KEYCODE(F5), \
    DEFINE_KEYCODE(F6), \
    DEFINE_KEYCODE(F7), \
    DEFINE_KEYCODE(F8), \
    DEFINE_KEYCODE(F9), \
    DEFINE_KEYCODE(F10), \
    DEFINE_KEYCODE(F11), \
    DEFINE_KEYCODE(F12), \
    DEFINE_KEYCODE(NUM_LOCK), \
    DEFINE_KEYCODE(NUMPAD_0), \
    DEFINE_KEYCODE(NUMPAD_1), \
    DEFINE_KEYCODE(NUMPAD_2), \
    DEFINE_KEYCODE(NUMPAD_3), \
    DEFINE_KEYCODE(NUMPAD_4), \
    DEFINE_KEYCODE(NUMPAD_5), \
    DEFINE_KEYCODE(NUMPAD_6), \
    DEFINE_KEYCODE(NUMPAD_7), \
    DEFINE_KEYCODE(NUMPAD_8), \
    DEFINE_KEYCODE(NUMPAD_9), \
    DEFINE_KEYCODE(NUMPAD_DIVIDE), \
    DEFINE_KEYCODE(NUMPAD_MULTIPLY), \
    DEFINE_KEYCODE(NUMPAD_SUBTRACT), \
    DEFINE_KEYCODE(NUMPAD_ADD), \
    DEFINE_KEYCODE(NUMPAD_DOT), \
    DEFINE_KEYCODE(NUMPAD_COMMA), \
    DEFINE_KEYCODE(NUMPAD_ENTER), \
    DEFINE_KEYCODE(NUMPAD_EQUALS), \
    DEFINE_KEYCODE(NUMPAD_LEFT_PAREN), \
    DEFINE_KEYCODE(NUMPAD_RIGHT_PAREN), \
    DEFINE_KEYCODE(VOLUME_MUTE), \
    DEFINE_KEYCODE(INFO), \
    DEFINE_KEYCODE(CHANNEL_UP), \
    DEFINE_KEYCODE(CHANNEL_DOWN), \
    DEFINE_KEYCODE(ZOOM_IN), \
    DEFINE_KEYCODE(ZOOM_OUT), \
    DEFINE_KEYCODE(TV), \
    DEFINE_KEYCODE(WINDOW), \
    DEFINE_KEYCODE(GUIDE), \
    DEFINE_KEYCODE(DVR), \
    DEFINE_KEYCODE(BOOKMARK), \
    DEFINE_KEYCODE(CAPTIONS), \
    DEFINE_KEYCODE(SETTINGS), \
    DEFINE_KEYCODE(TV_POWER), \
    DEFINE_KEYCODE(TV_INPUT), \
    DEFINE_KEYCODE(STB_POWER), \
    DEFINE_KEYCODE(STB_INPUT), \
    DEFINE_KEYCODE(AVR_POWER), \
    DEFINE_KEYCODE(AVR_INPUT), \
    DEFINE_KEYCODE(PROG_RED), \
    DEFINE_KEYCODE(PROG_GREEN), \
    DEFINE_KEYCODE(PROG_YELLOW), \
    DEFINE_KEYCODE(PROG_BLUE), \
    DEFINE_KEYCODE(APP_SWITCH), \
    DEFINE_KEYCODE(BUTTON_1), \
    DEFINE_KEYCODE(BUTTON_2), \
    DEFINE_KEYCODE(BUTTON_3), \
    DEFINE_KEYCODE(BUTTON_4), \
    DEFINE_KEYCODE(BUTTON_5), \
    DEFINE_KEYCODE(BUTTON_6), \
    DEFINE_KEYCODE(BUTTON_7), \
    DEFINE_KEYCODE(BUTTON_8), \
    DEFINE_KEYCODE(BUTTON_9), \
    DEFINE_KEYCODE(BUTTON_10), \
    DEFINE_KEYCODE(BUTTON_11), \
    DEFINE_KEYCODE(BUTTON_12), \
    DEFINE_KEYCODE(BUTTON_13), \
    DEFINE_KEYCODE(BUTTON_14), \
    DEFINE_KEYCODE(BUTTON_15), \
    DEFINE_KEYCODE(BUTTON_16), \
    DEFINE_KEYCODE(LANGUAGE_SWITCH), \
    DEFINE_KEYCODE(MANNER_MODE), \
    DEFINE_KEYCODE(3D_MODE), \
    DEFINE_KEYCODE(CONTACTS), \
    DEFINE_KEYCODE(CALENDAR), \
    DEFINE_KEYCODE(MUSIC), \
    DEFINE_KEYCODE(CALCULATOR), \
    DEFINE_KEYCODE(ZENKAKU_HANKAKU), \
    DEFINE_KEYCODE(EISU), \
    DEFINE_KEYCODE(MUHENKAN), \
    DEFINE_KEYCODE(HENKAN), \
    DEFINE_KEYCODE(KATAKANA_HIRAGANA), \
    DEFINE_KEYCODE(YEN), \
    DEFINE_KEYCODE(RO), \
    DEFINE_KEYCODE(KANA), \
    DEFINE_KEYCODE(ASSIST), \
    DEFINE_KEYCODE(BRIGHTNESS_DOWN), \
    DEFINE_KEYCODE(BRIGHTNESS_UP), \
    DEFINE_KEYCODE(MEDIA_AUDIO_TRACK), \
    DEFINE_KEYCODE(SLEEP), \
    DEFINE_KEYCODE(WAKEUP), \
    DEFINE_KEYCODE(PAIRING), \
    DEFINE_KEYCODE(MEDIA_TOP_MENU), \
    DEFINE_KEYCODE(11), \
    DEFINE_KEYCODE(12), \
    DEFINE_KEYCODE(LAST_CHANNEL), \
    DEFINE_KEYCODE(TV_DATA_SERVICE), \
    DEFINE_KEYCODE(VOICE_ASSIST), \
    DEFINE_KEYCODE(TV_RADIO_SERVICE), \
    DEFINE_KEYCODE(TV_TELETEXT), \
    DEFINE_KEYCODE(TV_NUMBER_ENTRY), \
    DEFINE_KEYCODE(TV_TERRESTRIAL_ANALOG), \
    DEFINE_KEYCODE(TV_TERRESTRIAL_DIGITAL), \
    DEFINE_KEYCODE(TV_SATELLITE), \
    DEFINE_KEYCODE(TV_SATELLITE_BS), \
    DEFINE_KEYCODE(TV_SATELLITE_CS), \
    DEFINE_KEYCODE(TV_SATELLITE_SERVICE), \
    DEFINE_KEYCODE(TV_NETWORK), \
    DEFINE_KEYCODE(TV_ANTENNA_CABLE), \
    DEFINE_KEYCODE(TV_INPUT_HDMI_1), \
    DEFINE_KEYCODE(TV_INPUT_HDMI_2), \
    DEFINE_KEYCODE(TV_INPUT_HDMI_3), \
    DEFINE_KEYCODE(TV_INPUT_HDMI_4), \
    DEFINE_KEYCODE(TV_INPUT_COMPOSITE_1), \
    DEFINE_KEYCODE(TV_INPUT_COMPOSITE_2), \
    DEFINE_KEYCODE(TV_INPUT_COMPONENT_1), \
    DEFINE_KEYCODE(TV_INPUT_COMPONENT_2), \
    DEFINE_KEYCODE(TV_INPUT_VGA_1), \
    DEFINE_KEYCODE(TV_AUDIO_DESCRIPTION), \
    DEFINE_KEYCODE(TV_AUDIO_DESCRIPTION_MIX_UP), \
    DEFINE_KEYCODE(TV_AUDIO_DESCRIPTION_MIX_DOWN), \
    DEFINE_KEYCODE(TV_ZOOM_MODE), \
    DEFINE_KEYCODE(TV_CONTENTS_MENU), \
    DEFINE_KEYCODE(TV_MEDIA_CONTEXT_MENU), \
    DEFINE_KEYCODE(TV_TIMER_PROGRAMMING), \
    DEFINE_KEYCODE(HELP), \
    DEFINE_KEYCODE(NAVIGATE_PREVIOUS), \
    DEFINE_KEYCODE(NAVIGATE_NEXT), \
    DEFINE_KEYCODE(NAVIGATE_IN), \
    DEFINE_KEYCODE(NAVIGATE_OUT), \
    DEFINE_KEYCODE(STEM_PRIMARY), \
    DEFINE_KEYCODE(STEM_1), \
    DEFINE_KEYCODE(STEM_2), \
    DEFINE_KEYCODE(STEM_3), \
    DEFINE_KEYCODE(DPAD_UP_LEFT), \
    DEFINE_KEYCODE(DPAD_DOWN_LEFT), \
    DEFINE_KEYCODE(DPAD_UP_RIGHT), \
    DEFINE_KEYCODE(DPAD_DOWN_RIGHT), \
    DEFINE_KEYCODE(MEDIA_SKIP_FORWARD), \
    DEFINE_KEYCODE(MEDIA_SKIP_BACKWARD), \
    DEFINE_KEYCODE(MEDIA_STEP_FORWARD), \
    DEFINE_KEYCODE(MEDIA_STEP_BACKWARD), \
    DEFINE_KEYCODE(SOFT_SLEEP), \
    DEFINE_KEYCODE(CUT), \
    DEFINE_KEYCODE(COPY), \
    DEFINE_KEYCODE(PASTE), \
    DEFINE_KEYCODE(SYSTEM_NAVIGATION_UP), \
    DEFINE_KEYCODE(SYSTEM_NAVIGATION_DOWN), \
    DEFINE_KEYCODE(SYSTEM_NAVIGATION_LEFT), \
    DEFINE_KEYCODE(SYSTEM_NAVIGATION_RIGHT), \
    DEFINE_KEYCODE(ALL_APPS), \
    DEFINE_KEYCODE(REFRESH), \
    DEFINE_KEYCODE(THUMBS_UP), \
    DEFINE_KEYCODE(THUMBS_DOWN), \
    DEFINE_KEYCODE(PROFILE_SWITCH), \
    DEFINE_KEYCODE(VIDEO_APP_1), \
    DEFINE_KEYCODE(VIDEO_APP_2), \
    DEFINE_KEYCODE(VIDEO_APP_3), \
    DEFINE_KEYCODE(VIDEO_APP_4), \
    DEFINE_KEYCODE(VIDEO_APP_5), \
    DEFINE_KEYCODE(VIDEO_APP_6), \
    DEFINE_KEYCODE(VIDEO_APP_7), \
    DEFINE_KEYCODE(VIDEO_APP_8), \
    DEFINE_KEYCODE(FEATURED_APP_1), \
    DEFINE_KEYCODE(FEATURED_APP_2), \
    DEFINE_KEYCODE(FEATURED_APP_3), \
    DEFINE_KEYCODE(FEATURED_APP_4), \
    DEFINE_KEYCODE(DEMO_APP_1), \
    DEFINE_KEYCODE(DEMO_APP_2), \
    DEFINE_KEYCODE(DEMO_APP_3), \
    DEFINE_KEYCODE(DEMO_APP_4), \
    DEFINE_KEYCODE(KEYBOARD_BACKLIGHT_DOWN), \
    DEFINE_KEYCODE(KEYBOARD_BACKLIGHT_UP), \
    DEFINE_KEYCODE(KEYBOARD_BACKLIGHT_TOGGLE), \
    DEFINE_KEYCODE(STYLUS_BUTTON_PRIMARY), \
    DEFINE_KEYCODE(STYLUS_BUTTON_SECONDARY), \
    DEFINE_KEYCODE(STYLUS_BUTTON_TERTIARY), \
    DEFINE_KEYCODE(STYLUS_BUTTON_TAIL), \
    DEFINE_KEYCODE(RECENT_APPS), \
    DEFINE_KEYCODE(MACRO_1), \
    DEFINE_KEYCODE(MACRO_2), \
    DEFINE_KEYCODE(MACRO_3), \
    DEFINE_KEYCODE(MACRO_4), \
//    DEFINE_KEYCODE(EMOJI_PICKER), \
//    DEFINE_KEYCODE(SCREENSHOT), \
//    DEFINE_KEYCODE(DICTATE), \
//    DEFINE_KEYCODE(NEW), \
//    DEFINE_KEYCODE(CLOSE), \
//    DEFINE_KEYCODE(DO_NOT_DISTURB), \
//    DEFINE_KEYCODE(PRINT), \
//    DEFINE_KEYCODE(LOCK), \
//    DEFINE_KEYCODE(FULLSCREEN), \
//    DEFINE_KEYCODE(F13), \
//    DEFINE_KEYCODE(F14), \
//    DEFINE_KEYCODE(F15), \
//    DEFINE_KEYCODE(F16), \
//    DEFINE_KEYCODE(F17), \
//    DEFINE_KEYCODE(F18), \
//    DEFINE_KEYCODE(F19),\
//    DEFINE_KEYCODE(F20), \
//    DEFINE_KEYCODE(F21), \
//    DEFINE_KEYCODE(F22), \
//    DEFINE_KEYCODE(F23), \
//    DEFINE_KEYCODE(F24)

// NOTE: If you add a new axis here you must also add it to several other files.
//       Refer to frameworks/base/core/java/android/view/MotionEvent.java for the full list.
#define AXES_SEQUENCE \
    DEFINE_AXIS(X), \
    DEFINE_AXIS(Y), \
    DEFINE_AXIS(PRESSURE), \
    DEFINE_AXIS(SIZE), \
    DEFINE_AXIS(TOUCH_MAJOR), \
    DEFINE_AXIS(TOUCH_MINOR), \
    DEFINE_AXIS(TOOL_MAJOR), \
    DEFINE_AXIS(TOOL_MINOR), \
    DEFINE_AXIS(ORIENTATION), \
    DEFINE_AXIS(VSCROLL), \
    DEFINE_AXIS(HSCROLL), \
    DEFINE_AXIS(Z), \
    DEFINE_AXIS(RX), \
    DEFINE_AXIS(RY), \
    DEFINE_AXIS(RZ), \
    DEFINE_AXIS(HAT_X), \
    DEFINE_AXIS(HAT_Y), \
    DEFINE_AXIS(LTRIGGER), \
    DEFINE_AXIS(RTRIGGER), \
    DEFINE_AXIS(THROTTLE), \
    DEFINE_AXIS(RUDDER), \
    DEFINE_AXIS(WHEEL), \
    DEFINE_AXIS(GAS), \
    DEFINE_AXIS(BRAKE), \
    DEFINE_AXIS(DISTANCE), \
    DEFINE_AXIS(TILT), \
    DEFINE_AXIS(SCROLL), \
    DEFINE_AXIS(RELATIVE_X), \
    DEFINE_AXIS(RELATIVE_Y), \
    {"RESERVED_29", 29}, \
    {"RESERVED_30", 30}, \
    {"RESERVED_31", 31}, \
    DEFINE_AXIS(GENERIC_1), \
    DEFINE_AXIS(GENERIC_2), \
    DEFINE_AXIS(GENERIC_3), \
    DEFINE_AXIS(GENERIC_4), \
    DEFINE_AXIS(GENERIC_5), \
    DEFINE_AXIS(GENERIC_6), \
    DEFINE_AXIS(GENERIC_7), \
    DEFINE_AXIS(GENERIC_8), \
    DEFINE_AXIS(GENERIC_9), \
    DEFINE_AXIS(GENERIC_10), \
    DEFINE_AXIS(GENERIC_11), \
    DEFINE_AXIS(GENERIC_12), \
    DEFINE_AXIS(GENERIC_13), \
    DEFINE_AXIS(GENERIC_14), \
    DEFINE_AXIS(GENERIC_15), \
    DEFINE_AXIS(GENERIC_16), \
    DEFINE_AXIS(GESTURE_X_OFFSET), \
    DEFINE_AXIS(GESTURE_Y_OFFSET), \
    DEFINE_AXIS(GESTURE_SCROLL_X_DISTANCE), \
    DEFINE_AXIS(GESTURE_SCROLL_Y_DISTANCE), \
    DEFINE_AXIS(GESTURE_PINCH_SCALE_FACTOR), \
    DEFINE_AXIS(GESTURE_SWIPE_FINGER_COUNT)

// clang-format on

// --- InputEventLookup ---

    InputEventLookup::InputEventLookup()
            : KEYCODES({KEYCODES_SEQUENCE}),
              KEY_NAMES({KEYCODES_SEQUENCE}),
              AXES({AXES_SEQUENCE}),
              AXES_NAMES({AXES_SEQUENCE}) {}

    std::optional<int> InputEventLookup::lookupValueByLabel(
            const std::unordered_map<std::string, int> &map, const char *literal) {
        std::string str(literal);
        auto it = map.find(str);
        return it != map.end() ? std::make_optional(it->second) : std::nullopt;
    }

    const char *InputEventLookup::lookupLabelByValue(const std::vector<InputEventLabel> &vec,
                                                     int value) {
        if (static_cast<size_t>(value) < vec.size()) {
            return vec[value].literal;
        }
        return nullptr;
    }

    std::optional<int> InputEventLookup::getKeyCodeByLabel(const char *label) {
        const auto &self = get();
        return self.lookupValueByLabel(self.KEYCODES, label);
    }

    const char *InputEventLookup::getLabelByKeyCode(int32_t keyCode) {
        const auto &self = get();
        if (keyCode >= 0 && static_cast<size_t>(keyCode) < self.KEYCODES.size()) {
            return get().lookupLabelByValue(self.KEY_NAMES, keyCode);
        }
        return nullptr;
    }

    std::optional<int> InputEventLookup::getKeyFlagByLabel(const char *label) {
        const auto &self = get();
        return lookupValueByLabel(self.FLAGS, label);
    }

    std::optional<int> InputEventLookup::getAxisByLabel(const char *label) {
        const auto &self = get();
        return lookupValueByLabel(self.AXES, label);
    }

    const char *InputEventLookup::getAxisLabel(int32_t axisId) {
        const auto &self = get();
        return lookupLabelByValue(self.AXES_NAMES, axisId);
    }

    std::optional<int> InputEventLookup::getLedByLabel(const char *label) {
        const auto &self = get();
        return lookupValueByLabel(self.LEDS, label);
    }

    namespace {

        struct label {
            const char *name;
            int value;
        };

#define LABEL(constant) \
    { #constant, constant }
#define LABEL_END \
    { nullptr, -1 }

// Inserted from the file: out/soong/.intermediates/system/core/toolbox/toolbox_input_labels/gen/input.h-labels.h
        static struct label ev_key_value_labels[] = {
                {"UP", 0},
                {"DOWN", 1},
                {"REPEAT", 2},
                LABEL_END,
        };


        static struct label input_prop_labels[] = {
                LABEL(INPUT_PROP_POINTER),
                LABEL(INPUT_PROP_DIRECT),
                LABEL(INPUT_PROP_BUTTONPAD),
                LABEL(INPUT_PROP_SEMI_MT),
                LABEL(INPUT_PROP_TOPBUTTONPAD),
                LABEL(INPUT_PROP_POINTING_STICK),
                LABEL(INPUT_PROP_ACCELEROMETER),
                LABEL(INPUT_PROP_MAX),
                LABEL_END,
        };
        static struct label ev_labels[] = {
                LABEL(EV_VERSION),
                LABEL(EV_SYN),
                LABEL(EV_KEY),
                LABEL(EV_REL),
                LABEL(EV_ABS),
                LABEL(EV_MSC),
                LABEL(EV_SW),
                LABEL(EV_LED),
                LABEL(EV_SND),
                LABEL(EV_REP),
                LABEL(EV_FF),
                LABEL(EV_PWR),
                LABEL(EV_FF_STATUS),
                LABEL(EV_MAX),
                LABEL_END,
        };
        static struct label syn_labels[] = {
                LABEL(SYN_REPORT),
                LABEL(SYN_CONFIG),
                LABEL(SYN_MT_REPORT),
                LABEL(SYN_DROPPED),
                LABEL(SYN_MAX),
                LABEL_END,
        };
        static struct label key_labels[] = {
                LABEL(KEY_RESERVED),
                LABEL(KEY_ESC),
                LABEL(KEY_1),
                LABEL(KEY_2),
                LABEL(KEY_3),
                LABEL(KEY_4),
                LABEL(KEY_5),
                LABEL(KEY_6),
                LABEL(KEY_7),
                LABEL(KEY_8),
                LABEL(KEY_9),
                LABEL(KEY_0),
                LABEL(KEY_MINUS),
                LABEL(KEY_EQUAL),
                LABEL(KEY_BACKSPACE),
                LABEL(KEY_TAB),
                LABEL(KEY_Q),
                LABEL(KEY_W),
                LABEL(KEY_E),
                LABEL(KEY_R),
                LABEL(KEY_T),
                LABEL(KEY_Y),
                LABEL(KEY_U),
                LABEL(KEY_I),
                LABEL(KEY_O),
                LABEL(KEY_P),
                LABEL(KEY_LEFTBRACE),
                LABEL(KEY_RIGHTBRACE),
                LABEL(KEY_ENTER),
                LABEL(KEY_LEFTCTRL),
                LABEL(KEY_A),
                LABEL(KEY_S),
                LABEL(KEY_D),
                LABEL(KEY_F),
                LABEL(KEY_G),
                LABEL(KEY_H),
                LABEL(KEY_J),
                LABEL(KEY_K),
                LABEL(KEY_L),
                LABEL(KEY_SEMICOLON),
                LABEL(KEY_APOSTROPHE),
                LABEL(KEY_GRAVE),
                LABEL(KEY_LEFTSHIFT),
                LABEL(KEY_BACKSLASH),
                LABEL(KEY_Z),
                LABEL(KEY_X),
                LABEL(KEY_C),
                LABEL(KEY_V),
                LABEL(KEY_B),
                LABEL(KEY_N),
                LABEL(KEY_M),
                LABEL(KEY_COMMA),
                LABEL(KEY_DOT),
                LABEL(KEY_SLASH),
                LABEL(KEY_RIGHTSHIFT),
                LABEL(KEY_KPASTERISK),
                LABEL(KEY_LEFTALT),
                LABEL(KEY_SPACE),
                LABEL(KEY_CAPSLOCK),
                LABEL(KEY_F1),
                LABEL(KEY_F2),
                LABEL(KEY_F3),
                LABEL(KEY_F4),
                LABEL(KEY_F5),
                LABEL(KEY_F6),
                LABEL(KEY_F7),
                LABEL(KEY_F8),
                LABEL(KEY_F9),
                LABEL(KEY_F10),
                LABEL(KEY_NUMLOCK),
                LABEL(KEY_SCROLLLOCK),
                LABEL(KEY_KP7),
                LABEL(KEY_KP8),
                LABEL(KEY_KP9),
                LABEL(KEY_KPMINUS),
                LABEL(KEY_KP4),
                LABEL(KEY_KP5),
                LABEL(KEY_KP6),
                LABEL(KEY_KPPLUS),
                LABEL(KEY_KP1),
                LABEL(KEY_KP2),
                LABEL(KEY_KP3),
                LABEL(KEY_KP0),
                LABEL(KEY_KPDOT),
                LABEL(KEY_ZENKAKUHANKAKU),
                LABEL(KEY_102ND),
                LABEL(KEY_F11),
                LABEL(KEY_F12),
                LABEL(KEY_RO),
                LABEL(KEY_KATAKANA),
                LABEL(KEY_HIRAGANA),
                LABEL(KEY_HENKAN),
                LABEL(KEY_KATAKANAHIRAGANA),
                LABEL(KEY_MUHENKAN),
                LABEL(KEY_KPJPCOMMA),
                LABEL(KEY_KPENTER),
                LABEL(KEY_RIGHTCTRL),
                LABEL(KEY_KPSLASH),
                LABEL(KEY_SYSRQ),
                LABEL(KEY_RIGHTALT),
                LABEL(KEY_LINEFEED),
                LABEL(KEY_HOME),
                LABEL(KEY_UP),
                LABEL(KEY_PAGEUP),
                LABEL(KEY_LEFT),
                LABEL(KEY_RIGHT),
                LABEL(KEY_END),
                LABEL(KEY_DOWN),
                LABEL(KEY_PAGEDOWN),
                LABEL(KEY_INSERT),
                LABEL(KEY_DELETE),
                LABEL(KEY_MACRO),
                LABEL(KEY_MUTE),
                LABEL(KEY_VOLUMEDOWN),
                LABEL(KEY_VOLUMEUP),
                LABEL(KEY_POWER),
                LABEL(KEY_KPEQUAL),
                LABEL(KEY_KPPLUSMINUS),
                LABEL(KEY_PAUSE),
                LABEL(KEY_SCALE),
                LABEL(KEY_KPCOMMA),
                LABEL(KEY_HANGEUL),
                LABEL(KEY_HANJA),
                LABEL(KEY_YEN),
                LABEL(KEY_LEFTMETA),
                LABEL(KEY_RIGHTMETA),
                LABEL(KEY_COMPOSE),
                LABEL(KEY_STOP),
                LABEL(KEY_AGAIN),
                LABEL(KEY_PROPS),
                LABEL(KEY_UNDO),
                LABEL(KEY_FRONT),
                LABEL(KEY_COPY),
                LABEL(KEY_OPEN),
                LABEL(KEY_PASTE),
                LABEL(KEY_FIND),
                LABEL(KEY_CUT),
                LABEL(KEY_HELP),
                LABEL(KEY_MENU),
                LABEL(KEY_CALC),
                LABEL(KEY_SETUP),
                LABEL(KEY_SLEEP),
                LABEL(KEY_WAKEUP),
                LABEL(KEY_FILE),
                LABEL(KEY_SENDFILE),
                LABEL(KEY_DELETEFILE),
                LABEL(KEY_XFER),
                LABEL(KEY_PROG1),
                LABEL(KEY_PROG2),
                LABEL(KEY_WWW),
                LABEL(KEY_MSDOS),
                LABEL(KEY_COFFEE),
                LABEL(KEY_ROTATE_DISPLAY),
                LABEL(KEY_CYCLEWINDOWS),
                LABEL(KEY_MAIL),
                LABEL(KEY_BOOKMARKS),
                LABEL(KEY_COMPUTER),
                LABEL(KEY_BACK),
                LABEL(KEY_FORWARD),
                LABEL(KEY_CLOSECD),
                LABEL(KEY_EJECTCD),
                LABEL(KEY_EJECTCLOSECD),
                LABEL(KEY_NEXTSONG),
                LABEL(KEY_PLAYPAUSE),
                LABEL(KEY_PREVIOUSSONG),
                LABEL(KEY_STOPCD),
                LABEL(KEY_RECORD),
                LABEL(KEY_REWIND),
                LABEL(KEY_PHONE),
                LABEL(KEY_ISO),
                LABEL(KEY_CONFIG),
                LABEL(KEY_HOMEPAGE),
                LABEL(KEY_REFRESH),
                LABEL(KEY_EXIT),
                LABEL(KEY_MOVE),
                LABEL(KEY_EDIT),
                LABEL(KEY_SCROLLUP),
                LABEL(KEY_SCROLLDOWN),
                LABEL(KEY_KPLEFTPAREN),
                LABEL(KEY_KPRIGHTPAREN),
                LABEL(KEY_NEW),
                LABEL(KEY_REDO),
                LABEL(KEY_F13),
                LABEL(KEY_F14),
                LABEL(KEY_F15),
                LABEL(KEY_F16),
                LABEL(KEY_F17),
                LABEL(KEY_F18),
                LABEL(KEY_F19),
                LABEL(KEY_F20),
                LABEL(KEY_F21),
                LABEL(KEY_F22),
                LABEL(KEY_F23),
                LABEL(KEY_F24),
                LABEL(KEY_PLAYCD),
                LABEL(KEY_PAUSECD),
                LABEL(KEY_PROG3),
                LABEL(KEY_PROG4),
                LABEL(KEY_ALL_APPLICATIONS),
                LABEL(KEY_SUSPEND),
                LABEL(KEY_CLOSE),
                LABEL(KEY_PLAY),
                LABEL(KEY_FASTFORWARD),
                LABEL(KEY_BASSBOOST),
                LABEL(KEY_PRINT),
                LABEL(KEY_HP),
                LABEL(KEY_CAMERA),
                LABEL(KEY_SOUND),
                LABEL(KEY_QUESTION),
                LABEL(KEY_EMAIL),
                LABEL(KEY_CHAT),
                LABEL(KEY_SEARCH),
                LABEL(KEY_CONNECT),
                LABEL(KEY_FINANCE),
                LABEL(KEY_SPORT),
                LABEL(KEY_SHOP),
                LABEL(KEY_ALTERASE),
                LABEL(KEY_CANCEL),
                LABEL(KEY_BRIGHTNESSDOWN),
                LABEL(KEY_BRIGHTNESSUP),
                LABEL(KEY_MEDIA),
                LABEL(KEY_SWITCHVIDEOMODE),
                LABEL(KEY_KBDILLUMTOGGLE),
                LABEL(KEY_KBDILLUMDOWN),
                LABEL(KEY_KBDILLUMUP),
                LABEL(KEY_SEND),
                LABEL(KEY_REPLY),
                LABEL(KEY_FORWARDMAIL),
                LABEL(KEY_SAVE),
                LABEL(KEY_DOCUMENTS),
                LABEL(KEY_BATTERY),
                LABEL(KEY_BLUETOOTH),
                LABEL(KEY_WLAN),
                LABEL(KEY_UWB),
                LABEL(KEY_UNKNOWN),
                LABEL(KEY_VIDEO_NEXT),
                LABEL(KEY_VIDEO_PREV),
                LABEL(KEY_BRIGHTNESS_CYCLE),
                LABEL(KEY_BRIGHTNESS_AUTO),
                LABEL(KEY_DISPLAY_OFF),
                LABEL(KEY_WWAN),
                LABEL(KEY_RFKILL),
                LABEL(KEY_MICMUTE),
                LABEL(BTN_MISC),
                LABEL(BTN_0),
                LABEL(BTN_1),
                LABEL(BTN_2),
                LABEL(BTN_3),
                LABEL(BTN_4),
                LABEL(BTN_5),
                LABEL(BTN_6),
                LABEL(BTN_7),
                LABEL(BTN_8),
                LABEL(BTN_9),
                LABEL(BTN_MOUSE),
                LABEL(BTN_LEFT),
                LABEL(BTN_RIGHT),
                LABEL(BTN_MIDDLE),
                LABEL(BTN_SIDE),
                LABEL(BTN_EXTRA),
                LABEL(BTN_FORWARD),
                LABEL(BTN_BACK),
                LABEL(BTN_TASK),
                LABEL(BTN_JOYSTICK),
                LABEL(BTN_TRIGGER),
                LABEL(BTN_THUMB),
                LABEL(BTN_THUMB2),
                LABEL(BTN_TOP),
                LABEL(BTN_TOP2),
                LABEL(BTN_PINKIE),
                LABEL(BTN_BASE),
                LABEL(BTN_BASE2),
                LABEL(BTN_BASE3),
                LABEL(BTN_BASE4),
                LABEL(BTN_BASE5),
                LABEL(BTN_BASE6),
                LABEL(BTN_DEAD),
                LABEL(BTN_GAMEPAD),
                LABEL(BTN_SOUTH),
                LABEL(BTN_EAST),
                LABEL(BTN_C),
                LABEL(BTN_NORTH),
                LABEL(BTN_WEST),
                LABEL(BTN_Z),
                LABEL(BTN_TL),
                LABEL(BTN_TR),
                LABEL(BTN_TL2),
                LABEL(BTN_TR2),
                LABEL(BTN_SELECT),
                LABEL(BTN_START),
                LABEL(BTN_MODE),
                LABEL(BTN_THUMBL),
                LABEL(BTN_THUMBR),
                LABEL(BTN_DIGI),
                LABEL(BTN_TOOL_PEN),
                LABEL(BTN_TOOL_RUBBER),
                LABEL(BTN_TOOL_BRUSH),
                LABEL(BTN_TOOL_PENCIL),
                LABEL(BTN_TOOL_AIRBRUSH),
                LABEL(BTN_TOOL_FINGER),
                LABEL(BTN_TOOL_MOUSE),
                LABEL(BTN_TOOL_LENS),
                LABEL(BTN_TOOL_QUINTTAP),
                LABEL(BTN_STYLUS3),
                LABEL(BTN_TOUCH),
                LABEL(BTN_STYLUS),
                LABEL(BTN_STYLUS2),
                LABEL(BTN_TOOL_DOUBLETAP),
                LABEL(BTN_TOOL_TRIPLETAP),
                LABEL(BTN_TOOL_QUADTAP),
                LABEL(BTN_WHEEL),
                LABEL(BTN_GEAR_DOWN),
                LABEL(BTN_GEAR_UP),
                LABEL(KEY_OK),
                LABEL(KEY_SELECT),
                LABEL(KEY_GOTO),
                LABEL(KEY_CLEAR),
                LABEL(KEY_POWER2),
                LABEL(KEY_OPTION),
                LABEL(KEY_INFO),
                LABEL(KEY_TIME),
                LABEL(KEY_VENDOR),
                LABEL(KEY_ARCHIVE),
                LABEL(KEY_PROGRAM),
                LABEL(KEY_CHANNEL),
                LABEL(KEY_FAVORITES),
                LABEL(KEY_EPG),
                LABEL(KEY_PVR),
                LABEL(KEY_MHP),
                LABEL(KEY_LANGUAGE),
                LABEL(KEY_TITLE),
                LABEL(KEY_SUBTITLE),
                LABEL(KEY_ANGLE),
                LABEL(KEY_FULL_SCREEN),
                LABEL(KEY_MODE),
                LABEL(KEY_KEYBOARD),
                LABEL(KEY_ASPECT_RATIO),
                LABEL(KEY_PC),
                LABEL(KEY_TV),
                LABEL(KEY_TV2),
                LABEL(KEY_VCR),
                LABEL(KEY_VCR2),
                LABEL(KEY_SAT),
                LABEL(KEY_SAT2),
                LABEL(KEY_CD),
                LABEL(KEY_TAPE),
                LABEL(KEY_RADIO),
                LABEL(KEY_TUNER),
                LABEL(KEY_PLAYER),
                LABEL(KEY_TEXT),
                LABEL(KEY_DVD),
                LABEL(KEY_AUX),
                LABEL(KEY_MP3),
                LABEL(KEY_AUDIO),
                LABEL(KEY_VIDEO),
                LABEL(KEY_DIRECTORY),
                LABEL(KEY_LIST),
                LABEL(KEY_MEMO),
                LABEL(KEY_CALENDAR),
                LABEL(KEY_RED),
                LABEL(KEY_GREEN),
                LABEL(KEY_YELLOW),
                LABEL(KEY_BLUE),
                LABEL(KEY_CHANNELUP),
                LABEL(KEY_CHANNELDOWN),
                LABEL(KEY_FIRST),
                LABEL(KEY_LAST),
                LABEL(KEY_AB),
                LABEL(KEY_NEXT),
                LABEL(KEY_RESTART),
                LABEL(KEY_SLOW),
                LABEL(KEY_SHUFFLE),
                LABEL(KEY_BREAK),
                LABEL(KEY_PREVIOUS),
                LABEL(KEY_DIGITS),
                LABEL(KEY_TEEN),
                LABEL(KEY_TWEN),
                LABEL(KEY_VIDEOPHONE),
                LABEL(KEY_GAMES),
                LABEL(KEY_ZOOMIN),
                LABEL(KEY_ZOOMOUT),
                LABEL(KEY_ZOOMRESET),
                LABEL(KEY_WORDPROCESSOR),
                LABEL(KEY_EDITOR),
                LABEL(KEY_SPREADSHEET),
                LABEL(KEY_GRAPHICSEDITOR),
                LABEL(KEY_PRESENTATION),
                LABEL(KEY_DATABASE),
                LABEL(KEY_NEWS),
                LABEL(KEY_VOICEMAIL),
                LABEL(KEY_ADDRESSBOOK),
                LABEL(KEY_MESSENGER),
                LABEL(KEY_DISPLAYTOGGLE),
                LABEL(KEY_SPELLCHECK),
                LABEL(KEY_LOGOFF),
                LABEL(KEY_DOLLAR),
                LABEL(KEY_EURO),
                LABEL(KEY_FRAMEBACK),
                LABEL(KEY_FRAMEFORWARD),
                LABEL(KEY_CONTEXT_MENU),
                LABEL(KEY_MEDIA_REPEAT),
                LABEL(KEY_10CHANNELSUP),
                LABEL(KEY_10CHANNELSDOWN),
                LABEL(KEY_IMAGES),
                LABEL(KEY_NOTIFICATION_CENTER),
                LABEL(KEY_PICKUP_PHONE),
                LABEL(KEY_HANGUP_PHONE),
                LABEL(KEY_DEL_EOL),
                LABEL(KEY_DEL_EOS),
                LABEL(KEY_INS_LINE),
                LABEL(KEY_DEL_LINE),
                LABEL(KEY_FN),
                LABEL(KEY_FN_ESC),
                LABEL(KEY_FN_F1),
                LABEL(KEY_FN_F2),
                LABEL(KEY_FN_F3),
                LABEL(KEY_FN_F4),
                LABEL(KEY_FN_F5),
                LABEL(KEY_FN_F6),
                LABEL(KEY_FN_F7),
                LABEL(KEY_FN_F8),
                LABEL(KEY_FN_F9),
                LABEL(KEY_FN_F10),
                LABEL(KEY_FN_F11),
                LABEL(KEY_FN_F12),
                LABEL(KEY_FN_1),
                LABEL(KEY_FN_2),
                LABEL(KEY_FN_D),
                LABEL(KEY_FN_E),
                LABEL(KEY_FN_F),
                LABEL(KEY_FN_S),
                LABEL(KEY_FN_B),
                LABEL(KEY_FN_RIGHT_SHIFT),
                LABEL(KEY_BRL_DOT1),
                LABEL(KEY_BRL_DOT2),
                LABEL(KEY_BRL_DOT3),
                LABEL(KEY_BRL_DOT4),
                LABEL(KEY_BRL_DOT5),
                LABEL(KEY_BRL_DOT6),
                LABEL(KEY_BRL_DOT7),
                LABEL(KEY_BRL_DOT8),
                LABEL(KEY_BRL_DOT9),
                LABEL(KEY_BRL_DOT10),
                LABEL(KEY_NUMERIC_0),
                LABEL(KEY_NUMERIC_1),
                LABEL(KEY_NUMERIC_2),
                LABEL(KEY_NUMERIC_3),
                LABEL(KEY_NUMERIC_4),
                LABEL(KEY_NUMERIC_5),
                LABEL(KEY_NUMERIC_6),
                LABEL(KEY_NUMERIC_7),
                LABEL(KEY_NUMERIC_8),
                LABEL(KEY_NUMERIC_9),
                LABEL(KEY_NUMERIC_STAR),
                LABEL(KEY_NUMERIC_POUND),
                LABEL(KEY_NUMERIC_A),
                LABEL(KEY_NUMERIC_B),
                LABEL(KEY_NUMERIC_C),
                LABEL(KEY_NUMERIC_D),
                LABEL(KEY_CAMERA_FOCUS),
                LABEL(KEY_WPS_BUTTON),
                LABEL(KEY_TOUCHPAD_TOGGLE),
                LABEL(KEY_TOUCHPAD_ON),
                LABEL(KEY_TOUCHPAD_OFF),
                LABEL(KEY_CAMERA_ZOOMIN),
                LABEL(KEY_CAMERA_ZOOMOUT),
                LABEL(KEY_CAMERA_UP),
                LABEL(KEY_CAMERA_DOWN),
                LABEL(KEY_CAMERA_LEFT),
                LABEL(KEY_CAMERA_RIGHT),
                LABEL(KEY_ATTENDANT_ON),
                LABEL(KEY_ATTENDANT_OFF),
                LABEL(KEY_ATTENDANT_TOGGLE),
                LABEL(KEY_LIGHTS_TOGGLE),
                LABEL(BTN_DPAD_UP),
                LABEL(BTN_DPAD_DOWN),
                LABEL(BTN_DPAD_LEFT),
                LABEL(BTN_DPAD_RIGHT),
                LABEL(KEY_ALS_TOGGLE),
                LABEL(KEY_ROTATE_LOCK_TOGGLE),
//                LABEL(KEY_REFRESH_RATE_TOGGLE),
                LABEL(KEY_BUTTONCONFIG),
                LABEL(KEY_TASKMANAGER),
                LABEL(KEY_JOURNAL),
                LABEL(KEY_CONTROLPANEL),
                LABEL(KEY_APPSELECT),
                LABEL(KEY_SCREENSAVER),
                LABEL(KEY_VOICECOMMAND),
                LABEL(KEY_ASSISTANT),
                LABEL(KEY_KBD_LAYOUT_NEXT),
                LABEL(KEY_EMOJI_PICKER),
                LABEL(KEY_DICTATE),
                LABEL(KEY_CAMERA_ACCESS_ENABLE),
                LABEL(KEY_CAMERA_ACCESS_DISABLE),
                LABEL(KEY_CAMERA_ACCESS_TOGGLE),
//                LABEL(KEY_ACCESSIBILITY),
//                LABEL(KEY_DO_NOT_DISTURB),
                LABEL(KEY_BRIGHTNESS_MIN),
                LABEL(KEY_BRIGHTNESS_MAX),
                LABEL(KEY_KBDINPUTASSIST_PREV),
                LABEL(KEY_KBDINPUTASSIST_NEXT),
                LABEL(KEY_KBDINPUTASSIST_PREVGROUP),
                LABEL(KEY_KBDINPUTASSIST_NEXTGROUP),
                LABEL(KEY_KBDINPUTASSIST_ACCEPT),
                LABEL(KEY_KBDINPUTASSIST_CANCEL),
                LABEL(KEY_RIGHT_UP),
                LABEL(KEY_RIGHT_DOWN),
                LABEL(KEY_LEFT_UP),
                LABEL(KEY_LEFT_DOWN),
                LABEL(KEY_ROOT_MENU),
                LABEL(KEY_MEDIA_TOP_MENU),
                LABEL(KEY_NUMERIC_11),
                LABEL(KEY_NUMERIC_12),
                LABEL(KEY_AUDIO_DESC),
                LABEL(KEY_3D_MODE),
                LABEL(KEY_NEXT_FAVORITE),
                LABEL(KEY_STOP_RECORD),
                LABEL(KEY_PAUSE_RECORD),
                LABEL(KEY_VOD),
                LABEL(KEY_UNMUTE),
                LABEL(KEY_FASTREVERSE),
                LABEL(KEY_SLOWREVERSE),
                LABEL(KEY_DATA),
                LABEL(KEY_ONSCREEN_KEYBOARD),
                LABEL(KEY_PRIVACY_SCREEN_TOGGLE),
                LABEL(KEY_SELECTIVE_SCREENSHOT),
                LABEL(KEY_NEXT_ELEMENT),
                LABEL(KEY_PREVIOUS_ELEMENT),
                LABEL(KEY_AUTOPILOT_ENGAGE_TOGGLE),
                LABEL(KEY_MARK_WAYPOINT),
                LABEL(KEY_SOS),
                LABEL(KEY_NAV_CHART),
                LABEL(KEY_FISHING_CHART),
                LABEL(KEY_SINGLE_RANGE_RADAR),
                LABEL(KEY_DUAL_RANGE_RADAR),
                LABEL(KEY_RADAR_OVERLAY),
                LABEL(KEY_TRADITIONAL_SONAR),
                LABEL(KEY_CLEARVU_SONAR),
                LABEL(KEY_SIDEVU_SONAR),
                LABEL(KEY_NAV_INFO),
                LABEL(KEY_BRIGHTNESS_MENU),
                LABEL(KEY_MACRO1),
                LABEL(KEY_MACRO2),
                LABEL(KEY_MACRO3),
                LABEL(KEY_MACRO4),
                LABEL(KEY_MACRO5),
                LABEL(KEY_MACRO6),
                LABEL(KEY_MACRO7),
                LABEL(KEY_MACRO8),
                LABEL(KEY_MACRO9),
                LABEL(KEY_MACRO10),
                LABEL(KEY_MACRO11),
                LABEL(KEY_MACRO12),
                LABEL(KEY_MACRO13),
                LABEL(KEY_MACRO14),
                LABEL(KEY_MACRO15),
                LABEL(KEY_MACRO16),
                LABEL(KEY_MACRO17),
                LABEL(KEY_MACRO18),
                LABEL(KEY_MACRO19),
                LABEL(KEY_MACRO20),
                LABEL(KEY_MACRO21),
                LABEL(KEY_MACRO22),
                LABEL(KEY_MACRO23),
                LABEL(KEY_MACRO24),
                LABEL(KEY_MACRO25),
                LABEL(KEY_MACRO26),
                LABEL(KEY_MACRO27),
                LABEL(KEY_MACRO28),
                LABEL(KEY_MACRO29),
                LABEL(KEY_MACRO30),
                LABEL(KEY_MACRO_RECORD_START),
                LABEL(KEY_MACRO_RECORD_STOP),
                LABEL(KEY_MACRO_PRESET_CYCLE),
                LABEL(KEY_MACRO_PRESET1),
                LABEL(KEY_MACRO_PRESET2),
                LABEL(KEY_MACRO_PRESET3),
                LABEL(KEY_KBD_LCD_MENU1),
                LABEL(KEY_KBD_LCD_MENU2),
                LABEL(KEY_KBD_LCD_MENU3),
                LABEL(KEY_KBD_LCD_MENU4),
                LABEL(KEY_KBD_LCD_MENU5),
                LABEL(BTN_TRIGGER_HAPPY),
                LABEL(BTN_TRIGGER_HAPPY1),
                LABEL(BTN_TRIGGER_HAPPY2),
                LABEL(BTN_TRIGGER_HAPPY3),
                LABEL(BTN_TRIGGER_HAPPY4),
                LABEL(BTN_TRIGGER_HAPPY5),
                LABEL(BTN_TRIGGER_HAPPY6),
                LABEL(BTN_TRIGGER_HAPPY7),
                LABEL(BTN_TRIGGER_HAPPY8),
                LABEL(BTN_TRIGGER_HAPPY9),
                LABEL(BTN_TRIGGER_HAPPY10),
                LABEL(BTN_TRIGGER_HAPPY11),
                LABEL(BTN_TRIGGER_HAPPY12),
                LABEL(BTN_TRIGGER_HAPPY13),
                LABEL(BTN_TRIGGER_HAPPY14),
                LABEL(BTN_TRIGGER_HAPPY15),
                LABEL(BTN_TRIGGER_HAPPY16),
                LABEL(BTN_TRIGGER_HAPPY17),
                LABEL(BTN_TRIGGER_HAPPY18),
                LABEL(BTN_TRIGGER_HAPPY19),
                LABEL(BTN_TRIGGER_HAPPY20),
                LABEL(BTN_TRIGGER_HAPPY21),
                LABEL(BTN_TRIGGER_HAPPY22),
                LABEL(BTN_TRIGGER_HAPPY23),
                LABEL(BTN_TRIGGER_HAPPY24),
                LABEL(BTN_TRIGGER_HAPPY25),
                LABEL(BTN_TRIGGER_HAPPY26),
                LABEL(BTN_TRIGGER_HAPPY27),
                LABEL(BTN_TRIGGER_HAPPY28),
                LABEL(BTN_TRIGGER_HAPPY29),
                LABEL(BTN_TRIGGER_HAPPY30),
                LABEL(BTN_TRIGGER_HAPPY31),
                LABEL(BTN_TRIGGER_HAPPY32),
                LABEL(BTN_TRIGGER_HAPPY33),
                LABEL(BTN_TRIGGER_HAPPY34),
                LABEL(BTN_TRIGGER_HAPPY35),
                LABEL(BTN_TRIGGER_HAPPY36),
                LABEL(BTN_TRIGGER_HAPPY37),
                LABEL(BTN_TRIGGER_HAPPY38),
                LABEL(BTN_TRIGGER_HAPPY39),
                LABEL(BTN_TRIGGER_HAPPY40),
                LABEL(KEY_MAX),
                LABEL_END,
        };
        static struct label rel_labels[] = {
                LABEL(REL_X),
                LABEL(REL_Y),
                LABEL(REL_Z),
                LABEL(REL_RX),
                LABEL(REL_RY),
                LABEL(REL_RZ),
                LABEL(REL_HWHEEL),
                LABEL(REL_DIAL),
                LABEL(REL_WHEEL),
                LABEL(REL_MISC),
                LABEL(REL_RESERVED),
                LABEL(REL_WHEEL_HI_RES),
                LABEL(REL_HWHEEL_HI_RES),
                LABEL(REL_MAX),
                LABEL_END,
        };
        static struct label abs_labels[] = {
                LABEL(ABS_X),
                LABEL(ABS_Y),
                LABEL(ABS_Z),
                LABEL(ABS_RX),
                LABEL(ABS_RY),
                LABEL(ABS_RZ),
                LABEL(ABS_THROTTLE),
                LABEL(ABS_RUDDER),
                LABEL(ABS_WHEEL),
                LABEL(ABS_GAS),
                LABEL(ABS_BRAKE),
                LABEL(ABS_HAT0X),
                LABEL(ABS_HAT0Y),
                LABEL(ABS_HAT1X),
                LABEL(ABS_HAT1Y),
                LABEL(ABS_HAT2X),
                LABEL(ABS_HAT2Y),
                LABEL(ABS_HAT3X),
                LABEL(ABS_HAT3Y),
                LABEL(ABS_PRESSURE),
                LABEL(ABS_DISTANCE),
                LABEL(ABS_TILT_X),
                LABEL(ABS_TILT_Y),
                LABEL(ABS_TOOL_WIDTH),
                LABEL(ABS_VOLUME),
                LABEL(ABS_PROFILE),
                LABEL(ABS_MISC),
                LABEL(ABS_RESERVED),
                LABEL(ABS_MT_SLOT),
                LABEL(ABS_MT_TOUCH_MAJOR),
                LABEL(ABS_MT_TOUCH_MINOR),
                LABEL(ABS_MT_WIDTH_MAJOR),
                LABEL(ABS_MT_WIDTH_MINOR),
                LABEL(ABS_MT_ORIENTATION),
                LABEL(ABS_MT_POSITION_X),
                LABEL(ABS_MT_POSITION_Y),
                LABEL(ABS_MT_TOOL_TYPE),
                LABEL(ABS_MT_BLOB_ID),
                LABEL(ABS_MT_TRACKING_ID),
                LABEL(ABS_MT_PRESSURE),
                LABEL(ABS_MT_DISTANCE),
                LABEL(ABS_MT_TOOL_X),
                LABEL(ABS_MT_TOOL_Y),
                LABEL(ABS_MAX),
                LABEL_END,
        };
        static struct label sw_labels[] = {
                LABEL(SW_LID),
                LABEL(SW_TABLET_MODE),
                LABEL(SW_HEADPHONE_INSERT),
                LABEL(SW_RFKILL_ALL),
                LABEL(SW_MICROPHONE_INSERT),
                LABEL(SW_DOCK),
                LABEL(SW_LINEOUT_INSERT),
                LABEL(SW_JACK_PHYSICAL_INSERT),
                LABEL(SW_VIDEOOUT_INSERT),
                LABEL(SW_CAMERA_LENS_COVER),
                LABEL(SW_KEYPAD_SLIDE),
                LABEL(SW_FRONT_PROXIMITY),
                LABEL(SW_ROTATE_LOCK),
                LABEL(SW_LINEIN_INSERT),
                LABEL(SW_MUTE_DEVICE),
                LABEL(SW_PEN_INSERTED),
                LABEL(SW_MACHINE_COVER),
                LABEL(SW_MAX),
                LABEL_END,
        };
        static struct label msc_labels[] = {
                LABEL(MSC_SERIAL),
                LABEL(MSC_PULSELED),
                LABEL(MSC_GESTURE),
                LABEL(MSC_RAW),
                LABEL(MSC_SCAN),
                LABEL(MSC_TIMESTAMP),
                LABEL(MSC_MAX),
                LABEL_END,
        };
        static struct label led_labels[] = {
                LABEL(LED_NUML),
                LABEL(LED_CAPSL),
                LABEL(LED_SCROLLL),
                LABEL(LED_COMPOSE),
                LABEL(LED_KANA),
                LABEL(LED_SLEEP),
                LABEL(LED_SUSPEND),
                LABEL(LED_MUTE),
                LABEL(LED_MISC),
                LABEL(LED_MAIL),
                LABEL(LED_CHARGING),
                LABEL(LED_MAX),
                LABEL_END,
        };
        static struct label rep_labels[] = {
                LABEL(REP_DELAY),
                LABEL(REP_PERIOD),
                LABEL(REP_MAX),
                LABEL_END,
        };
        static struct label snd_labels[] = {
                LABEL(SND_CLICK),
                LABEL(SND_BELL),
                LABEL(SND_TONE),
                LABEL(SND_MAX),
                LABEL_END,
        };
        static struct label mt_tool_labels[] = {
                LABEL(MT_TOOL_FINGER),
                LABEL(MT_TOOL_PEN),
                LABEL(MT_TOOL_PALM),
                LABEL(MT_TOOL_DIAL),
                LABEL(MT_TOOL_MAX),
                LABEL_END,
        };
        static struct label ff_status_labels[] = {
                LABEL(FF_STATUS_STOPPED),
                LABEL(FF_STATUS_PLAYING),
                LABEL(FF_STATUS_MAX),
                LABEL_END,
        };
        static struct label ff_labels[] = {
                LABEL(FF_RUMBLE),
                LABEL(FF_PERIODIC),
                LABEL(FF_CONSTANT),
                LABEL(FF_SPRING),
                LABEL(FF_FRICTION),
                LABEL(FF_DAMPER),
                LABEL(FF_INERTIA),
                LABEL(FF_RAMP),
                LABEL(FF_SQUARE),
                LABEL(FF_TRIANGLE),
                LABEL(FF_SINE),
                LABEL(FF_SAW_UP),
                LABEL(FF_SAW_DOWN),
                LABEL(FF_CUSTOM),
                LABEL(FF_GAIN),
                LABEL(FF_AUTOCENTER),
                LABEL(FF_MAX),
                LABEL_END,
        };

#undef LABEL
#undef LABEL_END

        std::string getLabel(const label *labels, int value) {
            if (labels == nullptr) return std::to_string(value);
            while (labels->name != nullptr && value != labels->value) {
                labels++;
            }
            return labels->name != nullptr ? labels->name : std::to_string(value);
        }

        std::optional<int> getValue(const label *labels, const char *searchLabel) {
            if (labels == nullptr) return {};
            while (labels->name != nullptr && ::strcasecmp(labels->name, searchLabel) != 0) {
                labels++;
            }
            return labels->name != nullptr ? std::make_optional(labels->value) : std::nullopt;
        }

        const label *getCodeLabelsForType(int32_t type) {
            switch (type) {
                case EV_SYN:
                    return syn_labels;
                case EV_KEY:
                    return key_labels;
                case EV_REL:
                    return rel_labels;
                case EV_ABS:
                    return abs_labels;
                case EV_SW:
                    return sw_labels;
                case EV_MSC:
                    return msc_labels;
                case EV_LED:
                    return led_labels;
                case EV_REP:
                    return rep_labels;
                case EV_SND:
                    return snd_labels;
                case EV_FF:
                    return ff_labels;
                case EV_FF_STATUS:
                    return ff_status_labels;
                default:
                    return nullptr;
            }
        }

        const label *getValueLabelsForTypeAndCode(int32_t type, int32_t code) {
            if (type == EV_KEY) {
                return ev_key_value_labels;
            }
            if (type == EV_ABS && code == ABS_MT_TOOL_TYPE) {
                return mt_tool_labels;
            }
            return nullptr;
        }

    } // namespace

    EvdevEventLabel
    InputEventLookup::getLinuxEvdevLabel(int32_t type, int32_t code, int32_t value) {
        return {
                .type = getLabel(ev_labels, type),
                .code = getLabel(getCodeLabelsForType(type), code),
                .value = getLabel(getValueLabelsForTypeAndCode(type, code), value),
        };
    }

    std::optional<int> InputEventLookup::getLinuxEvdevEventTypeByLabel(const char *label) {
        return getValue(ev_labels, label);
    }

    std::optional<int> InputEventLookup::getLinuxEvdevEventCodeByLabel(int32_t type,
                                                                       const char *label) {
        return getValue(getCodeLabelsForType(type), label);
    }

    std::optional<int> InputEventLookup::getLinuxEvdevInputPropByLabel(const char *label) {
        return getValue(input_prop_labels, label);
    }

} // namespace android
