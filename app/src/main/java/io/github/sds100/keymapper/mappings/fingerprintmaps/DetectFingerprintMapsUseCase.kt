package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.DetectMappingUseCase
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectFingerprintMapsUseCaseImpl(
    private val repository: FingerprintMapRepository,
    private val areSupportedUseCase: AreFingerprintGesturesSupportedUseCase,
    private val vibrator: VibratorAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val popupMessageAdapter: PopupMessageAdapter,
    private val resourceProvider: ResourceProvider
) : DetectFingerprintMapsUseCase {
    override val fingerprintMaps: Flow<List<FingerprintMap>> =
        repository.fingerprintMapList
            .mapNotNull { state ->
                if (state is State.Data) {
                    state.data
                } else {
                    null
                }
            }
            .map { entityList -> entityList.map { FingerprintMapEntityMapper.fromEntity(it) } }
            .flowOn(Dispatchers.Default)

    override val isSupported: Flow<Boolean?> = areSupportedUseCase.isSupported

    private val forceVibrate: Flow<Boolean> =
        preferenceRepository.get(Keys.forceVibrate).map { it ?: false }

    private val defaultVibrateDuration: Flow<Long> =
        preferenceRepository.get(Keys.defaultVibrateDuration)
            .map { it ?: PreferenceDefaults.VIBRATION_DURATION }
            .map { it.toLong() }

    override val defaultOptions: Flow<DefaultFingerprintMapOptions> =
        combine(defaultVibrateDuration, forceVibrate) { vibrateDuration, forceVibrate ->
            DefaultFingerprintMapOptions(vibrateDuration, forceVibrate)
        }

    override fun showTriggeredToast() {
        popupMessageAdapter.showPopupMessage(resourceProvider.getString(R.string.toast_triggered_keymap))
    }

    override fun setSupported(supported: Boolean) {
        areSupportedUseCase.setSupported(supported)
    }

    override fun vibrate(duration: Long) {
        vibrator.vibrate(duration)
    }
}

interface DetectFingerprintMapsUseCase : DetectMappingUseCase {
    val fingerprintMaps: Flow<List<FingerprintMap>>
    val defaultOptions: Flow<DefaultFingerprintMapOptions>

    val isSupported: Flow<Boolean?>
    fun setSupported(supported: Boolean)
}