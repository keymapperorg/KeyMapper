package io.github.sds100.keymapper.keymaps

import android.os.Build
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FingerprintGesturesSupportedUseCaseImpl(
    private val preferenceRepository: PreferenceRepository,
) : FingerprintGesturesSupportedUseCase {
    override val isSupported: Flow<Boolean?> =
        preferenceRepository.get(Keys.fingerprintGesturesAvailable).map {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return@map false

            it
        }

    override fun setSupported(supported: Boolean) {
        preferenceRepository.set(Keys.fingerprintGesturesAvailable, supported)
    }
}

interface FingerprintGesturesSupportedUseCase {
    /**
     * Is null if support is unknown
     */
    val isSupported: Flow<Boolean?>

    fun setSupported(supported: Boolean)
}
