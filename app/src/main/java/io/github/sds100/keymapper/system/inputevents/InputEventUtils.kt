package io.github.sds100.keymapper.system.inputevents

import android.os.Build
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent
import io.github.sds100.keymapper.R
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 17/07/2018.
 */
object InputEventUtils {
    /**
     * Maps keys which aren't single characters like the Control keys to a string representation
     */
    private val NON_CHARACTER_KEY_LABELS: Map<Int, String>
        get() = sequence {
            yieldAll(
                listOf(
                    KeyEvent.KEYCODE_VOLUME_DOWN to "Vol down",
                    KeyEvent.KEYCODE_VOLUME_UP to "Vol up",

                    KeyEvent.KEYCODE_CTRL_LEFT to "Ctrl Left",
                    KeyEvent.KEYCODE_CTRL_RIGHT to "Ctrl Right",

                    KeyEvent.KEYCODE_SHIFT_LEFT to "Shift Left",
                    KeyEvent.KEYCODE_SHIFT_RIGHT to "Shift Right",

                    KeyEvent.KEYCODE_ALT_LEFT to "Alt Left",
                    KeyEvent.KEYCODE_ALT_RIGHT to "Alt Right",

                    KeyEvent.KEYCODE_DPAD_LEFT to "Left",
                    KeyEvent.KEYCODE_DPAD_RIGHT to "Right",
                    KeyEvent.KEYCODE_DPAD_DOWN to "Down",
                    KeyEvent.KEYCODE_DPAD_UP to "Up",

                    KeyEvent.KEYCODE_ENTER to "Enter",
                    KeyEvent.KEYCODE_HOME to "Home",
                    KeyEvent.KEYCODE_BACK to "Back",
                    KeyEvent.KEYCODE_MENU to "Menu",
                    KeyEvent.KEYCODE_APP_SWITCH to "Recents",
                    KeyEvent.KEYCODE_DEL to "Delete",
                    KeyEvent.KEYCODE_TAB to "Tab",
                    KeyEvent.KEYCODE_SPACE to "Space",
                    KeyEvent.KEYCODE_SEARCH to "Search",
                    KeyEvent.KEYCODE_CAPS_LOCK to "Caps Lock",
                    KeyEvent.KEYCODE_HEADSETHOOK to "Headset Button",
                    KeyEvent.KEYCODE_NUM_LOCK to "Num Lock",

                    KeyEvent.KEYCODE_BUTTON_1 to "Button 1",
                    KeyEvent.KEYCODE_BUTTON_2 to "Button 2",
                    KeyEvent.KEYCODE_BUTTON_3 to "Button 3",
                    KeyEvent.KEYCODE_BUTTON_4 to "Button 4",
                    KeyEvent.KEYCODE_BUTTON_5 to "Button 5",
                    KeyEvent.KEYCODE_BUTTON_6 to "Button 6",
                    KeyEvent.KEYCODE_BUTTON_7 to "Button 7",
                    KeyEvent.KEYCODE_BUTTON_8 to "Button 8",
                    KeyEvent.KEYCODE_BUTTON_9 to "Button 9",
                    KeyEvent.KEYCODE_BUTTON_10 to "Button 10",
                    KeyEvent.KEYCODE_BUTTON_11 to "Button 11",
                    KeyEvent.KEYCODE_BUTTON_12 to "Button 12",
                    KeyEvent.KEYCODE_BUTTON_13 to "Button 13",
                    KeyEvent.KEYCODE_BUTTON_14 to "Button 14",
                    KeyEvent.KEYCODE_BUTTON_15 to "Button 15",
                    KeyEvent.KEYCODE_BUTTON_16 to "Button 16",

                    KeyEvent.KEYCODE_BUTTON_L1 to "Button L1",
                    KeyEvent.KEYCODE_BUTTON_L2 to "Button L2",
                    KeyEvent.KEYCODE_BUTTON_R1 to "Button R1",
                    KeyEvent.KEYCODE_BUTTON_R2 to "Button R2",

                    KeyEvent.KEYCODE_BUTTON_A to "Button A",
                    KeyEvent.KEYCODE_BUTTON_B to "Button B",
                    KeyEvent.KEYCODE_BUTTON_C to "Button C",

                    KeyEvent.KEYCODE_BUTTON_X to "Button X",
                    KeyEvent.KEYCODE_BUTTON_Y to "Button Y",
                    KeyEvent.KEYCODE_BUTTON_Z to "Button Z",

                    KeyEvent.KEYCODE_BUTTON_THUMBL to "Thumb Left",
                    KeyEvent.KEYCODE_BUTTON_THUMBR to "Thumb Right",

                    KeyEvent.KEYCODE_BUTTON_START to "Start",
                    KeyEvent.KEYCODE_BUTTON_SELECT to "Select",

                    KeyEvent.KEYCODE_SOFT_LEFT to "SOFT_LEFT",
                    KeyEvent.KEYCODE_SOFT_RIGHT to "SOFT_RIGHT",
                    KeyEvent.KEYCODE_CALL to "Call",
                    KeyEvent.KEYCODE_ENDCALL to "End Call",
                    KeyEvent.KEYCODE_0 to "0",
                    KeyEvent.KEYCODE_1 to "1",
                    KeyEvent.KEYCODE_2 to "2",
                    KeyEvent.KEYCODE_3 to "3",
                    KeyEvent.KEYCODE_4 to "4",
                    KeyEvent.KEYCODE_5 to "5",
                    KeyEvent.KEYCODE_6 to "6",
                    KeyEvent.KEYCODE_7 to "7",
                    KeyEvent.KEYCODE_8 to "8",
                    KeyEvent.KEYCODE_9 to "9",
                    KeyEvent.KEYCODE_STAR to "*",
                    KeyEvent.KEYCODE_POUND to "#",
                    KeyEvent.KEYCODE_DPAD_CENTER to "DPAD Center",
                    KeyEvent.KEYCODE_POWER to "Power",
                    KeyEvent.KEYCODE_CAMERA to "Camera",
                    KeyEvent.KEYCODE_CLEAR to "Clear",
                    KeyEvent.KEYCODE_A to "A",
                    KeyEvent.KEYCODE_B to "B",
                    KeyEvent.KEYCODE_C to "C",
                    KeyEvent.KEYCODE_D to "D",
                    KeyEvent.KEYCODE_E to "E",
                    KeyEvent.KEYCODE_F to "F",
                    KeyEvent.KEYCODE_G to "G",
                    KeyEvent.KEYCODE_H to "H",
                    KeyEvent.KEYCODE_I to "I",
                    KeyEvent.KEYCODE_J to "J",
                    KeyEvent.KEYCODE_K to "K",
                    KeyEvent.KEYCODE_L to "L",
                    KeyEvent.KEYCODE_M to "M",
                    KeyEvent.KEYCODE_N to "N",
                    KeyEvent.KEYCODE_O to "O",
                    KeyEvent.KEYCODE_P to "P",
                    KeyEvent.KEYCODE_Q to "Q",
                    KeyEvent.KEYCODE_R to "R",
                    KeyEvent.KEYCODE_S to "S",
                    KeyEvent.KEYCODE_T to "T",
                    KeyEvent.KEYCODE_U to "U",
                    KeyEvent.KEYCODE_V to "V",
                    KeyEvent.KEYCODE_W to "W",
                    KeyEvent.KEYCODE_X to "X",
                    KeyEvent.KEYCODE_Y to "Y",
                    KeyEvent.KEYCODE_Z to "Z",
                    KeyEvent.KEYCODE_COMMA to ",",
                    KeyEvent.KEYCODE_PERIOD to ".",
                    KeyEvent.KEYCODE_SYM to "Symbol",
                    KeyEvent.KEYCODE_EXPLORER to "Explorer",
                    KeyEvent.KEYCODE_ENVELOPE to "Mail",
                    KeyEvent.KEYCODE_GRAVE to "`",
                    KeyEvent.KEYCODE_MINUS to "-",
                    KeyEvent.KEYCODE_EQUALS to "=",
                    KeyEvent.KEYCODE_LEFT_BRACKET to "(",
                    KeyEvent.KEYCODE_RIGHT_BRACKET to ")",
                    KeyEvent.KEYCODE_BACKSLASH to "\\",
                    KeyEvent.KEYCODE_SEMICOLON to ";",
                    KeyEvent.KEYCODE_APOSTROPHE to "'",
                    KeyEvent.KEYCODE_SLASH to "/",
                    KeyEvent.KEYCODE_AT to "@",
                    KeyEvent.KEYCODE_NUM to "Num",
                    KeyEvent.KEYCODE_FOCUS to "Focus",
                    KeyEvent.KEYCODE_PLUS to "+",
                    KeyEvent.KEYCODE_NOTIFICATION to "Notification",
                    KeyEvent.KEYCODE_SEARCH to "Search",
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "Play/Pause",
                    KeyEvent.KEYCODE_MEDIA_STOP to "Stop Media",
                    KeyEvent.KEYCODE_MEDIA_NEXT to "Play Next",
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS to "Play Previous",
                    KeyEvent.KEYCODE_MEDIA_REWIND to "Rewind",
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to "Fast Forward",
                    KeyEvent.KEYCODE_MUTE to "Mute Microphone",
                    KeyEvent.KEYCODE_PAGE_UP to "Page Up",
                    KeyEvent.KEYCODE_PAGE_DOWN to "Page Down",
                    KeyEvent.KEYCODE_PICTSYMBOLS to "Picture Symbols",
                    KeyEvent.KEYCODE_SWITCH_CHARSET to "Switch Charset",
                    KeyEvent.KEYCODE_BUTTON_MODE to "Button Mode",
                    KeyEvent.KEYCODE_ESCAPE to "Esc",
                    KeyEvent.KEYCODE_FORWARD_DEL to "Forward Del",
                    KeyEvent.KEYCODE_SCROLL_LOCK to "Scroll Lock",
                    KeyEvent.KEYCODE_META_LEFT to "Meta Left",
                    KeyEvent.KEYCODE_META_RIGHT to "Meta Right",
                    KeyEvent.KEYCODE_FUNCTION to "Function",
                    KeyEvent.KEYCODE_SYSRQ to "SYSRQ",
                    KeyEvent.KEYCODE_BREAK to "Break",
                    KeyEvent.KEYCODE_MOVE_HOME to "Home",
                    KeyEvent.KEYCODE_MOVE_END to "End",
                    KeyEvent.KEYCODE_INSERT to "Insert",
                    KeyEvent.KEYCODE_FORWARD to "Forward",
                    KeyEvent.KEYCODE_MEDIA_PLAY to "Play",
                    KeyEvent.KEYCODE_MEDIA_PAUSE to "Pause",
                    KeyEvent.KEYCODE_MEDIA_CLOSE to "Media Close",
                    KeyEvent.KEYCODE_MEDIA_EJECT to "Eject",
                    KeyEvent.KEYCODE_MEDIA_RECORD to "Media Record",
                    KeyEvent.KEYCODE_F1 to "F1",
                    KeyEvent.KEYCODE_F2 to "F2",
                    KeyEvent.KEYCODE_F3 to "F3",
                    KeyEvent.KEYCODE_F4 to "F4",
                    KeyEvent.KEYCODE_F5 to "F5",
                    KeyEvent.KEYCODE_F6 to "F6",
                    KeyEvent.KEYCODE_F7 to "F7",
                    KeyEvent.KEYCODE_F8 to "F8",
                    KeyEvent.KEYCODE_F9 to "F9",
                    KeyEvent.KEYCODE_F10 to "F10",
                    KeyEvent.KEYCODE_F11 to "F11",
                    KeyEvent.KEYCODE_F12 to "F12",
                    KeyEvent.KEYCODE_NUMPAD_0 to "Numpad 0",
                    KeyEvent.KEYCODE_NUMPAD_1 to "Numpad 1",
                    KeyEvent.KEYCODE_NUMPAD_2 to "Numpad 2",
                    KeyEvent.KEYCODE_NUMPAD_3 to "Numpad 3",
                    KeyEvent.KEYCODE_NUMPAD_4 to "Numpad 4",
                    KeyEvent.KEYCODE_NUMPAD_5 to "Numpad 5",
                    KeyEvent.KEYCODE_NUMPAD_6 to "Numpad 6",
                    KeyEvent.KEYCODE_NUMPAD_7 to "Numpad 7",
                    KeyEvent.KEYCODE_NUMPAD_8 to "Numpad 8",
                    KeyEvent.KEYCODE_NUMPAD_9 to "Numpad 9",
                    KeyEvent.KEYCODE_NUMPAD_DIVIDE to "Numpad Divide",
                    KeyEvent.KEYCODE_NUMPAD_MULTIPLY to "Numpad Multiply",
                    KeyEvent.KEYCODE_NUMPAD_SUBTRACT to "Numpad -",
                    KeyEvent.KEYCODE_NUMPAD_ADD to "Numpad +",
                    KeyEvent.KEYCODE_NUMPAD_DOT to "Numpad .",
                    KeyEvent.KEYCODE_NUMPAD_COMMA to "Numpad ,",
                    KeyEvent.KEYCODE_NUMPAD_ENTER to "Numpad Enter",
                    KeyEvent.KEYCODE_NUMPAD_EQUALS to "Numpad =",
                    KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN to "Numpad (",
                    KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN to "Numpad )",
                    KeyEvent.KEYCODE_VOLUME_MUTE to "Mute Volume",
                    KeyEvent.KEYCODE_INFO to "Info",
                    KeyEvent.KEYCODE_CHANNEL_UP to "Channel Up",
                    KeyEvent.KEYCODE_CHANNEL_DOWN to "Channel Down",
                    KeyEvent.KEYCODE_ZOOM_IN to "Zoom In",
                    KeyEvent.KEYCODE_ZOOM_OUT to "Zoom Out",
                    KeyEvent.KEYCODE_TV to "TV",
                    KeyEvent.KEYCODE_WINDOW to "Window",
                    KeyEvent.KEYCODE_GUIDE to "Guide",
                    KeyEvent.KEYCODE_DVR to "DVR",
                    KeyEvent.KEYCODE_BOOKMARK to "Bookmark",
                    KeyEvent.KEYCODE_CAPTIONS to "Captions",
                    KeyEvent.KEYCODE_SETTINGS to "Settings",
                    KeyEvent.KEYCODE_TV_POWER to "TV Power",
                    KeyEvent.KEYCODE_TV_INPUT to "TV Input",
                    KeyEvent.KEYCODE_STB_POWER to "STB Power",
                    KeyEvent.KEYCODE_STB_INPUT to "STB Input",
                    KeyEvent.KEYCODE_AVR_POWER to "AVR Power",
                    KeyEvent.KEYCODE_AVR_INPUT to "AVR Input",
                    KeyEvent.KEYCODE_PROG_RED to "TV Red",
                    KeyEvent.KEYCODE_PROG_GREEN to "TV Green",
                    KeyEvent.KEYCODE_PROG_YELLOW to "TV Yellow",
                    KeyEvent.KEYCODE_PROG_BLUE to "TV Blue",
                    KeyEvent.KEYCODE_LANGUAGE_SWITCH to "Language Switch",
                    KeyEvent.KEYCODE_MANNER_MODE to "Manner Mode",
                    KeyEvent.KEYCODE_3D_MODE to "3D Mode",
                    KeyEvent.KEYCODE_CONTACTS to "Contacts",
                    KeyEvent.KEYCODE_CALENDAR to "Calendar",
                    KeyEvent.KEYCODE_MUSIC to "Music",
                    KeyEvent.KEYCODE_CALCULATOR to "Calculator",
                    KeyEvent.KEYCODE_ZENKAKU_HANKAKU to "Zenkaku Hankaku",
                    KeyEvent.KEYCODE_EISU to "Eisu",
                    KeyEvent.KEYCODE_MUHENKAN to "Muhenkan",
                    KeyEvent.KEYCODE_HENKAN to "Henkan",
                    KeyEvent.KEYCODE_KATAKANA_HIRAGANA to "Katakana Hiragana",
                    KeyEvent.KEYCODE_YEN to "Yen",
                    KeyEvent.KEYCODE_RO to "Ro",
                    KeyEvent.KEYCODE_KANA to "Kana",
                    KeyEvent.KEYCODE_ASSIST to "Assist",
                    KeyEvent.KEYCODE_BRIGHTNESS_DOWN to "Brightness Down",
                    KeyEvent.KEYCODE_BRIGHTNESS_UP to "Brightness Up",
                    KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK to "Audio Track",
                    KeyEvent.KEYCODE_PAIRING to "Pairing",
                    KeyEvent.KEYCODE_MEDIA_TOP_MENU to "Media Top Menu",
                    KeyEvent.KEYCODE_11 to "11",
                    KeyEvent.KEYCODE_12 to "12",
                    KeyEvent.KEYCODE_LAST_CHANNEL to "Last Channel",
                    KeyEvent.KEYCODE_TV_DATA_SERVICE to "TV Data Service",
                    KeyEvent.KEYCODE_VOICE_ASSIST to "Voice Assist",
                    KeyEvent.KEYCODE_TV_RADIO_SERVICE to "TV Radio Service",
                    KeyEvent.KEYCODE_TV_TELETEXT to "TV Teletext",
                    KeyEvent.KEYCODE_TV_NUMBER_ENTRY to "TV Number Entry",
                    KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG to "TV Terrestrial Analog",
                    KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL to "TV Terrestrial Digital",
                    KeyEvent.KEYCODE_TV_SATELLITE to "TV Satellite",
                    KeyEvent.KEYCODE_TV_SATELLITE_BS to "TV Satellite BS",
                    KeyEvent.KEYCODE_TV_SATELLITE_CS to "TV Satellite CS",
                    KeyEvent.KEYCODE_TV_SATELLITE_SERVICE to "TV Satellite Service",
                    KeyEvent.KEYCODE_TV_NETWORK to "TV Network",
                    KeyEvent.KEYCODE_TV_ANTENNA_CABLE to "TV Antenna Cable",
                    KeyEvent.KEYCODE_TV_INPUT_HDMI_1 to "TV HDMI 1",
                    KeyEvent.KEYCODE_TV_INPUT_HDMI_2 to "TV HDMI 2",
                    KeyEvent.KEYCODE_TV_INPUT_HDMI_3 to "TV HDMI 3",
                    KeyEvent.KEYCODE_TV_INPUT_HDMI_4 to "TV HDMI 4",
                    KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1 to "TV Composite 1",
                    KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2 to "TV Composite 2",
                    KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1 to "TV Component 1",
                    KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2 to "TV Component 2",
                    KeyEvent.KEYCODE_TV_INPUT_VGA_1 to "TV VGA 1",
                    KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION to "TV Audio Description",
                    KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP to "TV Audio Description Vol Up",
                    KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN to "TV Audio Description Vol Down",
                    KeyEvent.KEYCODE_TV_ZOOM_MODE to "TV Zoom Mode",
                    KeyEvent.KEYCODE_TV_CONTENTS_MENU to "TV Contents Menu",
                    KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU to "TV Media Context Menu",
                    KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING to "TV Timer Programming",
                    KeyEvent.KEYCODE_HELP to "Help",
                ),
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                yieldAll(
                    listOf(
                        KeyEvent.KEYCODE_NAVIGATE_PREVIOUS to "Navigate Previous",
                        KeyEvent.KEYCODE_NAVIGATE_NEXT to "Navigate Next",
                        KeyEvent.KEYCODE_NAVIGATE_IN to "Navigate In",
                        KeyEvent.KEYCODE_NAVIGATE_OUT to "Navigate Out",
                        KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD to "Media Skip Forward",
                        KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD to "Media Skip Backward",
                        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD to "Media Step Forward",
                        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD to "Media Step Backward",
                    ),
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                yieldAll(
                    listOf(
                        KeyEvent.KEYCODE_STEM_PRIMARY to "Stem Primary",
                        KeyEvent.KEYCODE_STEM_1 to "Stem 1",
                        KeyEvent.KEYCODE_STEM_2 to "Stem 2",
                        KeyEvent.KEYCODE_STEM_3 to "Stem 3",
                        KeyEvent.KEYCODE_DPAD_UP_LEFT to "DPAD Up Left",
                        KeyEvent.KEYCODE_DPAD_DOWN_LEFT to "DPAD Down Left",
                        KeyEvent.KEYCODE_DPAD_UP_RIGHT to "DPAD Up Right",
                        KeyEvent.KEYCODE_DPAD_DOWN_RIGHT to "DPAD Down Right",
                        KeyEvent.KEYCODE_SOFT_SLEEP to "Soft Sleep",
                        KeyEvent.KEYCODE_CUT to "Cut",
                        KeyEvent.KEYCODE_COPY to "Copy",
                        KeyEvent.KEYCODE_PASTE to "Paste",
                    ),
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                yieldAll(
                    listOf(
                        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP to "System Nav Up",
                        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN to "System Nav Down",
                        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT to "System Nav Left",
                        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT to "System Nav Right",
                    ),
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                yield(KeyEvent.KEYCODE_ALL_APPS to "All Apps")
            }
        }.toMap()

    private val KEYCODES: Set<Int>
        get() = setOf(
            KeyEvent.KEYCODE_SOFT_LEFT,
            KeyEvent.KEYCODE_SOFT_RIGHT,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_CALL,
            KeyEvent.KEYCODE_ENDCALL,
            KeyEvent.KEYCODE_0,
            KeyEvent.KEYCODE_1,
            KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4,
            KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_7,
            KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9,
            KeyEvent.KEYCODE_STAR,
            KeyEvent.KEYCODE_POUND,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_CLEAR,
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_B,
            KeyEvent.KEYCODE_C,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_E,
            KeyEvent.KEYCODE_F,
            KeyEvent.KEYCODE_G,
            KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_I,
            KeyEvent.KEYCODE_J,
            KeyEvent.KEYCODE_K,
            KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_M,
            KeyEvent.KEYCODE_N,
            KeyEvent.KEYCODE_O,
            KeyEvent.KEYCODE_P,
            KeyEvent.KEYCODE_Q,
            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_U,
            KeyEvent.KEYCODE_V,
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_Y,
            KeyEvent.KEYCODE_Z,
            KeyEvent.KEYCODE_COMMA,
            KeyEvent.KEYCODE_PERIOD,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_TAB,
            KeyEvent.KEYCODE_SPACE,
            KeyEvent.KEYCODE_SYM,
            KeyEvent.KEYCODE_EXPLORER,
            KeyEvent.KEYCODE_ENVELOPE,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_FORWARD_DEL,
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_GRAVE,
            KeyEvent.KEYCODE_MINUS,
            KeyEvent.KEYCODE_EQUALS,
            KeyEvent.KEYCODE_LEFT_BRACKET,
            KeyEvent.KEYCODE_RIGHT_BRACKET,
            KeyEvent.KEYCODE_BACKSLASH,
            KeyEvent.KEYCODE_SEMICOLON,
            KeyEvent.KEYCODE_APOSTROPHE,
            KeyEvent.KEYCODE_SLASH,
            KeyEvent.KEYCODE_AT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_FOCUS,
            KeyEvent.KEYCODE_PLUS,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_NOTIFICATION,
            KeyEvent.KEYCODE_SEARCH,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS,
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_PAGE_UP,
            KeyEvent.KEYCODE_PAGE_DOWN,
            KeyEvent.KEYCODE_PICTSYMBOLS,
            KeyEvent.KEYCODE_SWITCH_CHARSET,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_C,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_Z,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE,
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_DEL,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_CAPS_LOCK,
            KeyEvent.KEYCODE_SCROLL_LOCK,
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_FUNCTION,
            KeyEvent.KEYCODE_SYSRQ,
            KeyEvent.KEYCODE_BREAK,
            KeyEvent.KEYCODE_MOVE_HOME,
            KeyEvent.KEYCODE_MOVE_END,
            KeyEvent.KEYCODE_INSERT,
            KeyEvent.KEYCODE_FORWARD,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_CLOSE,
            KeyEvent.KEYCODE_MEDIA_EJECT,
            KeyEvent.KEYCODE_MEDIA_RECORD,
            KeyEvent.KEYCODE_F1,
            KeyEvent.KEYCODE_F2,
            KeyEvent.KEYCODE_F3,
            KeyEvent.KEYCODE_F4,
            KeyEvent.KEYCODE_F5,
            KeyEvent.KEYCODE_F6,
            KeyEvent.KEYCODE_F7,
            KeyEvent.KEYCODE_F8,
            KeyEvent.KEYCODE_F9,
            KeyEvent.KEYCODE_F10,
            KeyEvent.KEYCODE_F11,
            KeyEvent.KEYCODE_F12,
            KeyEvent.KEYCODE_NUM,
            KeyEvent.KEYCODE_NUM_LOCK,
            KeyEvent.KEYCODE_NUMPAD_0,
            KeyEvent.KEYCODE_NUMPAD_1,
            KeyEvent.KEYCODE_NUMPAD_2,
            KeyEvent.KEYCODE_NUMPAD_3,
            KeyEvent.KEYCODE_NUMPAD_4,
            KeyEvent.KEYCODE_NUMPAD_5,
            KeyEvent.KEYCODE_NUMPAD_6,
            KeyEvent.KEYCODE_NUMPAD_7,
            KeyEvent.KEYCODE_NUMPAD_8,
            KeyEvent.KEYCODE_NUMPAD_9,
            KeyEvent.KEYCODE_NUMPAD_DIVIDE,
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY,
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
            KeyEvent.KEYCODE_NUMPAD_ADD,
            KeyEvent.KEYCODE_NUMPAD_DOT,
            KeyEvent.KEYCODE_NUMPAD_COMMA,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_NUMPAD_EQUALS,
            KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN,
            KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN,
            KeyEvent.KEYCODE_MUTE,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_INFO,
            KeyEvent.KEYCODE_CHANNEL_UP,
            KeyEvent.KEYCODE_CHANNEL_DOWN,
            KeyEvent.KEYCODE_ZOOM_IN,
            KeyEvent.KEYCODE_ZOOM_OUT,
            KeyEvent.KEYCODE_TV,
            KeyEvent.KEYCODE_WINDOW,
            KeyEvent.KEYCODE_GUIDE,
            KeyEvent.KEYCODE_DVR,
            KeyEvent.KEYCODE_BOOKMARK,
            KeyEvent.KEYCODE_CAPTIONS,
            KeyEvent.KEYCODE_SETTINGS,
            KeyEvent.KEYCODE_TV_POWER,
            KeyEvent.KEYCODE_TV_INPUT,
            KeyEvent.KEYCODE_STB_POWER,
            KeyEvent.KEYCODE_STB_INPUT,
            KeyEvent.KEYCODE_AVR_POWER,
            KeyEvent.KEYCODE_AVR_INPUT,
            KeyEvent.KEYCODE_PROG_RED,
            KeyEvent.KEYCODE_PROG_GREEN,
            KeyEvent.KEYCODE_PROG_YELLOW,
            KeyEvent.KEYCODE_PROG_BLUE,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_BUTTON_3,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_BUTTON_5,
            KeyEvent.KEYCODE_BUTTON_6,
            KeyEvent.KEYCODE_BUTTON_7,
            KeyEvent.KEYCODE_BUTTON_8,
            KeyEvent.KEYCODE_BUTTON_9,
            KeyEvent.KEYCODE_BUTTON_10,
            KeyEvent.KEYCODE_BUTTON_11,
            KeyEvent.KEYCODE_BUTTON_12,
            KeyEvent.KEYCODE_BUTTON_13,
            KeyEvent.KEYCODE_BUTTON_14,
            KeyEvent.KEYCODE_BUTTON_15,
            KeyEvent.KEYCODE_BUTTON_16,
            KeyEvent.KEYCODE_LANGUAGE_SWITCH,
            KeyEvent.KEYCODE_MANNER_MODE,
            KeyEvent.KEYCODE_3D_MODE,
            KeyEvent.KEYCODE_CONTACTS,
            KeyEvent.KEYCODE_CALENDAR,
            KeyEvent.KEYCODE_MUSIC,
            KeyEvent.KEYCODE_CALCULATOR,
            KeyEvent.KEYCODE_ZENKAKU_HANKAKU,
            KeyEvent.KEYCODE_EISU,
            KeyEvent.KEYCODE_MUHENKAN,
            KeyEvent.KEYCODE_HENKAN,
            KeyEvent.KEYCODE_KATAKANA_HIRAGANA,
            KeyEvent.KEYCODE_YEN,
            KeyEvent.KEYCODE_RO,
            KeyEvent.KEYCODE_KANA,
            KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_POWER,
            KeyEvent.KEYCODE_BRIGHTNESS_DOWN,
            KeyEvent.KEYCODE_BRIGHTNESS_UP,
            KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK,
            KeyEvent.KEYCODE_PAIRING,
            KeyEvent.KEYCODE_MEDIA_TOP_MENU,
            KeyEvent.KEYCODE_11,
            KeyEvent.KEYCODE_12,
            KeyEvent.KEYCODE_LAST_CHANNEL,
            KeyEvent.KEYCODE_TV_DATA_SERVICE,
            KeyEvent.KEYCODE_VOICE_ASSIST,
            KeyEvent.KEYCODE_TV_RADIO_SERVICE,
            KeyEvent.KEYCODE_TV_TELETEXT,
            KeyEvent.KEYCODE_TV_NUMBER_ENTRY,
            KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG,
            KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL,
            KeyEvent.KEYCODE_TV_SATELLITE,
            KeyEvent.KEYCODE_TV_SATELLITE_BS,
            KeyEvent.KEYCODE_TV_SATELLITE_CS,
            KeyEvent.KEYCODE_TV_SATELLITE_SERVICE,
            KeyEvent.KEYCODE_TV_NETWORK,
            KeyEvent.KEYCODE_TV_ANTENNA_CABLE,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_1,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_2,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_3,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_4,
            KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1,
            KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2,
            KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1,
            KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2,
            KeyEvent.KEYCODE_TV_INPUT_VGA_1,
            KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION,
            KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP,
            KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN,
            KeyEvent.KEYCODE_TV_ZOOM_MODE,
            KeyEvent.KEYCODE_TV_CONTENTS_MENU,
            KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU,
            KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING,
            KeyEvent.KEYCODE_HELP,
        )

    private val KEYCODES_API_23: Set<Int>
        get() = setOf(
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
            KeyEvent.KEYCODE_NAVIGATE_NEXT,
            KeyEvent.KEYCODE_NAVIGATE_IN,
            KeyEvent.KEYCODE_NAVIGATE_OUT,
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD,
        )

    private val KEYCODES_API_24: Set<Int>
        get() = setOf(
            KeyEvent.KEYCODE_STEM_PRIMARY,
            KeyEvent.KEYCODE_STEM_1,
            KeyEvent.KEYCODE_STEM_2,
            KeyEvent.KEYCODE_STEM_3,
            KeyEvent.KEYCODE_DPAD_UP_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN_LEFT,
            KeyEvent.KEYCODE_DPAD_UP_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN_RIGHT,
            KeyEvent.KEYCODE_SOFT_SLEEP,
            KeyEvent.KEYCODE_CUT,
            KeyEvent.KEYCODE_COPY,
            KeyEvent.KEYCODE_PASTE,
        )

    private val KEYCODES_API_25: Set<Int>
        get() = setOf(
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT,
        )

    private val KEYCODES_API_28: Set<Int>
        get() = setOf(
            KeyEvent.KEYCODE_ALL_APPS,
        )

    /**
     * These are key code maps for the getevent command. These names aren't the same as the
     * KeyEvent key codes in the Android SDK so these have to be manually whitelisted
     * as people need.
     */
    val GET_EVENT_LABEL_TO_KEYCODE: List<Pair<String, Int>> = listOf(
        "KEY_VOLUMEDOWN" to KeyEvent.KEYCODE_VOLUME_DOWN,
        "KEY_VOLUMEUP" to KeyEvent.KEYCODE_VOLUME_UP,
        "KEY_MEDIA" to KeyEvent.KEYCODE_HEADSETHOOK,
        "KEY_HEADSETHOOK" to KeyEvent.KEYCODE_HEADSETHOOK,
        "KEY_CAMERA_FOCUS" to KeyEvent.KEYCODE_FOCUS,
        "02fe" to KeyEvent.KEYCODE_CAMERA,
        "00fa" to KeyEvent.KEYCODE_CAMERA,

        // This kernel key event code seems to be the Bixby button
        // but different ROMs have different key maps and so
        // it is reported as different Android key codes.
        "02bf" to KeyEvent.KEYCODE_MENU,
        "02bf" to KeyEvent.KEYCODE_ASSIST,

        "KEY_SEARCH" to KeyEvent.KEYCODE_SEARCH,
    )

    fun canDetectKeyWhenScreenOff(keyCode: Int): Boolean = GET_EVENT_LABEL_TO_KEYCODE.any { it.second == keyCode }

    val MODIFIER_KEYCODES: Set<Int>
        get() = setOf(
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_SYM,
            KeyEvent.KEYCODE_NUM,
            KeyEvent.KEYCODE_FUNCTION,
        )

    /**
     * Create a text representation of a key event. E.g if the control key was pressed,
     * "Ctrl" will be returned
     */
    fun keyCodeToString(keyCode: Int): String = NON_CHARACTER_KEY_LABELS[keyCode].let {
        it ?: "unknown keycode $keyCode"
    }

    fun isModifierKey(keyCode: Int): Boolean = keyCode in MODIFIER_KEYCODES

    fun isGamepadKeyCode(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_C,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_Z,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_MODE,
            KeyEvent.KEYCODE_BUTTON_1,
            KeyEvent.KEYCODE_BUTTON_2,
            KeyEvent.KEYCODE_BUTTON_3,
            KeyEvent.KEYCODE_BUTTON_4,
            KeyEvent.KEYCODE_BUTTON_5,
            KeyEvent.KEYCODE_BUTTON_6,
            KeyEvent.KEYCODE_BUTTON_7,
            KeyEvent.KEYCODE_BUTTON_8,
            KeyEvent.KEYCODE_BUTTON_9,
            KeyEvent.KEYCODE_BUTTON_10,
            KeyEvent.KEYCODE_BUTTON_11,
            KeyEvent.KEYCODE_BUTTON_12,
            KeyEvent.KEYCODE_BUTTON_13,
            KeyEvent.KEYCODE_BUTTON_14,
            KeyEvent.KEYCODE_BUTTON_15,
            KeyEvent.KEYCODE_BUTTON_16,
            -> return true

            else -> return false
        }
    }

    /**
     * Get all the valid key codes which work on the Android version for the device.
     */
    fun getKeyCodes(): List<Int> {
        val keyCodes = KEYCODES.toMutableList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyCodes.addAll(KEYCODES_API_23)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            keyCodes.addAll(KEYCODES_API_24)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            keyCodes.addAll(KEYCODES_API_25)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            keyCodes.addAll(KEYCODES_API_28)
        }

        return keyCodes
    }

    fun modifierKeycodeToMetaState(modifier: Int) = when (modifier) {
        KeyEvent.KEYCODE_ALT_LEFT -> KeyEvent.META_ALT_LEFT_ON.withFlag(KeyEvent.META_ALT_ON)
        KeyEvent.KEYCODE_ALT_RIGHT -> KeyEvent.META_ALT_RIGHT_ON.withFlag(KeyEvent.META_ALT_ON)

        KeyEvent.KEYCODE_SHIFT_LEFT -> KeyEvent.META_SHIFT_LEFT_ON.withFlag(KeyEvent.META_SHIFT_ON)
        KeyEvent.KEYCODE_SHIFT_RIGHT -> KeyEvent.META_SHIFT_RIGHT_ON.withFlag(KeyEvent.META_SHIFT_ON)

        KeyEvent.KEYCODE_SYM -> KeyEvent.META_SYM_ON

        KeyEvent.KEYCODE_FUNCTION -> KeyEvent.META_FUNCTION_ON

        KeyEvent.KEYCODE_CTRL_LEFT -> KeyEvent.META_CTRL_LEFT_ON.withFlag(KeyEvent.META_CTRL_ON)
        KeyEvent.KEYCODE_CTRL_RIGHT -> KeyEvent.META_CTRL_RIGHT_ON.withFlag(KeyEvent.META_CTRL_ON)

        KeyEvent.KEYCODE_META_LEFT -> KeyEvent.META_META_LEFT_ON.withFlag(KeyEvent.META_META_ON)
        KeyEvent.KEYCODE_META_RIGHT -> KeyEvent.META_META_RIGHT_ON.withFlag(KeyEvent.META_META_ON)

        KeyEvent.KEYCODE_CAPS_LOCK -> KeyEvent.META_CAPS_LOCK_ON
        KeyEvent.KEYCODE_NUM_LOCK -> KeyEvent.META_NUM_LOCK_ON
        KeyEvent.KEYCODE_SCROLL_LOCK -> KeyEvent.META_SCROLL_LOCK_ON

        else -> throw Exception("can't convert modifier $modifier to meta state")
    }

    val MODIFIER_LABELS = mapOf(
        KeyEvent.META_CTRL_ON to R.string.meta_state_ctrl,
        KeyEvent.META_CTRL_LEFT_ON to R.string.meta_state_ctrl_left,
        KeyEvent.META_CTRL_RIGHT_ON to R.string.meta_state_ctrl_right,

        KeyEvent.META_ALT_ON to R.string.meta_state_alt,
        KeyEvent.META_ALT_LEFT_ON to R.string.meta_state_alt_left,
        KeyEvent.META_ALT_RIGHT_ON to R.string.meta_state_alt_right,

        KeyEvent.META_SHIFT_ON to R.string.meta_state_shift,
        KeyEvent.META_SHIFT_LEFT_ON to R.string.meta_state_shift_left,
        KeyEvent.META_SHIFT_RIGHT_ON to R.string.meta_state_shift_right,

        KeyEvent.META_META_ON to R.string.meta_state_meta,
        KeyEvent.META_META_LEFT_ON to R.string.meta_state_meta_left,
        KeyEvent.META_META_RIGHT_ON to R.string.meta_state_meta_right,

        KeyEvent.META_SYM_ON to R.string.meta_state_sym,
        KeyEvent.META_CAPS_LOCK_ON to R.string.meta_state_caps_lock,
        KeyEvent.META_NUM_LOCK_ON to R.string.meta_state_num_lock,
        KeyEvent.META_SCROLL_LOCK_ON to R.string.meta_state_scroll_lock,
        KeyEvent.META_FUNCTION_ON to R.string.meta_state_function,
    )

    fun isDpadKeyCode(code: Int): Boolean {
        return code == KeyEvent.KEYCODE_DPAD_LEFT ||
            code == KeyEvent.KEYCODE_DPAD_RIGHT ||
            code == KeyEvent.KEYCODE_DPAD_UP ||
            code == KeyEvent.KEYCODE_DPAD_DOWN ||
            code == KeyEvent.KEYCODE_DPAD_UP_LEFT ||
            code == KeyEvent.KEYCODE_DPAD_UP_RIGHT ||
            code == KeyEvent.KEYCODE_DPAD_DOWN_LEFT ||
            code == KeyEvent.KEYCODE_DPAD_DOWN_RIGHT
    }

    fun isDpadDevice(event: InputEvent): Boolean = // Check that input comes from a device with directional pads.
        event.source and InputDevice.SOURCE_DPAD != InputDevice.SOURCE_DPAD
}
