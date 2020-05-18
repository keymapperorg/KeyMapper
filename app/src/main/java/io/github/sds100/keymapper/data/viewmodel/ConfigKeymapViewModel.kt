package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.*
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.InputDeviceUtils
import io.github.sds100.keymapper.util.isExternalCompat
import io.github.sds100.keymapper.util.result.onSuccess
import io.github.sds100.keymapper.util.toggleFlag
import kotlinx.coroutines.launch
import java.util.*

class ConfigKeymapViewModel internal constructor(
    private val mKeymapRepository: KeymapRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository,
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

                triggerExtras.value = triggerExtras.value?.toMutableList()?.apply {
                    removeAll { extra -> extra.id == Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT }
                }

                // set all the keys to a short press because they must all be the same click type and
                // can't all be double pressed
                triggerKeys.value?.let { keys ->
                    if (keys.isEmpty()) {
                        return@let
                    }

                    triggerKeys.value = keys.map { key ->
                        key.clickType = Trigger.SHORT_PRESS
                        key
                    }
                }
            }
        }

        addSource(triggerInSequence) {
            triggerExtras.value = triggerExtras.value?.toMutableList()?.apply {
                if (none { extra -> extra.id == Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT }) {
                    add(Extra(
                        id = Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
                        data = Trigger.DEFAULT_TIMEOUT.toString()
                    ))
                }
            }

            if (it == true) {
                value = Trigger.SEQUENCE
            }
        }
    }

    val triggerKeys: MutableLiveData<List<Trigger.Key>> = MutableLiveData(listOf())

    val triggerKeyModelList = MutableLiveData(listOf<TriggerKeyModel>())
    val buildTriggerKeyModelListEvent = triggerKeys.map {
        Event(it)
    }

    val triggerExtras: MutableLiveData<List<Extra>> = MutableLiveData(listOf())

    val constraintAndMode: MutableLiveData<Boolean> = MutableLiveData()
    val constraintOrMode: MutableLiveData<Boolean> = MutableLiveData()
    val flags: MutableLiveData<Int> = MutableLiveData()
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()


    val actionList: MutableLiveData<List<Action>> = MutableLiveData(listOf())

    val constraintList: MutableLiveData<List<Constraint>> = MutableLiveData(listOf())

    init {
        if (mId == NEW_KEYMAP_ID) {
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
                mKeymapRepository.getKeymap(mId).let { keymap ->
                    triggerKeys.value = keymap.trigger.keys
                    triggerExtras.value = keymap.trigger.extras
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
                trigger = Trigger(triggerKeys.value!!, triggerExtras.value!!).apply { mode = triggerMode.value!! },
                actionList = actionList.value!!,
                constraintList = constraintList.value!!,
                constraintMode = constraintMode,
                flags = flags.value!!,
                isEnabled = isEnabled.value!!
            )

            if (mId == NEW_KEYMAP_ID) {
                mKeymapRepository.createKeymap(keymap)
            } else {
                mKeymapRepository.updateKeymap(keymap)
            }
        }
    }

    fun setParallelTriggerClickType(@Trigger.ClickType clickType: Int) {
        triggerKeys.value = triggerKeys.value?.map {
            it.clickType = clickType

            it
        }
    }

    fun setTriggerKeyClickType(keyCode: Int, @Trigger.ClickType clickType: Int) {
        triggerKeys.value = triggerKeys.value?.map {
            if (it.keyCode == keyCode) {
                it.clickType = clickType
            }

            it
        }
    }

    fun setTriggerKeyDevice(keyCode: Int, descriptor: String) {
        triggerKeys.value = triggerKeys.value?.map {
            if (it.keyCode == keyCode) {
                it.deviceId = descriptor
            }

            it
        }
    }

    /**
     * @return whether the key already exists has been added to the list
     */
    suspend fun addTriggerKey(keyEvent: KeyEvent): Boolean {
        val device = keyEvent.device
        val triggerKeyDeviceDescriptor = device.descriptor

        InputDeviceUtils.getName(triggerKeyDeviceDescriptor).onSuccess { deviceName ->
            mDeviceInfoRepository.createDeviceInfo(DeviceInfo(triggerKeyDeviceDescriptor, deviceName))
        }

        val containsKey = triggerKeys.value?.any {
            val sameKeyCode = keyEvent.keyCode == it.keyCode

            //if the key is not external, check whether a trigger key already exists for this device
            val sameDeviceId = if (
                (it.deviceId == Trigger.Key.DEVICE_ID_THIS_DEVICE
                    || it.deviceId == Trigger.Key.DEVICE_ID_ANY_DEVICE)
                && !device.isExternalCompat) {
                true

            } else {
                it.deviceId == triggerKeyDeviceDescriptor
            }

            sameKeyCode && sameDeviceId

        } ?: false

        if (containsKey) {
            return false
        }

        triggerKeys.value = triggerKeys.value?.toMutableList()?.apply {
            val triggerKey = Trigger.Key.fromKeyEvent(keyEvent)
            add(triggerKey)
        }

        if (triggerKeys.value!!.size <= 1) {
            triggerInSequence.value = true
        }

        return true
    }

    fun setTriggerExtra(id: String, data: String) {
        triggerExtras.value = triggerExtras.value?.toMutableList()?.apply {
            removeAll { it.id == id }
            add(Extra(
                id = id,
                data = data
            ))
        }
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    fun removeTriggerKey(keycode: Int) {
        triggerKeys.value = triggerKeys.value?.toMutableList()?.apply {
            removeAll { it.keyCode == keycode }
        }

        if (triggerKeys.value!!.size <= 1) {
            triggerInSequence.value = true
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
        constraintList.value = constraintList.value?.toMutableList()?.apply {
            removeAll { it.uniqueId == id }
        }
    }

    /**
     * @return whether the action already exists has been added to the list
     */
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
        if (constraintList.value?.any { it.uniqueId == constraint.uniqueId } == true) {
            return false
        }

        constraintList.value = constraintList.value?.toMutableList()?.apply {
            add(constraint)
        }

        return true
    }

    fun rebuildActionModels() {
        actionList.value = actionList.value
    }

    class Factory(
        private val mKeymapRepository: KeymapRepository,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mId: Long) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mKeymapRepository, mDeviceInfoRepository, mId) as T
    }
}