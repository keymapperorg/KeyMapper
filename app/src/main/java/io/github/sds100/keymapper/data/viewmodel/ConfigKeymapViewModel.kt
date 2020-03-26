package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.buildChipModel
import io.github.sds100.keymapper.util.buildModel
import io.github.sds100.keymapper.util.toggleFlag
import kotlinx.coroutines.launch

class ConfigKeymapViewModel internal constructor(
    private val mRepository: KeymapRepository,
    private val mId: Long
) : ViewModel() {

    companion object {
        const val NEW_KEYMAP_ID = -2L
    }

    val triggerInParallel: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerInSequence: MutableLiveData<Boolean> = MutableLiveData(false)
    val constraintAndMode: MutableLiveData<Boolean> = MutableLiveData()
    val constraintOrMode: MutableLiveData<Boolean> = MutableLiveData()
    val flags: MutableLiveData<Int> = MutableLiveData()
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()

    val triggerKeys: MutableLiveData<List<Trigger.Key>> = MutableLiveData()

    private val mActionList: MutableLiveData<List<Action>> = MutableLiveData()

    val actionModelList: LiveData<List<ActionModel>> = Transformations.map(mActionList) { actionList ->
        sequence {
            actionList.forEach {
                yield(it.buildModel())
            }
        }.toList()
    }

    private val mConstraintList: MutableLiveData<List<Constraint>> = MutableLiveData()

    val constraintModelList: LiveData<List<ConstraintModel>> = Transformations.map(mConstraintList) { constraintList ->
        sequence {
            constraintList.forEach {
                yield(it.buildChipModel())
            }
        }.toList()
    }

    init {
        if (mId == NEW_KEYMAP_ID) {
            triggerKeys.value = listOf()
            mActionList.value = listOf()
            flags.value = 0
            isEnabled.value = true
            mConstraintList.value = listOf()

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
                mRepository.getKeymap(mId).let { keymap ->
                    triggerKeys.value = keymap.trigger.keys
                    mActionList.value = keymap.actionList
                    flags.value = keymap.flags
                    isEnabled.value = keymap.isEnabled
                    mConstraintList.value = keymap.constraintList

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
                if (mId == NEW_KEYMAP_ID) {
                    0
                } else {
                    mId
                }

            val keymap = KeyMap(
                id = actualId,
                trigger = Trigger(triggerKeys.value!!).apply { mode = triggerMode },
                actionList = mActionList.value!!,
                constraintList = mConstraintList.value!!,
                constraintMode = constraintMode,
                flags = flags.value!!,
                isEnabled = isEnabled.value!!
            )

            if (mId == NEW_KEYMAP_ID) {
                mRepository.createKeymap(keymap)
            } else {
                mRepository.updateKeymap(keymap)
            }
        }
    }

    fun toggleFlag(flagId: Int) {
        flags.value = flags.value?.toggleFlag(flagId)
    }

    fun removeConstraint(id: String) {
        mConstraintList.value = mConstraintList.value?.toMutableList()?.apply {
            removeAll { it.uniqueId == id }
        }
    }

    /**
     * @return whether the constraint already exists and has been added to the list
     */
    fun addConstraint(constraint: Constraint): Boolean {
        if (mConstraintList.value?.any { it.uniqueId == constraint.uniqueId } == true) {
            return false
        }

        mConstraintList.value = mConstraintList.value?.toMutableList()?.apply {
            add(constraint)
        }

        return true
    }

    class Factory(private val mRepository: KeymapRepository, private val mId: Long) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mRepository, mId) as T
    }
}