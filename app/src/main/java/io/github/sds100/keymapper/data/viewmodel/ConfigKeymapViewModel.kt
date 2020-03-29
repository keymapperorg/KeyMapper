package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.buildChipModel
import io.github.sds100.keymapper.util.buildModel
import io.github.sds100.keymapper.util.toggleFlag
import kotlinx.coroutines.launch
import java.util.*

class ConfigKeymapViewModel internal constructor(
    private val mRepository: KeymapRepository,
    private val mId: Long
) : ViewModel() {

    companion object {
        const val NEW_KEYMAP_ID = -2L
    }

    val triggerInParallel: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerInSequence: MutableLiveData<Boolean> = MutableLiveData(false)

    val triggerMode: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(triggerInParallel) {
            if (it == true) {
                value = Trigger.PARALLEL
            }
        }

        addSource(triggerInSequence) {
            if (it == true) {
                value = Trigger.SEQUENCE
            }
        }
    }

    val constraintAndMode: MutableLiveData<Boolean> = MutableLiveData()
    val constraintOrMode: MutableLiveData<Boolean> = MutableLiveData()
    val flags: MutableLiveData<Int> = MutableLiveData()
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()

    val triggerKeys: MutableLiveData<List<Trigger.Key>> = MutableLiveData()

    val triggerKeyModels: LiveData<List<TriggerKeyModel>> = Transformations.map(triggerKeys) { triggerKeys ->
        sequence {
            triggerKeys.forEach {
                yield(it.buildModel())
            }
        }.toList()
    }

    val actionList: MutableLiveData<List<Action>> = MutableLiveData()

    val actionModelList: LiveData<List<ActionModel>> = Transformations.map(actionList) { actionList ->
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
            actionList.value = listOf()
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
                    actionList.value = keymap.actionList
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
                trigger = Trigger(triggerKeys.value!!).apply { mode = triggerMode.value!! },
                actionList = actionList.value!!,
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

    @Trigger.ClickType
    fun getParallelTriggerClickType() = triggerKeys.value?.get(0)?.clickType

    fun setParallelTriggerClickType(@Trigger.ClickType clickType: Int) {
        triggerKeys.value = triggerKeys.value?.map {
            it.clickType = clickType

            it
        }
    }

    fun setTriggerKeyClickType(keycode: Int, @Trigger.ClickType clickType: Int) {
        triggerKeys.value = triggerKeys.value?.map {
            if (it.keyCode == keycode) {
                it.clickType = clickType
            }

            it
        }
    }

    fun removeTriggerKey(keycode: Int) {
        triggerKeys.value = triggerKeys.value?.toMutableList()?.apply {
            removeAll { it.keyCode == keycode }
        }
    }

    fun moveTriggerKey(fromIndex: Int, toIndex: Int) {
        triggerKeys.value = triggerKeys.value?.toMutableList()?.apply {
            if (fromIndex < toIndex) {
                for (i in fromIndex until toIndex) {
                    Collections.swap(this, i, i + 1)
                }
            } else {
                for (i in fromIndex downTo toIndex + 1) {
                    Collections.swap(this, i, i - 1)
                }
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

    fun addAction(action: Action): Boolean {
        if (actionList.value?.find { it.uniqueId == action.uniqueId } != null) {
            return false
        }

        actionList.value = actionList.value?.toMutableList()?.apply {
            add(action)
        }

        return true
    }

    fun setActionFlags(actionId: String, flags: Int) {
        actionList.value = actionList.value?.map {
            if (it.uniqueId == actionId) {
                it.flags = flags
            }

            it
        }
    }

    fun removeAction(id: String) {
        actionList.value = actionList.value?.toMutableList()?.apply {
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