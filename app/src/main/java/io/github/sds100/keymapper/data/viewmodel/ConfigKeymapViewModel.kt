package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.IPreferenceDataStore
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.options.KeymapActionOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.ConfigKeymapUseCase
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.EnableAccessibilityServicePrompt
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.FixFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by sds100 on 22/11/20.
 */

class ConfigKeymapViewModel(private val mKeymapRepository: ConfigKeymapUseCase,
                            private val mDeviceInfoRepository: DeviceInfoRepository,
                            preferenceDataStore: IPreferenceDataStore
) : ViewModel(), IPreferenceDataStore by preferenceDataStore {

    companion object {
        const val NEW_KEYMAP_ID = -2L
        private const val STATE_KEY = "config_keymap"
    }

    private var mId = NEW_KEYMAP_ID

    private val _uid = MutableLiveData<String>()
    val uid: LiveData<String> = _uid

    val actionListViewModel = object : ActionListViewModel<KeymapActionOptions>(viewModelScope, mDeviceInfoRepository) {
        override val stateKey = "keymap_action_list_view_model"

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
        mDeviceInfoRepository,
        preferenceDataStore = this,
        this.uid
    )

    private val supportedConstraints =
        Constraint.COMMON_SUPPORTED_CONSTRAINTS.toMutableList().apply {
            add(Constraint.SCREEN_ON)
            add(Constraint.SCREEN_OFF)
        }.toList()

    val constraintListViewModel = ConstraintListViewModel(viewModelScope, supportedConstraints)

    val isEnabled = MutableLiveData(false)

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(constraintListViewModel.eventStream) {
            when (it) {
                is FixFailure -> value = it
            }
        }

        addSource(actionListViewModel.eventStream) {
            when (it) {
                is FixFailure, is EnableAccessibilityServicePrompt -> value = it
            }
        }

        addSource(triggerViewModel.eventStream) {
            when (it) {
                is FixFailure, is EnableAccessibilityServicePrompt -> value = it
            }
        }
    }

    val eventStream: LiveData<Event> = _eventStream

    init {
        actionListViewModel.setActionList(emptyList())
        triggerViewModel.setTrigger(Trigger())
        constraintListViewModel.setConstraintList(emptyList(), Constraint.DEFAULT_MODE)
        isEnabled.value = true
        _uid.value = UUID.randomUUID().toString()

        triggerViewModel.mode.observeForever {
            actionListViewModel.invalidateOptions()
        }

        triggerViewModel.keys.observeForever {
            actionListViewModel.invalidateOptions()
        }
    }

    fun saveKeymap(scope: CoroutineScope) {
        val keymap = createKeymap()

        scope.launch {
            if (mId == NEW_KEYMAP_ID) {
                mKeymapRepository.insertKeymap(keymap.copy(id = 0))
            } else {
                mKeymapRepository.updateKeymap(keymap)
            }
        }
    }

    private fun createKeymap(): KeyMap {
        val trigger = triggerViewModel.createTrigger()

        return KeyMap(
            id = mId,
            trigger = trigger ?: Trigger(),
            actionList = actionListViewModel.actionList.value ?: listOf(),
            constraintList = constraintListViewModel.constraintList.value ?: listOf(),
            constraintMode = constraintListViewModel.getConstraintMode(),
            isEnabled = isEnabled.value ?: true,
            uid = uid.value ?: UUID.randomUUID().toString()
        )
    }

    fun loadKeymap(id: Long) {
        if (id == NEW_KEYMAP_ID) return

        viewModelScope.launch {
            val keymap = mKeymapRepository.getKeymap(id)
            loadKeymap(keymap)
        }
    }

    private fun loadKeymap(keymap: KeyMap) {
        mId = keymap.id

        /* this must be before everything else because action options might be deselected if
        * there is no trigger. issue #593 */
        triggerViewModel.setTrigger(keymap.trigger)

        actionListViewModel.setActionList(keymap.actionList)
        constraintListViewModel.setConstraintList(keymap.constraintList, keymap.constraintMode)
        isEnabled.value = keymap.isEnabled
        _uid.value = keymap.uid
    }

    fun saveState(outState: Bundle) {
        outState.putParcelable(STATE_KEY, createKeymap())
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreState(state: Bundle) {
        state.getParcelable<KeyMap>(STATE_KEY)?.let {
            loadKeymap(it)
        }
    }

    class Factory(
        private val mConfigKeymapUseCase: ConfigKeymapUseCase,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mIPreferenceDataStore: IPreferenceDataStore) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mConfigKeymapUseCase, mDeviceInfoRepository, mIPreferenceDataStore) as T
    }
}