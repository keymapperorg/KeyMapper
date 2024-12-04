package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 25/04/2021.
 */
object VersionHelper {
    const val VERSION_2_3_0 = 43

    /**
     * Use version code for 2.2.0.beta.2 because in beta 1 there were issues detecting the
     * availability of fingerprint gestures.
     */
    const val FINGERPRINT_GESTURES_MIN_VERSION = 40

    /**
     * This is version 2.7.0 when the assistant trigger was first introduced.
     */
    const val ASSISTANT_TRIGGER_MIN_VERSION = 66
}
