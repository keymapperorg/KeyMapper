//! Hardcoded Generic key layout map based on AOSP Generic.kl.
//!
//! This provides a fallback key layout when no device-specific or system Generic.kl
//! file is available.
//!
//! Source: https://cs.android.com/android/platform/superproject/+/android-latest-release:frameworks/base/data/keyboards/Generic.kl

/// Generic key layout content matching AOSP Generic.kl
pub const GENERIC_KEY_LAYOUT_CONTENTS: &str = r#"
key 1     ESCAPE
key 2     1
key 3     2
key 4     3
key 5     4
key 6     5
key 7     6
key 8     7
key 9     8
key 10    9
key 11    0
key 12    MINUS
key 13    EQUALS
key 14    DEL
key 15    TAB
key 16    Q
key 17    W
key 18    E
key 19    R
key 20    T
key 21    Y
key 22    U
key 23    I
key 24    O
key 25    P
key 26    LEFT_BRACKET
key 27    RIGHT_BRACKET
key 28    ENTER
key 29    CTRL_LEFT
key 30    A
key 31    S
key 32    D
key 33    F
key 34    G
key 35    H
key 36    J
key 37    K
key 38    L
key 39    SEMICOLON
key 40    APOSTROPHE
key 41    GRAVE
key 42    SHIFT_LEFT
key 43    BACKSLASH
key 44    Z
key 45    X
key 46    C
key 47    V
key 48    B
key 49    N
key 50    M
key 51    COMMA
key 52    PERIOD
key 53    SLASH
key 54    SHIFT_RIGHT
key 55    NUMPAD_MULTIPLY
key 56    ALT_LEFT
key 57    SPACE
key 58    CAPS_LOCK
key 59    F1
key 60    F2
key 61    F3
key 62    F4
key 63    F5
key 64    F6
key 65    F7
key 66    F8
key 67    F9
key 68    F10
key 69    NUM_LOCK
key 70    SCROLL_LOCK
key 71    NUMPAD_7
key 72    NUMPAD_8
key 73    NUMPAD_9
key 74    NUMPAD_SUBTRACT
key 75    NUMPAD_4
key 76    NUMPAD_5
key 77    NUMPAD_6
key 78    NUMPAD_ADD
key 79    NUMPAD_1
key 80    NUMPAD_2
key 81    NUMPAD_3
key 82    NUMPAD_0
key 83    NUMPAD_DOT
key 85    ZENKAKU_HANKAKU
key 86    BACKSLASH
key 87    F11
key 88    F12
key 89    RO
key 92    HENKAN
key 93    KATAKANA_HIRAGANA
key 94    MUHENKAN
key 95    NUMPAD_COMMA
key 96    NUMPAD_ENTER
key 97    CTRL_RIGHT
key 98    NUMPAD_DIVIDE
key 99    SYSRQ
key 100   ALT_RIGHT
key 102   MOVE_HOME
key 103   DPAD_UP
key 104   PAGE_UP
key 105   DPAD_LEFT
key 106   DPAD_RIGHT
key 107   MOVE_END
key 108   DPAD_DOWN
key 109   PAGE_DOWN
key 110   INSERT
key 111   FORWARD_DEL
key 113   VOLUME_MUTE
key 114   VOLUME_DOWN
key 115   VOLUME_UP
key 116   POWER
key 117   NUMPAD_EQUALS
key 119   BREAK
key 120   RECENT_APPS
key 121   NUMPAD_COMMA
key 122   KANA
key 123   EISU
key 124   YEN
key 125   META_LEFT
key 126   META_RIGHT
key 127   MENU
key 128   MEDIA_STOP
key 133   COPY
key 135   PASTE
key 137   CUT
key 139   MENU
key 140   CALCULATOR
key 142   SLEEP
key 143   WAKEUP
key 150   EXPLORER
key 152   POWER
key 155   ENVELOPE
key 156   BOOKMARK
key 158   BACK
key 159   FORWARD
key 160   MEDIA_CLOSE
key 161   MEDIA_EJECT
key 162   MEDIA_EJECT
key 163   MEDIA_NEXT
key 164   MEDIA_PLAY_PAUSE
key 165   MEDIA_PREVIOUS
key 166   MEDIA_STOP
key 167   MEDIA_RECORD
key 168   MEDIA_REWIND
key 169   CALL
key 171   MUSIC
key 172   HOME
key 173   REFRESH
key 177   PAGE_UP
key 178   PAGE_DOWN
key 179   NUMPAD_LEFT_PAREN
key 180   NUMPAD_RIGHT_PAREN
key 200   MEDIA_PLAY
key 201   MEDIA_PAUSE
key 204   NOTIFICATION
key 207   MEDIA_PLAY
key 208   MEDIA_FAST_FORWARD
key 212   CAMERA
key 213   MUSIC
key 215   ENVELOPE
key 217   SEARCH
key 224   BRIGHTNESS_DOWN
key 225   BRIGHTNESS_UP
key 226   HEADSETHOOK
key 228   KEYBOARD_BACKLIGHT_TOGGLE
key 229   KEYBOARD_BACKLIGHT_DOWN
key 230   KEYBOARD_BACKLIGHT_UP
key 248   MUTE
key 256   BUTTON_1
key 257   BUTTON_2
key 258   BUTTON_3
key 259   BUTTON_4
key 260   BUTTON_5
key 261   BUTTON_6
key 262   BUTTON_7
key 263   BUTTON_8
key 264   BUTTON_9
key 265   BUTTON_10
key 266   BUTTON_11
key 267   BUTTON_12
key 268   BUTTON_13
key 269   BUTTON_14
key 270   BUTTON_15
key 271   BUTTON_16
key 288   BUTTON_1
key 289   BUTTON_2
key 290   BUTTON_3
key 291   BUTTON_4
key 292   BUTTON_5
key 293   BUTTON_6
key 294   BUTTON_7
key 295   BUTTON_8
key 296   BUTTON_9
key 297   BUTTON_10
key 298   BUTTON_11
key 299   BUTTON_12
key 300   BUTTON_13
key 301   BUTTON_14
key 302   BUTTON_15
key 303   BUTTON_16
key 304   BUTTON_A
key 305   BUTTON_B
key 306   BUTTON_C
key 307   BUTTON_X
key 308   BUTTON_Y
key 309   BUTTON_Z
key 310   BUTTON_L1
key 311   BUTTON_R1
key 312   BUTTON_L2
key 313   BUTTON_R2
key 314   BUTTON_SELECT
key 315   BUTTON_START
key 316   BUTTON_MODE
key 317   BUTTON_THUMBL
key 318   BUTTON_THUMBR
key 329   STYLUS_BUTTON_TERTIARY
key 331   STYLUS_BUTTON_PRIMARY
key 332   STYLUS_BUTTON_SECONDARY
key 353   DPAD_CENTER
key 362   GUIDE
key 366   DVR
key 370   CAPTIONS
key 377   TV
key 397   CALENDAR
key 398   PROG_RED
key 399   PROG_GREEN
key 400   PROG_YELLOW
key 401   PROG_BLUE
key 402   CHANNEL_UP
key 403   CHANNEL_DOWN
key 405   LAST_CHANNEL
key 418   ZOOM_IN
key 419   ZOOM_OUT
key 429   CONTACTS
key 464   FUNCTION
key 465   ESCAPE            FUNCTION
key 466   F1                FUNCTION
key 467   F2                FUNCTION
key 468   F3                FUNCTION
key 469   F4                FUNCTION
key 470   F5                FUNCTION
key 471   F6                FUNCTION
key 472   F7                FUNCTION
key 473   F8                FUNCTION
key 474   F9                FUNCTION
key 475   F10               FUNCTION
key 476   F11               FUNCTION
key 477   F12               FUNCTION
key 478   1                 FUNCTION
key 479   2                 FUNCTION
key 480   D                 FUNCTION
key 481   E                 FUNCTION
key 482   F                 FUNCTION
key 483   S                 FUNCTION
key 484   B                 FUNCTION
key 522   STAR
key 523   POUND
key 528   FOCUS
key 580   APP_SWITCH
key 582   VOICE_ASSIST
key 583   ASSIST
key 656   MACRO_1
key 657   MACRO_2
key 658   MACRO_3
key 659   MACRO_4
axis 0x00 X
axis 0x01 Y
axis 0x02 Z
axis 0x03 RX
axis 0x04 RY
axis 0x05 RZ
axis 0x06 THROTTLE
axis 0x07 RUDDER
axis 0x08 WHEEL
axis 0x09 RTRIGGER
axis 0x0a LTRIGGER
axis 0x10 HAT_X
axis 0x11 HAT_Y
"#;


