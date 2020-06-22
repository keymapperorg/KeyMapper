package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.KeyEventUtils
import io.github.sds100.keymapper.util.delegate.KeymapDetectionDelegate
import io.github.sds100.keymapper.util.repeatable
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.toggleFlag
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
        const val TRIGGER_EXTRA_USE_DEFAULT = -1
    }

    val triggerInParallel: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerInSequence: MutableLiveData<Boolean> = MutableLiveData(false)

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
    val chooseParallelTriggerClickType: MutableLiveData<Event<Unit>> = MutableLiveData()

    private val mKeymapFlags: MutableLiveData<Int> = MutableLiveData(0)
    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()

    val actionList: MutableLiveData<List<Action>> = MutableLiveData(listOf())
    val chooseAction: MutableLiveData<Event<Unit>> = MutableLiveData()
    val showFixPrompt: MutableLiveData<Event<Failure>> = MutableLiveData()
    val testAction: MutableLiveData<Event<Action>> = MutableLiveData()

    val constraintList: MutableLiveData<List<Constraint>> = MutableLiveData(listOf())

    val constraintAndMode: MutableLiveData<Boolean> = MutableLiveData()
    val constraintOrMode: MutableLiveData<Boolean> = MutableLiveData()

    val showOnboardingPrompt = MediatorLiveData<Event<NotifyUserModel>>().apply {
        addSource(actionList) {
            if (!getShownPrompt(R.string.key_pref_showcase_tap_action_to_test) && it.isNotEmpty()) {

                showPrompt(
                    NotifyUserModel(R.string.showcase_tap_action_to_test_message) {
                        onboardingState.setShownPrompt(R.string.key_pref_showcase_tap_action_to_test)
                    }
                )
            }
        }
    }
    val promptToEnableAccessibilityService: MutableLiveData<Event<Unit>> = MutableLiveData()

    val triggerOptions = MutableLiveData<List<TriggerOption>>()
    val keymapOptions = MutableLiveData<List<KeymapOption>>()

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
                }

                Trigger.SEQUENCE -> {
                    triggerInSequence.value = true
                    triggerInParallel.value = false
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
                        }

                        Trigger.SEQUENCE -> {
                            triggerInSequence.value = true
                            triggerInParallel.value = false
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
            triggerInSequence.value = true
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

            if (value != TRIGGER_EXTRA_USE_DEFAULT) {
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
            triggerInSequence.value = true
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
    fun addAction(action: Action): Boolean {
        if (actionList.value?.find { it.uniqueId == action.uniqueId } != null) {
            return false
        }

        actionList.value = actionList.value?.toMutableList()?.apply {
            add(action)
        }

        invalidateOptions()

        return true
    }

    fun setActionFlags(actionId: String, flags: Int) {
        actionList.value = actionList.value?.map {
            if (it.uniqueId == actionId) {
                it.flags = flags
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

        invalidateOptions()

        return true
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

    private fun allowedFlags(): IntArray {
        val allowedFlags = mutableListOf(KeyMap.KEYMAP_FLAG_VIBRATE)

        if (actionList.value?.isNotEmpty() == true) {
            allowedFlags.add(KeyMap.KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST)

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
            allowedExtras.add(Extra.EXTRA_LONG_PRESS_DELAY)
        }

        if (triggerKeys.value?.any { it.clickType == Trigger.DOUBLE_PRESS } == true) {
            allowedExtras.add(Extra.EXTRA_DOUBLE_PRESS_DELAY)
        }

        if (!triggerKeys.value.isNullOrEmpty() && triggerKeys.value?.let { it.size > 1 } == true
            && triggerMode.value == Trigger.SEQUENCE) {
            allowedExtras.add(Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT)
        }

        if (actionList.value?.any { it.repeatable } == true &&
            KeymapDetectionDelegate.performActionOnDown(triggerKeys.value!!, triggerMode.value!!)) {
            allowedExtras.add(Extra.EXTRA_HOLD_DOWN_DELAY)
            allowedExtras.add(Extra.EXTRA_REPEAT_DELAY)
        }

        if (mKeymapFlags.value?.hasFlag(KeyMap.KEYMAP_FLAG_VIBRATE) == true ||
            mKeymapFlags.value?.hasFlag(KeyMap.KEYMAP_FLAG_LONG_PRESS_DOUBLE_VIBRATION) == true) {
            allowedExtras.add(Extra.EXTRA_VIBRATION_DURATION)
        }

        return allowedExtras.toSet()
    }

    private fun removeDeniedFlags() {

        var newKeymapFlags = mKeymapFlags.value ?: 0

        KeyMap.KEYMAP_FLAG_LABEL_MAP.keys.forEach { flagId ->
            //remove the flag if it isn't allowed anymore
            if (newKeymapFlags.hasFlag(flagId) && !allowedFlags().contains(flagId)) {
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

    private fun invalidateOptions() {
        removeDeniedFlags()
        removeDeniedTriggerOptions()

        val allowedFlags = allowedFlags()

        keymapOptions.value = sequence {
            KeyMap.KEYMAP_FLAG_LABEL_MAP.keys.forEach { flagId ->
                if (allowedFlags.contains(flagId)) {
                    val enabled = mKeymapFlags.value?.hasFlag(flagId) == true

                    yield(flagId to enabled)
                }
            }
        }.toList()

        //Iterate over the list of ALL trigger extra IDs to keep the order consistent.
        val allowedTriggerOptions = allowedTriggerOptions()

        triggerOptions.value = sequence {
            Extra.TRIGGER_EXTRAS.forEach { extraId ->
                if (allowedTriggerOptions.contains(extraId)) {
                    val currentValue = mTriggerExtras.value?.find { extra -> extra.id == extraId }?.data?.toInt()

                    yield(TriggerOption(extraId, currentValue))
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

typealias TriggerOption = Pair<String, Int?>
typealias KeymapOption = Pair<Int, Boolean>