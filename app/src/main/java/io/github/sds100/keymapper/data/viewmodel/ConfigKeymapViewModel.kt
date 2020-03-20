package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class ConfigKeymapViewModel internal constructor(
    private val repository: KeymapRepository
) : ViewModel() {

    companion object {
        const val NEW_KEYMAP_ID = -2L
    }

    private var id by Delegates.notNull<Long>()

    val triggerKeys: MutableLiveData<List<Trigger.Key>> = MutableLiveData()
    val triggerInParallel: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerInSequence: MutableLiveData<Boolean> = MutableLiveData(false)
    val actionList: MutableLiveData<List<Action>> = MutableLiveData()
    val constraintList: MutableLiveData<List<Constraint>> = MutableLiveData()
    val constraintAndMode: MutableLiveData<Boolean> = MutableLiveData()
    val constraintOrMode: MutableLiveData<Boolean> = MutableLiveData()
    val flags: MutableLiveData<Int> = MutableLiveData()
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()

    fun init(keymapId: Long) {
        id = keymapId

        if (id == NEW_KEYMAP_ID) {
            triggerKeys.value = listOf()
            actionList.value = listOf()
            flags.value = 0
            isEnabled.value = true
            constraintList.value = listOf()

            when (Constraint.DEFAULT_MODE) {
                Constraint.MODE_AND -> {
                    constraintAndMode.value = true
                    constraintOrMode.value = false
                }

                Constraint.MODE_OR -> {
                    constraintOrMode.value = true
                    constraintAndMode.value = false
                }
            }

            when (Trigger.DEFAULT_TRIGGER_MODE) {
                Trigger.PARALLEL -> {
                    triggerInParallel.value = true
                    triggerInSequence.value = false
                }

                Trigger.SEQUENCE -> {
                    triggerInSequence.value = true
                    triggerInParallel.value = false
                }
            }

        } else {
            viewModelScope.launch {
                repository.getKeymap(id).let { keymap ->
                    triggerKeys.value = keymap.trigger.keys
                    actionList.value = keymap.actionList
                    flags.value = keymap.flags
                    isEnabled.value = keymap.isEnabled
                    constraintList.value = keymap.constraintList

                    when (keymap.constraintMode) {
                        Constraint.MODE_AND -> {
                            constraintAndMode.value = true
                            constraintOrMode.value = false
                        }

                        Constraint.MODE_OR -> {
                            constraintOrMode.value = true
                            constraintAndMode.value = false
                        }
                    }

                    when (keymap.trigger.mode) {
                        Trigger.PARALLEL -> {
                            triggerInParallel.value = true
                            triggerInSequence.value = false
                        }

                        Trigger.SEQUENCE -> {
                            triggerInSequence.value = true
                            triggerInParallel.value = false
                        }
                    }
                }
            }
        }
    }

    fun saveKeymap() {
        viewModelScope.launch {
            val triggerMode = when {
                triggerInParallel.value == true -> Trigger.PARALLEL
                triggerInSequence.value == true -> Trigger.SEQUENCE
                else -> Trigger.DEFAULT_TRIGGER_MODE
            }

            val constraintMode = when {
                constraintAndMode.value == true -> Constraint.MODE_AND
                constraintOrMode.value == true -> Constraint.MODE_OR
                else -> Constraint.DEFAULT_MODE
            }

            val actualId =
                if (id == NEW_KEYMAP_ID) {
                    0
                } else {
                    id
                }

            val keymap = KeyMap(
                id = actualId,
                trigger = Trigger(triggerKeys.value!!).apply { mode = triggerMode },
                actionList = actionList.value!!,
                constraintList = constraintList.value!!,
                constraintMode = constraintMode,
                flags = flags.value!!,
                isEnabled = isEnabled.value!!
            )

            if (id == NEW_KEYMAP_ID) {
                repository.createKeymap(keymap)
            } else {
                repository.updateKeymap(keymap)
            }
        }
    }

    class Factory(private val mRepository: KeymapRepository) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mRepository) as T
    }
}