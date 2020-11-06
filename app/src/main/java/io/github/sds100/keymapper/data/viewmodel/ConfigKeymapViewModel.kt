package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.model.behavior.ActionBehavior
import io.github.sds100.keymapper.data.model.behavior.BehaviorOption.Companion.nullIfDefault
import io.github.sds100.keymapper.data.model.behavior.TriggerBehavior
import io.github.sds100.keymapper.data.model.behavior.TriggerKeyBehavior
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.ConfigKeymapUseCase
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.dataExtraString
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

class ConfigKeymapViewModel internal constructor(
    private val mKeymapRepository: ConfigKeymapUseCase,
    private val mDeviceInfoRepository: DeviceInfoRepository,
    onboardingState: IOnboardingState,
    private val mId: Long
) : ViewModel(), IOnboardingState by onboardingState {

    companion object {
        const val NEW_KEYMAP_ID = -2L
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

                // set all the keys to a short press if coming from a non-parallel trigger
                // because they must all be the same click type and can't all be double pressed
                if (it == true && value != null && value != Trigger.PARALLEL) {
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

    val triggerBehavior: MutableLiveData<TriggerBehavior> = MutableLiveData()

    val isParallelTriggerClickTypeShortPress = triggerKeys.map {
        if (!it.isNullOrEmpty()) {
            it[0].clickType == Trigger.SHORT_PRESS
        } else {
            false
        }
    }

    val isParallelTriggerClickTypeLongPress = triggerKeys.map {
        if (!it.isNullOrEmpty()) {
            it[0].clickType == Trigger.LONG_PRESS
        } else {
            false
        }
    }

    val recordTriggerTimeLeft = MutableLiveData(0)
    val recordingTrigger = MutableLiveData(false)
    val startRecordingTriggerInService: MutableLiveData<Event<Unit>> = MutableLiveData()
    val stopRecordingTrigger: MutableLiveData<Event<Unit>> = MutableLiveData()

    val isEnabled: MutableLiveData<Boolean> = MutableLiveData()
    val actionList: MutableLiveData<List<Action>> = MutableLiveData(listOf())
    val chooseAction: MutableLiveData<Event<Unit>> = MutableLiveData()
    val showFixPrompt: MutableLiveData<Event<Failure>> = MutableLiveData()
    val duplicateActionsEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val duplicateConstraintsEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val testAction: MutableLiveData<Event<Action>> = MutableLiveData()
    val chooseActionBehavior: MutableLiveData<Event<ActionBehavior>> = MutableLiveData()
    val editTriggerKeyBehavior: MutableLiveData<Event<TriggerKeyBehavior>> = MutableLiveData()

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

    val promptToEnableCapsLockKeyboardLayout: MutableLiveData<Event<Unit>> = MutableLiveData()
    val promptToEnableAccessibilityService: MutableLiveData<Event<Unit>> = MutableLiveData()

    val sliderModels = triggerBehavior.map {
        it ?: return@map listOf<SliderListItemModel>()

        sequence {
            if (it.vibrateDuration.isAllowed) {
                yield(SliderListItemModel(
                    id = it.vibrateDuration.id,
                    label = R.string.extra_label_vibration_duration,
                    sliderModel = SliderModel(
                        value = it.vibrateDuration.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.vibrate_duration_min,
                        maxSlider = R.integer.vibrate_duration_max,
                        stepSize = R.integer.vibrate_duration_step_size
                    )
                ))
            }

            if (it.longPressDelay.isAllowed) {
                yield(SliderListItemModel(
                    id = it.longPressDelay.id,
                    label = R.string.extra_label_long_press_delay_timeout,
                    sliderModel = SliderModel(
                        value = it.longPressDelay.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.long_press_delay_min,
                        maxSlider = R.integer.long_press_delay_max,
                        stepSize = R.integer.long_press_delay_step_size
                    )
                ))
            }

            if (it.doublePressDelay.isAllowed) {
                yield(SliderListItemModel(
                    id = it.doublePressDelay.id,
                    label = R.string.extra_label_double_press_delay_timeout,
                    sliderModel = SliderModel(
                        value = it.doublePressDelay.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.double_press_delay_min,
                        maxSlider = R.integer.double_press_delay_max,
                        stepSize = R.integer.double_press_delay_step_size
                    )
                ))
            }

            if (it.sequenceTriggerTimeout.isAllowed) {
                yield(SliderListItemModel(
                    id = it.sequenceTriggerTimeout.id,
                    label = R.string.extra_label_sequence_trigger_timeout,
                    sliderModel = SliderModel(
                        value = it.sequenceTriggerTimeout.value.nullIfDefault,
                        isDefaultStepEnabled = true,
                        min = R.integer.sequence_trigger_timeout_min,
                        maxSlider = R.integer.sequence_trigger_timeout_max,
                        stepSize = R.integer.sequence_trigger_timeout_step_size
                    )
                ))
            }
        }.toList()
    }

    val checkBoxModels = triggerBehavior.map {

        it ?: return@map listOf<CheckBoxListItemModel>()

        sequence {
            if (it.vibrate.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.vibrate.id,
                    label = R.string.flag_vibrate,
                    isChecked = it.vibrate.value
                ))
            }

            if (it.screenOffTrigger.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.screenOffTrigger.id,
                    label = R.string.flag_detect_triggers_screen_off,
                    isChecked = it.screenOffTrigger.value
                ))
            }

            if (it.longPressDoubleVibration.isAllowed) {
                yield(CheckBoxListItemModel(
                    id = it.longPressDoubleVibration.id,
                    label = R.string.flag_long_press_double_vibration,
                    isChecked = it.longPressDoubleVibration.value
                ))
            }

        }.toList()
    }

    init {
        if (mId == NEW_KEYMAP_ID) {
            triggerKeys.value = listOf()
            actionList.value = listOf()
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

            triggerBehavior.value = TriggerBehavior(
                listOf(),
                Trigger.DEFAULT_TRIGGER_MODE,
                Trigger.DEFAULT_FLAGS,
                listOf()
            )

            invalidateOptions()

        } else {
            viewModelScope.launch {
                mKeymapRepository.getKeymap(mId).let { keymap ->
                    triggerKeys.value = keymap.trigger.keys
                    actionList.value = keymap.actionList
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

                    triggerBehavior.value = TriggerBehavior(
                        keymap.trigger.keys,
                        keymap.trigger.mode,
                        keymap.trigger.flags,
                        keymap.trigger.extras
                    )

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

        val trigger =
            triggerBehavior.value!!.applyToTrigger(Trigger(keys = triggerKeys.value!!, mode = triggerMode.value!!))

        val keymap = KeyMap(
            id = actualId,
            trigger = trigger,
            actionList = actionList.value!!,
            constraintList = constraintList.value!!,
            constraintMode = constraintMode,
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

    fun setParallelTriggerClickType(@Trigger.ClickType clickType: Int) {
        triggerKeys.value = triggerKeys.value?.map {
            it.clickType = clickType

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

        if (keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            promptToEnableCapsLockKeyboardLayout.value = Event(Unit)
        }

        invalidateOptions()

        return true
    }

    fun setTriggerOption(id: String, newValue: Int) {
        triggerBehavior.value = triggerBehavior.value?.setValue(id, newValue)
        invalidateOptions()
    }

    fun setTriggerOption(id: String, newValue: Boolean) {

        if (id == TriggerBehavior.ID_SCREEN_OFF_TRIGGER &&
            !getShownPrompt(R.string.key_pref_shown_screen_off_triggers_explanation)) {
            showPrompt(NotifyUserModel(R.string.showcase_screen_off_triggers) {
                setShownPrompt(R.string.key_pref_shown_screen_off_triggers_explanation)
            })
        }

        triggerBehavior.value = triggerBehavior.value?.setValue(id, newValue)
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

        if (action.type == ActionType.KEY_EVENT) {
            ActionBehavior(action, actionList.value!!.size, triggerMode.value!!, triggerKeys.value!!).apply {
                setValue(ActionBehavior.ID_REPEAT, true)

                setActionBehavior(this)
            }
        }

        invalidateOptions()
    }

    fun moveAction(fromIndex: Int, toIndex: Int) {
        actionList.value = actionList.value?.toMutableList()?.apply {
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

    fun setActionBehavior(actionBehavior: ActionBehavior) {
        actionList.value = actionList.value?.map {

            if (it.uniqueId == actionBehavior.actionId) {
                return@map actionBehavior.applyToAction(it)
            }

            it
        }

        invalidateOptions()
    }

    fun setTriggerKeyBehavior(triggerKeyBehavior: TriggerKeyBehavior) {
        triggerKeys.value = triggerKeys.value?.map { triggerKey ->

            if (triggerKey.uniqueId == triggerKeyBehavior.uniqueId) {
                return@map triggerKeyBehavior.applyToTriggerKey(triggerKey)
            }

            triggerKey
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

    fun chooseActionBehavior(id: String) {
        val action = actionList.value?.find { it.uniqueId == id } ?: return
        val behavior = ActionBehavior(action, actionList.value!!.size, triggerMode.value!!, triggerKeys.value!!)

        chooseActionBehavior.value = Event(behavior)
    }

    fun editTriggerKeyBehavior(uniqueId: String) {
        val key = triggerKeys.value?.find { it.uniqueId == uniqueId } ?: return

        val behavior = TriggerKeyBehavior(key, triggerMode.value!!)

        editTriggerKeyBehavior.value = Event(behavior)
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

    private fun invalidateOptions() {

        actionList.value = actionList.value?.map { action ->
            val newBehavior = ActionBehavior(
                action,
                actionList.value!!.size,
                triggerMode.value ?: Trigger.DEFAULT_TRIGGER_MODE,
                triggerKeys.value ?: listOf()
            )

            newBehavior.applyToAction(action)
        }

        triggerBehavior.value = triggerBehavior.value?.dependentDataChanged(
            triggerKeys.value ?: listOf(),
            triggerMode.value ?: Trigger.DEFAULT_TRIGGER_MODE
        )
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    class Factory(
        private val mConfigKeymapUseCase: ConfigKeymapUseCase,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mIOnboardingState: IOnboardingState,
        private val mId: Long) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mConfigKeymapUseCase, mDeviceInfoRepository, mIOnboardingState, mId) as T
    }
}