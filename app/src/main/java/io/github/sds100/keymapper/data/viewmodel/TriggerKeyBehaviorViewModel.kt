package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.options.TriggerKeyOptions
import io.github.sds100.keymapper.util.Event

/**
 * Created by sds100 on 27/06/20.
 */
class TriggerKeyBehaviorViewModel : ViewModel() {

    companion object {
        private const val STATE_KEY = "state_trigger_behavior"
    }

    val options: MediatorLiveData<TriggerKeyOptions> = MediatorLiveData()

    val checkBoxModels = options.map {
        sequence {
            if (it.doNotConsumeKeyEvents.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.doNotConsumeKeyEvents.id,
                    label = R.string.flag_dont_override_default_action,
                    isChecked = it.doNotConsumeKeyEvents.value
                ))
            }
        }.toList()
    }

    val shortPress = MediatorLiveData<Boolean>().apply {
        addSource(options) {
            val newValue = it.clickType.value == Trigger.SHORT_PRESS

            if (value != newValue) {
                value = newValue
            }
        }
    }

    val longPress = MediatorLiveData<Boolean>().apply {
        addSource(options) {
            val newValue = it.clickType.value == Trigger.LONG_PRESS

            if (value != newValue) {
                value = newValue
            }
        }
    }

    val doublePress = MediatorLiveData<Boolean>().apply {
        addSource(options) {
            val newValue = it.clickType.value == Trigger.DOUBLE_PRESS

            if (value != newValue) {
                value = newValue
            }
        }
    }

    val onSaveEvent: MutableLiveData<Event<TriggerKeyOptions>> = MutableLiveData()

    init {
        options.addSource(shortPress) {
            if (it) {
                setValue(TriggerKeyOptions.ID_CLICK_TYPE, Trigger.SHORT_PRESS)
            }
        }

        options.addSource(longPress) {
            if (it) {
                setValue(TriggerKeyOptions.ID_CLICK_TYPE, Trigger.LONG_PRESS)
            }
        }

        options.addSource(doublePress) {
            if (it) {
                setValue(TriggerKeyOptions.ID_CLICK_TYPE, Trigger.DOUBLE_PRESS)
            }
        }
    }

    fun setValue(id: String, newValue: Boolean) {
        options.value = options.value?.setValue(id, newValue)
    }

    fun setValue(id: String, newValue: Int) {
        options.value = options.value?.setValue(id, newValue)
    }

    fun setBehavior(triggerKeyOptions: TriggerKeyOptions) {
        options.value = triggerKeyOptions
    }

    fun save() {
        onSaveEvent.value = Event(options.value!!)
    }

    fun saveState(outState: Bundle) {
        outState.putSerializable(STATE_KEY, options.value)
    }

    fun restoreState(state: Bundle) {
        val behavior = state.getSerializable(STATE_KEY) as TriggerKeyOptions
        setBehavior(behavior)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TriggerKeyBehaviorViewModel() as T
        }
    }
}