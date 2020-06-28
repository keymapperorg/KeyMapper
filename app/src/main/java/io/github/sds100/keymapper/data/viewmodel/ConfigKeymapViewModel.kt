package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import java.util.*

class ConfigKeymapViewModel internal constructor(
    private val mKeymapRepository: KeymapRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository,
    onboardingState: IOnboardingState,
    private val mId: Long
) : ViewModel(), IOnboardingState by onboardingState {

    companion object {
        const val NEW_KEYMAP_ID = -2L
        const val EXTRA_USE_DEFAULT = -1
    }

    val triggerInParallel: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerInSequence: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerModeUndefined: MutableLiveData<Boolean> = MutableLiveData(false)

    val triggerMode: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(triggerInParallel) {
            if (it == true) {

                /* when the user first chooses to make parallel a trigger, show a dialog informing them that
                the order in which they list the keys is the order in which they will need to be held down.
                 */
                if (triggerKeys.value?.size!! > 1 &&
                    !getShownPrompt(R.string.key_pref_shown_parallel_trigger_order_dialog)) {

                    val notifyUser = NotifyUserModel(R.string.dialog_message_parallel_trigger_order) {
                        setShownPrompt(R.string.key_pref_shown_parallel_trigger_order_dialog)
                    }

                    showPrompt(notifyUser)
                }

                // set all the keys to a short press if coming from a sequence trigger
                // because they must all be the same click type and can't all be double pressed
                if (it == true && value == Trigger.SEQUENCE) {
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

                value = Trigger.PARALLEL
            }
        }

        addSource(triggerInSequence) {
            if (it == true) {
                value = Trigger.SEQUENCE

                if (triggerKeys.value?.size!! > 1 &&
                    !getShownPrompt(R.string.key_pref_shown_sequence_trigger_explanation_dialog)) {
                    val notifyUser = NotifyUserModel(R.string.dialog_message_sequence_trigger_explanation) {
                        setShownPrompt(R.string.key_pref_shown_sequence_trigger_explanation_dialog)
                    }

                    showPrompt(notifyUser)
                }
            }
        }

        addSource(triggerModeUndefined) {
            if (it == true) {
                value = Trigger.UNDEFINED

                triggerInSequence.value = false
                triggerInParallel.value = false
            }
        }
    }

    val triggerKeys: MutableLiveData<List<Trigger.Key>> = MutableLiveData(listOf())

    val triggerKeyModelList = MutableLiveData(listOf<TriggerKeyModel>())

    val buildTriggerKeyModelListEvent = triggerKeys.map {
        Event(it)
    }

    private val mTriggerExtras: MutableLiveData<List<Extra>> = MutableLiveData(listOf())

    val recordTriggerTimeLeft = MutableLiveData(0)
    val recordingTrigger = MutableLiveData(false)
    val startRecordingTriggerInService: MutableLiveData<Event<Unit>> = MutableLiveData()
    val stopRecordingTrigger: MutableLiveData<Event<Unit>> = MutableLiveData()
    val chooseParallelTriggerClickType: MutableLiveData<Event<Unit>> = MutableLiveData()

    private val mKeymapFlags: MutableLiveData<Int> = MutableLiveData(0)
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()

    val actionList: MutableLiveData<List<Action>> = MutableLiveData(listOf())
    val chooseAction: MutableLiveData<Event<Unit>> = MutableLiveData()
    val showFixPrompt: MutableLiveData<Event<Failure>> = MutableLiveData()
    val duplicateActionsEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val duplicateConstraintsEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val testAction: MutableLiveData<Event<Action>> = MutableLiveData()
    val chooseActionOptions: MutableLiveData<Event<ChooseActionOptions>> = MutableLiveData()

    val constraintList: MutableLiveData<List<Constraint>> = MutableLiveData(listOf())

    val constraintAndMode: MutableLiveData<Boolean> = MutableLiveData()
    val constraintOrMode: MutableLiveData<Boolean> = MutableLiveData()

    val showOnboardingPrompt = MediatorLiveData<Event<NotifyUserModel>>().apply {
        addSource(actionList) {
            if (!getShownPrompt(R.string.key_pref_showcase_action_list) && it.isNotEmpty()) {

                showPrompt(
                    NotifyUserModel(R.string.showcase_action_list) {
                        onboardingState.setShownPrompt(R.string.key_pref_showcase_action_list)
                    }
                )
            }
        }
    }

    val promptToEnableAccessibilityService: MutableLiveData<Event<Unit>> = MutableLiveData()

    val sliderOptions = MutableLiveData<List<SliderOption>>()
    val checkBoxOptions = MutableLiveData<List<CheckBoxOption>>()

    init {
        if (mId == NEW_KEYMAP_ID) {
            triggerKeys.value = listOf()
            actionList.value = listOf()
            mKeymapFlags.value = 0
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
                    triggerModeUndefined.value = false
                }

                Trigger.SEQUENCE -> {
                    triggerInSequence.value = true
                    triggerInParallel.value = false
                    triggerModeUndefined.value = false
                }

                Trigger.UNDEFINED -> {
                    triggerInSequence.value = false
                    triggerInParallel.value = false
                    triggerModeUndefined.value = true
                }
            }

            invalidateOptions()

        } else {
            viewModelScope.launch {
                mKeymapRepository.getKeymap(mId).let { keymap ->
                    triggerKeys.value = keymap.trigger.keys
                    mTriggerExtras.value = keymap.trigger.extras
                    actionList.value = keymap.actionList
                    mKeymapFlags.value = keymap.flags
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
                            triggerModeUndefined.value = false
                        }

                        Trigger.SEQUENCE -> {
                            triggerInSequence.value = true
                            triggerInParallel.value = false
                            triggerModeUndefined.value = false
                        }

                        Trigger.UNDEFINED -> {
                            triggerInSequence.value = false
                            triggerInParallel.value = false
                            triggerModeUndefined.value = true
                        }
                    }

                    invalidateOptions()
                }

                triggerMode.observeForever {
                    invalidateOptions()
                }
            }
        }
    }

    fun saveKeymap(scope: CoroutineScope) {
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
            trigger = Trigger(triggerKeys.value!!, mTriggerExtras.value!!).apply { mode = triggerMode.value!! },
            actionList = actionList.value!!,
            constraintList = constraintList.value!!,
            constraintMode = constraintMode,
            flags = mKeymapFlags.value!!,
            isEnabled = isEnabled.value!!
        )

        scope.launch {
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

            showPrompt(notifyUser)
        } else {
            chooseParallelTriggerClickType.value = Event(Unit)
        }
    }

    fun setParallelTriggerClickType(@Trigger.ClickType clickType: Int) {
        triggerKeys.value = triggerKeys.value?.map {
            it.clickType = clickType

            it
        }

        invalidateOptions()
    }

    fun setTriggerKeyClickType(keyCode: Int, @Trigger.ClickType clickType: Int) {
        triggerKeys.value = triggerKeys.value?.map {
            if (it.keyCode == keyCode) {
                it.clickType = clickType
            }

            it
        }

        invalidateOptions()
    }

    fun setTriggerKeyDevice(keyCode: Int, descriptor: String) {
        triggerKeys.value = triggerKeys.value?.map {
            if (it.keyCode == keyCode) {
                it.deviceId = descriptor
            }

            it
        }

        invalidateOptions()
    }

    /**
     * @return whether the key already exists has been added to the list
     */
    suspend fun addTriggerKey(keyCode: Int, deviceDescriptor: String, deviceName: String, isExternal: Boolean): Boolean {
        mDeviceInfoRepository.insertDeviceInfo(DeviceInfo(deviceDescriptor, deviceName))

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
            triggerModeUndefined.value = true
        }

        /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
        because this is what most users are expecting when they make a trigger with multiple keys */
        if (triggerKeys.value!!.size == 2) {
            triggerInParallel.value = true
        }

        invalidateOptions()

        return true
    }

    fun setTriggerExtraValue(@ExtraId id: String, value: Int) {
        mTriggerExtras.value = mTriggerExtras.value?.toMutableList()?.apply {
            removeAll { it.id == id }

            if (value != EXTRA_USE_DEFAULT) {
                add(Extra(id, value.toString()))
            }
        }

        invalidateOptions()
    }

    fun removeTriggerKey(keycode: Int) {
        triggerKeys.value = triggerKeys.value?.toMutableList()?.apply {
            removeAll { it.keyCode == keycode }
        }

        if (triggerKeys.value!!.size <= 1) {
            triggerModeUndefined.value = true
        }

        invalidateOptions()
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

        invalidateOptions()
    }

    fun recordTrigger() {
        if (!recordingTrigger.value!!) {
            startRecordingTriggerInService.value = Event(Unit)
        }
    }

    fun stopRecording() {
        if (recordingTrigger.value == true) {
            stopRecordingTrigger.value = Event(Unit)
        }
    }

    fun toggleFlag(flagId: Int) {
        if (flagId == KeyMap.KEYMAP_FLAG_SCREEN_OFF_TRIGGERS &&
            !getShownPrompt(R.string.key_pref_shown_screen_off_triggers_explanation)) {
            showPrompt(NotifyUserModel(R.string.showcase_screen_off_triggers) {
                setShownPrompt(R.string.key_pref_shown_screen_off_triggers_explanation)
            })
        }

        mKeymapFlags.value = mKeymapFlags.value?.toggleFlag(flagId)

        invalidateOptions()
    }

    fun removeConstraint(id: String) {
        constraintList.value = constraintList.value?.toMutableList()?.apply {
            removeAll { it.uniqueId == id }
        }

        invalidateOptions()
    }

    fun chooseAction() {
        chooseAction.value = Event(Unit)
    }

    /**
     * @return whether the action already exists has been added to the list
     */
    fun addAction(action: Action) {
        if (actionList.value?.find {
                it.type == action.type && it.data == action.data && it.dataExtraString == action.dataExtraString
            } != null) {
            duplicateActionsEvent.value = Event(Unit)
            return
        }

        actionList.value = actionList.value?.toMutableList()?.apply {
            add(action)
        }

        invalidateOptions()
    }

    fun setActionOptions(actionOptions: ActionOptions) {
        actionList.value = actionList.value?.map {
            if (it.uniqueId == actionOptions.actionId) {
                it.flags = actionOptions.flags
                it.extras.clear()
                it.extras.addAll(actionOptions.extras)
            }

            it
        }

        invalidateOptions()
    }

    fun onActionModelClick(model: ActionModel) {
        if (model.hasError) {
            showFixPrompt.value = Event(model.failure!!)
        } else {
            if (model.hasError) {
                showFixPrompt.value = Event(model.failure!!)
            } else {
                actionList.value?.single { it.uniqueId == model.id }?.let {
                    testAction.value = Event(it)
                }
            }
        }
    }

    fun removeAction(id: String) {
        actionList.value = actionList.value?.toMutableList()?.apply {
            removeAll { it.uniqueId == id }
        }

        invalidateOptions()
    }

    fun chooseActionOptions(id: String) {
        val action = actionList.value?.find { it.uniqueId == id } ?: return
        val model = ChooseActionOptions(id, action.flags, action.extras, allowedActionFlags(id))

        chooseActionOptions.value = Event(model)
    }

    /**
     * @return whether the constraint already exists and has been added to the list
     */
    fun addConstraint(constraint: Constraint) {
        if (constraintList.value?.any { it.uniqueId == constraint.uniqueId } == true) {
            duplicateConstraintsEvent.value = Event(Unit)
            return
        }

        constraintList.value = constraintList.value?.toMutableList()?.apply {
            add(constraint)
        }

        invalidateOptions()
    }

    fun rebuildActionModels() {
        actionList.value = actionList.value
    }

    fun rebuildConstraintModels() {
        constraintList.value = constraintList.value
    }

    private fun showPrompt(notifyUserModel: NotifyUserModel) {
        if (showOnboardingPrompt.value?.peekContent()?.message != notifyUserModel.message) {
            showOnboardingPrompt.value = Event(notifyUserModel)
        }
    }

    private fun allowedActionFlags(actionId: String): List<Int> = sequence {
        val action = actionList.value?.find { it.uniqueId == actionId } ?: return@sequence

        if (action.isVolumeAction && action.data != SystemAction.VOLUME_SHOW_DIALOG) {
            yield(Action.ACTION_FLAG_SHOW_VOLUME_UI)
        }

        yield(Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST)

        if (KeymapDetectionDelegate.performActionOnDown(triggerKeys.value!!, triggerMode.value!!)) {
            yield(Action.ACTION_FLAG_REPEAT)
        }
    }.toList()

    private fun allowedKeymapFlags(): IntArray {
        val allowedFlags = mutableListOf(KeyMap.KEYMAP_FLAG_VIBRATE)

        if (actionList.value?.isNotEmpty() == true) {
            if ((triggerKeys.value?.size == 1 || (triggerInParallel.value == true))
                && triggerKeys.value?.getOrNull(0)?.clickType == Trigger.LONG_PRESS) {
                allowedFlags.add(KeyMap.KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION)
            }

            //If all the keys can be detected when the screen is off
            if (triggerKeys.value?.isNotEmpty() == true
                && triggerKeys.value?.all {
                    KeyEventUtils.GET_EVENT_LABEL_TO_KEYCODE.containsValue(it.keyCode)
                } == true) {
                allowedFlags.add(KeyMap.KEYMAP_FLAG_SCREEN_OFF_TRIGGERS)
            }
        }

        return allowedFlags.toIntArray()
    }

    private fun allowedTriggerOptions(): Set<String> {
        val allowedExtras = mutableListOf<String>()

        if (triggerKeys.value?.any { it.clickType == Trigger.LONG_PRESS } == true) {
            allowedExtras.add(Trigger.EXTRA_LONG_PRESS_DELAY)
        }

        if (triggerKeys.value?.any { it.clickType == Trigger.DOUBLE_PRESS } == true) {
            allowedExtras.add(Trigger.EXTRA_DOUBLE_PRESS_DELAY)
        }

        if (!triggerKeys.value.isNullOrEmpty() && triggerKeys.value?.let { it.size > 1 } == true
            && triggerMode.value == Trigger.SEQUENCE) {
            allowedExtras.add(Trigger.EXTRA_SEQUENCE_TRIGGER_TIMEOUT)
        }

        if (mKeymapFlags.value?.hasFlag(KeyMap.KEYMAP_FLAG_VIBRATE) == true ||
            mKeymapFlags.value?.hasFlag(KeyMap.KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION) == true) {
            allowedExtras.add(Trigger.EXTRA_VIBRATION_DURATION)
        }

        return allowedExtras.toSet()
    }

    private fun removeDeniedFlags() {

        var newKeymapFlags = mKeymapFlags.value ?: 0

        KeyMap.KEYMAP_FLAG_LABEL_MAP.keys.forEach { flagId ->
            //remove the flag if it isn't allowed anymore
            if (newKeymapFlags.hasFlag(flagId) && !allowedKeymapFlags().contains(flagId)) {
                newKeymapFlags = newKeymapFlags.minusFlag(flagId)
            }
        }

        mKeymapFlags.value = newKeymapFlags
    }

    private fun removeDeniedTriggerOptions() {
        //remove all extras which aren't allowed
        mTriggerExtras.value = mTriggerExtras.value?.toMutableList()?.apply {
            val allowedOptions = allowedTriggerOptions()
            removeAll { extra -> allowedOptions.none { extra.id == it } }
        }
    }

    private fun removeDeniedActionOptions() {
        actionList.value = actionList.value?.toMutableList()?.apply {
            forEach { action ->
                val allowedFlags = allowedActionFlags(action.uniqueId)
                val allowedExtras = ActionUtils.allowedExtraIds(action.flags)

                Action.ACTION_FLAG_LABEL_MAP.keys.forEach { flagId ->
                    if (action.flags.hasFlag(flagId) && !allowedFlags.contains(flagId)) {
                        action.flags = action.flags.minusFlag(flagId)
                    }
                }

                action.extras.removeAll { extra -> allowedExtras.none { extra.id == it } }
            }
        }
    }

    private fun invalidateOptions() {
        removeDeniedFlags()
        removeDeniedTriggerOptions()
        removeDeniedActionOptions()

        val allowedFlags = allowedKeymapFlags()

        checkBoxOptions.value = sequence {
            KeyMap.KEYMAP_FLAG_LABEL_MAP.keys.forEach { flagId ->
                if (allowedFlags.contains(flagId)) {
                    val isChecked = mKeymapFlags.value?.hasFlag(flagId) == true

                    yield(CheckBoxOption(flagId, isChecked))
                }
            }
        }.toList()

        //Iterate over the list of ALL trigger extra IDs to keep the order consistent.
        val allowedTriggerOptions = allowedTriggerOptions()

        sliderOptions.value = sequence {
            Trigger.EXTRAS.forEach { extraId ->
                if (allowedTriggerOptions.contains(extraId)) {
                    val currentValue = mTriggerExtras.value?.find { extra -> extra.id == extraId }?.data?.toInt()

                    yield(SliderOption(extraId, currentValue))
                }
            }
        }.toList()
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