package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.SliderModel
import io.github.sds100.keymapper.data.model.options.BoolOption
import io.github.sds100.keymapper.data.model.options.FingerprintMapOptions
import io.github.sds100.keymapper.data.model.options.IntOption
import io.github.sds100.keymapper.data.model.options.IntOption.Companion.nullIfDefault

/**
 * Created by sds100 on 18/11/20.
 */

class FingerprintMapOptionsViewModel : BaseOptionsViewModel<FingerprintMapOptions>() {
    companion object {
        private const val STATE_KEY = "state_fingerprint_gesture_map_behavior"
    }

    override val stateKey = STATE_KEY

    override fun createSliderListItemModel(option: IntOption) = when (option.id) {
        FingerprintMapOptions.ID_VIBRATION_DURATION -> {
            SliderListItemModel(
                id = option.id,
                label = R.string.extra_label_vibration_duration,
                sliderModel = SliderModel(
                    value = option.value.nullIfDefault,
                    isDefaultStepEnabled = true,
                    min = R.integer.vibrate_duration_min,
                    maxSlider = R.integer.vibrate_duration_max,
                    stepSize = R.integer.vibrate_duration_step_size
                )
            )
        }
        else -> throw Exception(
            "Don't know how to create a SliderListItemModel for this option $option.id")
    }

    override fun createCheckboxListItemModel(option: BoolOption) = when (option.id) {
        FingerprintMapOptions.ID_VIBRATE -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_vibrate,
            isChecked = option.value
        )

        FingerprintMapOptions.ID_SHOW_TOAST -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_show_toast,
            isChecked = option.value
        )

        else -> throw Exception(
            "Don't know how to create a SliderListItemModel for this option $option.id")
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintMapOptionsViewModel() as T
        }
    }
}