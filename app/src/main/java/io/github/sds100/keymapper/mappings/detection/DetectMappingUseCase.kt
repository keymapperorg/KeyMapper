package io.github.sds100.keymapper.mappings.detection

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.popup.ToastAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectMappingUseCaseImpl @Inject constructor(
    private val vibrator: VibratorAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val popupMessageAdapter: ToastAdapter,
    private val resourceProvider: ResourceProvider
) : DetectMappingUseCase {

    override val forceVibrate: Flow<Boolean> =
        preferenceRepository.get(Keys.forceVibrate).map { it ?: false }

    override val defaultVibrateDuration: Flow<Long> =
        preferenceRepository.get(Keys.defaultVibrateDuration)
            .map { it ?: PreferenceDefaults.VIBRATION_DURATION }
            .map { it.toLong() }

    override fun showTriggeredToast() {
        popupMessageAdapter.showPopupMessage(resourceProvider.getString(R.string.toast_triggered_keymap))
    }

    override fun vibrate(duration: Long) {
        vibrator.vibrate(duration)
    }
}

interface DetectMappingUseCase {
    val forceVibrate: Flow<Boolean>
    val defaultVibrateDuration: Flow<Long>

    fun showTriggeredToast()
    fun vibrate(duration: Long)
}