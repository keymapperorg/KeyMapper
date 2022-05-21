package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.mappings.DefaultMappingOptions

/**
 * Created by sds100 on 21/05/2022.
 */
data class DefaultFingerprintMapOptions(
    override val vibrateDuration: Long,
    override val forceVibrate: Boolean
) : DefaultMappingOptions {
    companion object {
        val DEFAULT: DefaultFingerprintMapOptions = DefaultFingerprintMapOptions(
            PreferenceDefaults.VIBRATION_DURATION.toLong(),
            PreferenceDefaults.FORCE_VIBRATE,
        )
    }
}