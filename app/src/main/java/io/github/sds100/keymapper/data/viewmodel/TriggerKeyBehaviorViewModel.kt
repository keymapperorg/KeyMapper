package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.TriggerKeyBehavior
import io.github.sds100.keymapper.util.Event

/**
 * Created by sds100 on 27/06/20.
 */
class TriggerKeyBehaviorViewModel : ViewModel() {

    companion object {
        private const val STATE_KEY = "state_trigger_behavior"
    }

    val behavior: MediatorLiveData<TriggerKeyBehavior> = MediatorLiveData()

    val checkBoxModels = behavior.map {
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
        addSource(behavior) {
            val newValue = it.clickType.value == Trigger.SHORT_PRESS

            if (value != newValue) {
                value = newValue
            }
        }
    }

    val longPress = MediatorLiveData<Boolean>().apply {
        addSource(behavior) {
            val newValue = it.clickType.value == Trigger.LONG_PRESS

            if (value != newValue) {
                value = newValue
            }
        }
    }

    val doublePress = MediatorLiveData<Boolean>().apply {
        addSource(behavior) {
            val newValue = it.clickType.value == Trigger.DOUBLE_PRESS

            if (value != newValue) {
                value = newValue
            }
        }
    }

    val onSaveEvent: MutableLiveData<Event<TriggerKeyBehavior>> = MutableLiveData()

    init {
        behavior.addSource(shortPress) {
            if (it) {
                setValue(TriggerKeyBehavior.ID_CLICK_TYPE, Trigger.SHORT_PRESS)
            }
        }

        behavior.addSource(longPress) {
            if (it) {
                setValue(TriggerKeyBehavior.ID_CLICK_TYPE, Trigger.LONG_PRESS)
            }
        }

        behavior.addSource(doublePress) {
            if (it) {
                setValue(TriggerKeyBehavior.ID_CLICK_TYPE, Trigger.DOUBLE_PRESS)
            }
        }
    }

    fun setValue(id: String, newValue: Boolean) {
        behavior.value = behavior.value?.setValue(id, newValue)
    }

    fun setValue(id: String, newValue: Int) {
        behavior.value = behavior.value?.setValue(id, newValue)
    }

    fun setBehavior(triggerKeyBehavior: TriggerKeyBehavior) {
        behavior.value = triggerKeyBehavior
    }

    fun save() {
        onSaveEvent.value = Event(behavior.value!!)
    }

    fun saveState(outState: Bundle) {
        outState.putSerializable(STATE_KEY, behavior.value)
    }

    fun restoreState(state: Bundle) {
        val behavior = state.getSerializable(STATE_KEY) as TriggerKeyBehavior
        setBehavior(behavior)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TriggerKeyBehaviorViewModel() as T
        }
    }
}