package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.SliderModel
import io.github.sds100.keymapper.data.model.options.BehaviorOption.Companion.nullIfDefault
import io.github.sds100.keymapper.data.model.options.FingerprintGestureMapOptions
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.Loading
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 18/11/20.
 */

class FingerprintGestureMapOptionsViewModel : ViewModel() {
    companion object {
        private const val STATE_KEY = "state_fingerprint_gesture_map_behavior"
    }

    private val _options: MutableLiveData<State<FingerprintGestureMapOptions>> = MutableLiveData(Loading())
    val options: LiveData<State<FingerprintGestureMapOptions>> = _options

    private val _onSave: MutableSharedFlow<FingerprintGestureMapOptions> = MutableSharedFlow()
    val onSave = _onSave.asSharedFlow()

    fun setOptions(behavior: FingerprintGestureMapOptions) {
        _options.value = Data(behavior)
    }

    val checkBoxModels = options.map {
        if (it !is Data) return@map listOf()

        return@map sequence {
            if (it.data.vibrate.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.data.vibrate.id,
                    label = R.string.flag_vibrate,
                    isChecked = it.data.vibrate.value
                ))
            }
        }.toList()
    }

    val sliderModels = options.map {
        if (it !is Data) return@map listOf()

        return@map sequence {
            if (it.data.vibrateDuration.isAllowed) {
                yield(SliderListItemModel(
                    id = it.data.vibrateDuration.id,
                    label = R.string.extra_label_vibration_duration,
                    sliderModel = SliderModel(
                        value = it.data.vibrateDuration.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.vibrate_duration_min,
                        maxSlider = R.integer.vibrate_duration_max,
                        stepSize = R.integer.vibrate_duration_step_size
                    )
                ))
            }

        }.toList()
    }

    fun setValue(id: String, newValue: Int) {
        options.value?.ifIsData {
            _options.value = Data(it.setValue(id, newValue))
        }
    }

    fun setValue(id: String, newValue: Boolean) {
        options.value?.ifIsData {
            _options.value = Data(it.setValue(id, newValue))
        }
    }

    fun save() {
        options.value?.ifIsData {
            viewModelScope.launch {
                _onSave.emit(it)
            }
        }
    }

    fun saveState(outState: Bundle) {
        options.value?.ifIsData {
            outState.putSerializable(STATE_KEY, it)
        }
    }

    fun restoreState(state: Bundle) {
        _options.value = Loading()

        val behavior = state.getSerializable(STATE_KEY) as FingerprintGestureMapOptions
        setOptions(behavior)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureMapOptionsViewModel() as T
        }
    }
}