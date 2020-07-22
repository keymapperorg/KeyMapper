package io.github.sds100.keymapper.util.delegate

/**
 * Created by sds100 on 23/05/20.
 */
data class KeymapDetectionPreferences(var defaultLongPressDelay: Int,
                                      var defaultDoublePressDelay: Int,
                                      var defaultRepeatDelay: Int,
                                      var defaultRepeatRate: Int,
                                      var defaultSequenceTriggerTimeout: Int,
                                      var defaultVibrateDuration: Int,
                                      var forceVibrate: Boolean)