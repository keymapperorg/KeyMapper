package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.IPreferenceDataStore
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.options.KeymapActionOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.ConfigKeymapUseCase
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 22/11/20.
 */

class ConfigKeymapViewModel(private val mKeymapRepository: ConfigKeymapUseCase,
                            private val mDeviceInfoRepository: DeviceInfoRepository,
                            preferenceDataStore: IPreferenceDataStore,
                            private val mId: Long
) : ViewModel(), IPreferenceDataStore by preferenceDataStore {

    companion object {
        const val NEW_KEYMAP_ID = -2L
    }

    val actionListViewModel = object : ActionListViewModel<KeymapActionOptions>(viewModelScope, mDeviceInfoRepository) {
        override fun getActionOptions(action: Action): KeymapActionOptions {
            return KeymapActionOptions(
                action,
                actionList.value!!.size,
                triggerViewModel.mode.value,
                triggerViewModel.keys.value
            )
        }

        override fun onAddAction(action: Action) {
            if (action.type == ActionType.KEY_EVENT) {
                getActionOptions(action).apply {
                    setValue(KeymapActionOptions.ID_REPEAT, true)

                    setOptions(this)
                }
            }
        }
    }

    val triggerViewModel = TriggerViewModel(
        viewModelScope,
        mDeviceInfoRepository,
        preferenceDataStore = this
    )

    val isEnabled = MutableLiveData<Boolean>()

    private val _showFixPrompt = MutableSharedFlow<Failure>()
    val showFixPrompt = _showFixPrompt.asSharedFlow()

    init {
        viewModelScope.launch {
            if (mId == NEW_KEYMAP_ID) {
                actionListViewModel.setActionList(listOf())
                triggerViewModel.setTrigger(Trigger())
                isEnabled.value = true

            } else {
                val keymap = mKeymapRepository.getKeymap(mId)

                actionListViewModel.setActionList(keymap.actionList)
                triggerViewModel.setTrigger(keymap.trigger)
                isEnabled.value = keymap.isEnabled
            }

            triggerViewModel.mode.observeForever {
                actionListViewModel.invalidateOptions()
            }

            triggerViewModel.keys.observeForever {
                actionListViewModel.invalidateOptions()
            }

            actionListViewModel.showFixPrompt.collect {
                _showFixPrompt.emit(it)
            }
        }
    }

    fun saveKeymap(scope: CoroutineScope) {
        //TODO
//        val constraintMode = when {
//            constraintAndMode.value == true -> Constraint.MODE_AND
//            constraintOrMode.value == true -> Constraint.MODE_OR
//            else -> Constraint.DEFAULT_MODE
//        }

        val actualId =
            if (mId == NEW_KEYMAP_ID) {
                0
            } else {
                mId
            }

        val trigger = triggerViewModel.createTrigger()

        val keymap = KeyMap(
            id = actualId,
            trigger = trigger ?: Trigger(),
            actionList = actionListViewModel.actionList.value ?: listOf(),
            constraintList = listOf(),
            constraintMode = Constraint.DEFAULT_MODE,
            isEnabled = isEnabled.value ?: true
        )

        scope.launch {
            if (mId == NEW_KEYMAP_ID) {
                mKeymapRepository.insertKeymap(keymap)
            } else {
                mKeymapRepository.updateKeymap(keymap)
            }
        }
    }

    class Factory(
        private val mConfigKeymapUseCase: ConfigKeymapUseCase,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mIPreferenceDataStore: IPreferenceDataStore,
        private val mId: Long) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mConfigKeymapUseCase, mDeviceInfoRepository, mIPreferenceDataStore, mId) as T
    }
}