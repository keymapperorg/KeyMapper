package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.OptionMinimums
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.CheckBoxListItem
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SliderListItem
import io.github.sds100.keymapper.util.ui.SliderMaximums
import io.github.sds100.keymapper.util.ui.SliderModel
import io.github.sds100.keymapper.util.ui.SliderStepSizes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 12/04/2021.
 */
class ConfigFingerprintMapOptionsViewModel(
    private val coroutineScope: CoroutineScope,
    private val config: ConfigFingerprintMapUseCase,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    companion object {
        private const val ID_VIBRATE_DURATION = "vibrate_duration"
        private const val ID_VIBRATE = "vibrate"
        private const val ID_SHOW_TOAST = "show_toast"
    }

    private val _state = MutableStateFlow(buildUiState(State.Loading))
    val state = _state.asStateFlow()

    init {
        coroutineScope.launch {
            config.mapping.collectLatest { mapping ->
                _state.value = withContext(Dispatchers.Default) {
                    buildUiState(mapping)
                }
            }
        }
    }

    fun setSliderValue(id: String, value: Defaultable<Int>) {
        when (id) {
            ID_VIBRATE_DURATION -> config.setVibrationDuration(value)
        }
    }

    fun setCheckboxValue(id: String, value: Boolean) {
        when (id) {
            ID_VIBRATE -> config.setVibrateEnabled(value)
            ID_SHOW_TOAST -> config.setShowToastEnabled(value)
        }
    }

    private fun buildUiState(configState: State<FingerprintMap>): State<List<ListItem>> =
        configState.mapData { fingerprintMap ->
            sequence {
                yield(
                    CheckBoxListItem(
                        id = ID_SHOW_TOAST,
                        isChecked = fingerprintMap.showToast,
                        label = getString(R.string.flag_show_toast),
                    ),
                )

                if (fingerprintMap.isVibrateAllowed()) {
                    yield(
                        CheckBoxListItem(
                            id = ID_VIBRATE,
                            isChecked = fingerprintMap.vibrate,
                            label = getString(R.string.flag_vibrate),
                        ),
                    )
                }

                if (fingerprintMap.isChangingVibrationDurationAllowed()) {
                    yield(
                        SliderListItem(
                            id = ID_VIBRATE_DURATION,
                            label = getString(R.string.extra_label_vibration_duration),
                            SliderModel(
                                value = Defaultable.create(fingerprintMap.vibrateDuration),
                                isDefaultStepEnabled = true,
                                min = OptionMinimums.VIBRATION_DURATION,
                                max = SliderMaximums.VIBRATION_DURATION,
                                stepSize = SliderStepSizes.VIBRATION_DURATION,
                            ),
                        ),
                    )
                }
            }.toList()
        }
}
