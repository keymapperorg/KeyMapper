package io.github.sds100.keymapper.base.util


object VersionHelper {
    const val VERSION_2_3_0 = 43

    /**
     * Use version code for 2.2.0.beta.2 because in beta 1 there were issues detecting the
     * availability of fingerprint gestures.
     */
    const val FINGERPRINT_GESTURES_MIN_VERSION = 40

    /**
     * This is version 3.0.0-beta.1 when the assistant trigger was first introduced.
     */
    const val FLOATING_BUTTON_MIN_VERSION = 81
}
