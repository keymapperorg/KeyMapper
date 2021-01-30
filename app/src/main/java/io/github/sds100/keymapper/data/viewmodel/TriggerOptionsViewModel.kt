package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.IGlobalPreferences
import io.github.sds100.keymapper.data.PreferenceKeys
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.model.options.BoolOption
import io.github.sds100.keymapper.data.model.options.IntOption
import io.github.sds100.keymapper.data.model.options.IntOption.Companion.nullIfDefault
import io.github.sds100.keymapper.data.model.options.TriggerOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.OkDialog
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 29/11/20.
 */
class TriggerOptionsViewModel(
    private val prefs: IGlobalPreferences,
    private val deviceInfoRepository: DeviceInfoRepository,
    val getTriggerKeys: () -> List<Trigger.Key>,
    val getTriggerMode: () -> Int,
    private val keymapUid: LiveData<String>
) : BaseOptionsViewModel<TriggerOptions>() {

    override val stateKey = "trigger_options_view_model"

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    override fun createSliderListItemModel(option: IntOption) = when (option.id) {

        TriggerOptions.ID_VIBRATE_DURATION -> SliderListItemModel(
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

        TriggerOptions.ID_LONG_PRESS_DELAY -> SliderListItemModel(
            id = option.id,
            label = R.string.extra_label_long_press_delay_timeout,
            sliderModel = SliderModel(
                value = option.value.nullIfDefault,
                isDefaultStepEnabled = true,
                min = R.integer.long_press_delay_min,
                maxSlider = R.integer.long_press_delay_max,
                stepSize = R.integer.long_press_delay_step_size
            )
        )

        TriggerOptions.ID_DOUBLE_PRESS_DELAY -> SliderListItemModel(
            id = option.id,
            label = R.string.extra_label_double_press_delay_timeout,
            sliderModel = SliderModel(
                value = option.value.nullIfDefault,
                isDefaultStepEnabled = true,
                min = R.integer.double_press_delay_min,
                maxSlider = R.integer.double_press_delay_max,
                stepSize = R.integer.double_press_delay_step_size
            )
        )

        TriggerOptions.ID_SEQUENCE_TRIGGER_TIMEOUT -> SliderListItemModel(
            id = option.id,
            label = R.string.extra_label_sequence_trigger_timeout,
            sliderModel = SliderModel(
                value = option.value.nullIfDefault,
                isDefaultStepEnabled = true,
                min = R.integer.sequence_trigger_timeout_min,
                maxSlider = R.integer.sequence_trigger_timeout_max,
                stepSize = R.integer.sequence_trigger_timeout_step_size
            )
        )

        else -> throw Exception("Don't know how to create a SliderListItemModel for this option $option.id")
    }

    override fun createCheckboxListItemModel(option: BoolOption) = when (option.id) {
        TriggerOptions.ID_VIBRATE -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_vibrate,
            isChecked = option.value
        )

        TriggerOptions.ID_SHOW_TOAST -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_show_toast,
            isChecked = option.value
        )

        TriggerOptions.ID_SCREEN_OFF_TRIGGER -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_detect_triggers_screen_off,
            isChecked = option.value
        )

        TriggerOptions.ID_LONG_PRESS_DOUBLE_VIBRATION -> CheckBoxListItemModel(
            id = option.id,
            label = R.string.flag_long_press_double_vibration,
            isChecked = option.value
        )

        else -> throw Exception("Don't know how to create a CheckboxListItemModel for this option $option.id")
    }

    private val _triggerFromOtherApps = MediatorLiveData<TriggerByIntentModel>().apply {
        value = null

        fun invalidate() {
            value = TriggerByIntentModel(
                keymapUid.value ?: return,
                options.value?.triggerFromOtherApps?.value ?: return
            )
        }

        addSource(keymapUid) {
            invalidate()
        }

        addSource(options) {
            invalidate()
        }
    }
    val triggerFromOtherApps: LiveData<TriggerByIntentModel> = _triggerFromOtherApps

    override fun setValue(id: String, newValue: Boolean) {
        super.setValue(id, newValue)

        invalidateOptions()
    }

    override fun setValue(id: String, newValue: Int) {
        super.setValue(id, newValue)

        if (id == TriggerOptions.ID_SCREEN_OFF_TRIGGER &&
            prefs.getFlow(PreferenceKeys.shownScreenOffTriggersExplanation).firstBlocking() == false) {

            _eventStream.value = OkDialog(R.string.showcase_screen_off_triggers) {
                prefs.set(PreferenceKeys.shownScreenOffTriggersExplanation, true)
            }
        }

        invalidateOptions()
    }

    fun invalidateOptions() {
        options.value?.apply {
            setOptions(dependentDataChanged(getTriggerKeys.invoke(), getTriggerMode.invoke()))
        }
    }

    suspend fun getDeviceInfoList() = deviceInfoRepository.getAll()
}