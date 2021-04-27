package io.github.sds100.keymapper.mappings.fingerprintmaps

/**
 * Created by sds100 on 04/04/2021.
 */
data class FingerprintMapGroup(
    val swipeDown: FingerprintMap = FingerprintMap(),
    val swipeUp: FingerprintMap = FingerprintMap(),
    val swipeLeft: FingerprintMap = FingerprintMap(),
    val swipeRight: FingerprintMap = FingerprintMap()
) {
    fun get(fingerprintMapId: FingerprintMapId): FingerprintMap {
        return when (fingerprintMapId) {
            FingerprintMapId.SWIPE_DOWN -> swipeDown
            FingerprintMapId.SWIPE_UP -> swipeUp
            FingerprintMapId.SWIPE_LEFT -> swipeLeft
            FingerprintMapId.SWIPE_RIGHT -> swipeRight
        }
    }

    fun toList(): List<FingerprintMap> = listOf(swipeDown, swipeLeft, swipeUp, swipeRight)
}