package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.toggleFlag
import kotlinx.coroutines.launch
import java.util.*

class ConfigKeymapViewModel internal constructor(
    private val mKeymapRepository: KeymapRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository,
    onboardingState: IOnboardingState,
    private val mId: Long
) : ViewModel(), IOnboardingState by onboardingState {

    companion object {
        const val NEW_KEYMAP_ID = -2L
    }

    val triggerInParallel: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerInSequence: MutableLiveData<Boolean> = MutableLiveData(false)

    val triggerMode: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(triggerInParallel) {
            if (it == true) {
                value = Trigger.PARALLEL

                /* when the user first chooses to make parallel a trigger, show a dialog informing them that
                the order in which they list the keys is the order in which they will need to be held down.
                 */
                if (triggerKeys.value?.size!! > 1 &&
                    !getShownPrompt(R.string.key_pref_shown_parallel_trigger_order_dialog)) {

                    val notifyUser = NotifyUserModel(R.string.dialog_message_parallel_trigger_order) {
                        setShownPrompt(R.string.key_pref_shown_parallel_trigger_order_dialog)
                    }

                    showOnboardingPrompt.value = Event(notifyUser)
                }

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

                if (triggerKeys.value?.size!! > 1 &&
                    !getShownPrompt(R.string.key_pref_shown_sequence_trigger_explanation_dialog)) {
                    val notifyUser = NotifyUserModel(R.string.dialog_message_sequence_trigger_explanation) {
                        setShownPrompt(R.string.key_pref_shown_sequence_trigger_explanation_dialog)
                    }

                    showOnboardingPrompt.value = Event(notifyUser)

                }
            }
        }
    }

    val triggerKeys: MutableLiveData<List<Trigger.Key>> = MutableLiveData(listOf())

    val triggerKeyModelList = MutableLiveData(listOf<TriggerKeyModel>())

    val buildTriggerKeyModelListEvent = triggerKeys.map {
        Event(it)
    }

    val triggerExtras: MutableLiveData<List<Extra>> = MutableLiveData(listOf())
    val chooseTriggerTimeout: MutableLiveData<Event<Long>> = MutableLiveData()

    val recordTriggerTimeLeft = MutableLiveData(0)
    val recordingTrigger = MutableLiveData(false)
    val startRecordingTriggerInService: MutableLiveData<Event<Unit>> = MutableLiveData()
    val chooseParallelTriggerClickType: MutableLiveData<Event<Unit>> = MutableLiveData()

    val flags: MutableLiveData<Int> = MutableLiveData()
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()

    val actionList: MutableLiveData<List<Action>> = MutableLiveData(listOf())
    val chooseAction: MutableLiveData<Event<Unit>> = MutableLiveData()
    val showFixActionPrompt: MutableLiveData<Event<Failure>> = MutableLiveData()

    val constraintList: MutableLiveData<List<Constraint>> = MutableLiveData(listOf())

    val constraintAndMode: MutableLiveData<Boolean> = MutableLiveData()
    val constraintOrMode: MutableLiveData<Boolean> = MutableLiveData()

    val showOnboardingPrompt: MutableLiveData<Event<NotifyUserModel>> = MutableLiveData()

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

        viewModelScope.launch {
            if (mId == NEW_KEYMAP_ID) {
                mKeymapRepository.insertKeymap(keymap)
            } else {
                mKeymapRepository.updateKeymap(keymap)
            }
        }
    }

    fun chooseParallelTriggerClickType() {
        if (!getShownPrompt(R.string.key_pref_shown_double_press_restriction_warning)
            && triggerInParallel.value == true) {
            val notifyUser = NotifyUserModel(R.string.dialog_message_double_press_restricted_to_single_key) {
                setShownPrompt(R.string.key_pref_shown_double_press_restriction_warning)

                chooseParallelTriggerClickType.value = Event(Unit)
            }

            showOnboardingPrompt.value = Event(notifyUser)
        } else {
            chooseParallelTriggerClickType.value = Event(Unit)
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
    suspend fun addTriggerKey(keyCode: Int, deviceDescriptor: String, deviceName: String, isExternal: Boolean): Boolean {
        mDeviceInfoRepository.createDeviceInfo(DeviceInfo(deviceDescriptor, deviceName))

        val containsKey = triggerKeys.value?.any {
            val sameKeyCode = keyCode == it.keyCode

            //if the key is not external, check whether a trigger key already exists for this device
            val sameDeviceId = if (
                (it.deviceId == Trigger.Key.DEVICE_ID_THIS_DEVICE
                    || it.deviceId == Trigger.Key.DEVICE_ID_ANY_DEVICE)
                && !isExternal) {
                true

            } else {
                it.deviceId == deviceDescriptor
            }

            sameKeyCode && sameDeviceId

        } ?: false

        if (containsKey) {
            return false
        }

        triggerKeys.value = triggerKeys.value?.toMutableList()?.apply {
            val deviceId = if (isExternal) {
                deviceDescriptor
            } else {
                Trigger.Key.DEVICE_ID_THIS_DEVICE
            }

            val triggerKey = Trigger.Key(keyCode, deviceId)
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

    fun chooseTriggerTimeout() {
        triggerExtras.value?.single { it.id == Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT }?.let {
            chooseTriggerTimeout.value = Event(it.data.toLong())
        }
    }

    fun recordTrigger() {
        if (!recordingTrigger.value!!) {
            startRecordingTriggerInService.value = Event(Unit)
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

    fun chooseAction() {
        chooseAction.value = Event(Unit)
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

    fun onActionModelClick(model: ActionModel) {
        if (model.hasError) {
            showFixActionPrompt.value = Event(model.failure!!)
        } else {
            //TEST the action
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

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    class Factory(
        private val mKeymapRepository: KeymapRepository,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mIOnboardingState: IOnboardingState,
        private val mId: Long) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mKeymapRepository, mDeviceInfoRepository, mIOnboardingState, mId) as T
    }
}