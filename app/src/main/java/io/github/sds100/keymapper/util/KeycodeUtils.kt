package io.github.sds100.keymapper.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import io.github.sds100.keymapper.service.MyIMEService

/**
 * Created by sds100 on 17/07/2018.
 */

val KeyEvent.isVolumeKey: Boolean
    get() = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        || keyCode == KeyEvent.KEYCODE_VOLUME_UP
        || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
        || keyCode == KeyEvent.KEYCODE_MUTE


fun Context.inputKeyEvent(action: Int = KeyEvent.ACTION_DOWN, keyCode: Int, metaState: Int? = null) {
    val time = System.currentTimeMillis()

    val event = if (metaState == null
    ) {
        KeyEvent(action, keyCode)
    } else {
        KeyEvent(
            time,
            time,
            action,
            keyCode,
            0,
            metaState
        )
    }

    val intent = Intent(MyIMEService.ACTION_INPUT_KEYEVENT)
    intent.putExtra(MyIMEService.EXTRA_KEYEVENT, event)
    sendBroadcast(intent)
}

fun Context.inputKeyCode(keyCode: Int) {
    val intent = Intent(MyIMEService.ACTION_INPUT_KEYCODE)
    //put the keycode in the intent
    intent.putExtra(MyIMEService.EXTRA_KEYCODE, keyCode)

    sendBroadcast(intent)
}

object KeycodeUtils {
    /**
     * Maps keys which aren't single characters like the Control keys to a string representation
     */
    private val NON_CHARACTER_KEY_LABELS = mutableMapOf(
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
        KeyEvent.KEYCODE_BUTTON_SELECT to "Select"

    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            put(KeyEvent.KEYCODE_ALL_APPS, "All Apps")
        }
    }

    private val KEYCODES = setOf(
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
        KeyEvent.KEYCODE_NUM,
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
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_MUTE,
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
        KeyEvent.KEYCODE_FORWARD_DEL,
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
        KeyEvent.KEYCODE_BRIGHTNESS_DOWN,
        KeyEvent.KEYCODE_BRIGHTNESS_UP,
        KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK,
        KeyEvent.KEYCODE_POWER)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val KEYCODES_API_21 = listOf(
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
        KeyEvent.KEYCODE_HELP
    )

    @RequiresApi(Build.VERSION_CODES.M)
    private val KEYCODES_API_23 = listOf(
        KeyEvent.KEYCODE_NAVIGATE_PREVIOUS,
        KeyEvent.KEYCODE_NAVIGATE_NEXT,
        KeyEvent.KEYCODE_NAVIGATE_IN,
        KeyEvent.KEYCODE_NAVIGATE_OUT,
        KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD,
        KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD,
        KeyEvent.KEYCODE_MEDIA_STEP_FORWARD,
        KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD
    )

    @RequiresApi(Build.VERSION_CODES.N)
    private val KEYCODES_API_24 = listOf(
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
        KeyEvent.KEYCODE_PASTE
    )

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private val KEYCODES_API_25 = listOf(
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP,
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN,
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT,
        KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT
    )

    @RequiresApi(Build.VERSION_CODES.P)
    private val KEYCODES_API_28 = listOf(
        KeyEvent.KEYCODE_ALL_APPS
    )

    /**
     * Create a text representation of a key event. E.g if the control key was pressed,
     * "Ctrl" will be returned
     */
    fun keycodeToString(keyCode: Int): String {
        return if (NON_CHARACTER_KEY_LABELS.containsKey(keyCode)) {
            NON_CHARACTER_KEY_LABELS.getValue(keyCode)
        } else {
            KeyEvent(KeyEvent.ACTION_UP, keyCode).displayLabel.toString()
        }
    }

    fun keyEventToString(event: KeyEvent): String {
        return keycodeToString(event.keyCode)
    }

    /**
     * Get all the valid key codes which work on the Android version for the device.
     */
    fun getKeyCodes(): List<Int> {
        val keyCodes = KEYCODES.toMutableList()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            keyCodes.addAll(KEYCODES_API_21)
        }

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
}