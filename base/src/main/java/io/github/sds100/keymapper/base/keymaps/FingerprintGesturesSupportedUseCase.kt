package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class FingerprintGesturesSupportedUseCaseImpl @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
) : FingerprintGesturesSupportedUseCase {
    override val isSupported: Flow<Boolean?> =
        preferenceRepository.get(Keys.fingerprintGesturesAvailable)

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
