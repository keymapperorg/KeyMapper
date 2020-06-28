package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.ActionUtils
import io.github.sds100.keymapper.util.Event
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 27/06/20.
 */
class ActionOptionsViewModel : ViewModel() {

    companion object {
        private const val STATE_KEY = "state_choose_action_options"
    }

    val onSaveEvent: MutableLiveData<Event<ActionOptions>> = MutableLiveData()

    private val mActionId: MutableLiveData<String> = MutableLiveData()
    private val mFlags: MutableLiveData<Int> = MutableLiveData()
    private val mExtras: MutableLiveData<List<Extra>> = MutableLiveData()
    private val mAllowedFlags: MutableLiveData<List<Int>> = MutableLiveData()
    private val mAllowedExtras = mFlags.map {
        val allowedExtras = ActionUtils.allowedExtraIds(mFlags.value ?: 0)

        mExtras.value = mExtras.value?.toMutableList()?.apply {
            Action.EXTRAS.forEach { extraId ->
                if (allowedExtras.none { it == extraId }) {
                    removeAll { it.id == extraId }
                }
            }
        }

        allowedExtras
    }

    val checkBoxModels = MediatorLiveData<List<CheckBoxOption>>().apply {
        fun invalidate() {
            value = sequence {
                mAllowedFlags.value?.forEach { flagId ->
                    val isChecked = mFlags.value?.hasFlag(flagId) == true

                    yield(CheckBoxOption(flagId, isChecked))
                }
            }.toList()
        }

        addSource(mFlags) {
            invalidate()
        }

        addSource(mAllowedFlags) {
            invalidate()
        }
    }

    val sliderModels = MediatorLiveData<List<SliderOption>>().apply {
        fun invalidate() {
            value = sequence {
                mAllowedExtras.value?.forEach { extraId ->
                    val value = mExtras.value?.find { it.id == extraId }?.data?.toInt()

                    yield(SliderOption(extraId, value))
                }
            }.toList()
        }

        addSource(mAllowedExtras) {
            invalidate()
        }

        addSource(mExtras) {
            invalidate()
        }
    }

    fun toggleFlag(flagId: Int) {
        mFlags.value = if (mFlags.value?.hasFlag(flagId) == true) {
            mFlags.value?.minusFlag(flagId)
        } else {
            mFlags.value?.withFlag(flagId)
        }
    }

    fun setOptionValue(@ExtraId extraId: String, newValue: Int) {
        mExtras.value = mExtras.value?.toMutableList()?.apply {
            removeAll { it.id == extraId }

            if (newValue != ConfigKeymapViewModel.EXTRA_USE_DEFAULT) {
                add(Extra(extraId, newValue.toString()))
            }
        }
    }

    fun setInitialOptions(model: ChooseActionOptions) {
        reset()

        mActionId.value = model.actionId
        mFlags.value = model.currentFlags
        mAllowedFlags.value = model.allowedFlags

        mExtras.value = model.currentExtras
    }

    fun save() {
        val id = mActionId.value ?: return
        val flags = mFlags.value ?: return
        val extras = mExtras.value ?: return

        onSaveEvent.value = Event(ActionOptions(id, flags, extras))
        reset()
    }

    fun saveState(outState: Bundle) {
        val id = mActionId.value ?: return
        val flags = mFlags.value ?: return
        val allowedFlags = mAllowedFlags.value ?: return
        val extras = mExtras.value ?: return

        outState.putSerializable(STATE_KEY, ChooseActionOptions(id, flags, extras, allowedFlags))
    }

    fun restoreState(state: Bundle) {
        val model = state.getSerializable(STATE_KEY) as ChooseActionOptions
        setInitialOptions(model)
    }

    private fun reset() {
        mActionId.value = null
        mFlags.value = null
        mExtras.value = null
        mAllowedFlags.value = null
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ActionOptionsViewModel() as T
        }
    }
}