package io.github.sds100.keymapper.data

import androidx.datastore.preferences.core.preferencesKey

/**
 * Created by sds100 on 17/11/20.
 */
object DataStoreKeys {
    val FINGERPRINT_GESTURE_SWIPE_DOWN = preferencesKey<String>("swipe_down")
    val FINGERPRINT_GESTURE_SWIPE_UP = preferencesKey<String>("swipe_up")
    val FINGERPRINT_GESTURE_SWIPE_LEFT = preferencesKey<String>("swipe_left")
    val FINGERPRINT_GESTURE_SWIPE_RIGHT = preferencesKey<String>("swipe_right")
}