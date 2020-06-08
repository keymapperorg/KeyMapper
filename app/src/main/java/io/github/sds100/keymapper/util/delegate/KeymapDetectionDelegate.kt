package io.github.sds100.keymapper.util.delegate

import android.view.KeyEvent
import androidx.annotation.MainThread
import androidx.collection.SparseArrayCompat
import androidx.collection.keyIterator
import androidx.collection.valueIterator
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.model.Constraint.Companion.APP_FOREGROUND
import io.github.sds100.keymapper.data.model.Constraint.Companion.APP_NOT_FOREGROUND
import io.github.sds100.keymapper.data.model.Constraint.Companion.BT_DEVICE_CONNECTED
import io.github.sds100.keymapper.data.model.Constraint.Companion.BT_DEVICE_DISCONNECTED
import io.github.sds100.keymapper.data.model.Constraint.Companion.MODE_AND
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.onSuccess
import io.github.sds100.keymapper.util.result.valueOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import timber.log.Timber

/**
 * Created by sds100 on 05/05/2020.
 */

class KeymapDetectionDelegate(private val mCoroutineScope: CoroutineScope,
                              val preferences: KeymapDetectionPreferences,
                              iClock: IClock,
                              iConstraintState: IConstraintState) : IClock by iClock, IConstraintState by iConstraintState {

    companion object {

        //FLAGS
        //0 to 1023 is reserved for keycodes
        private const val FLAG_SHORT_PRESS = 1024
        private const val FLAG_LONG_PRESS = 2048
        private const val FLAG_DOUBLE_PRESS = 4096
        private const val FLAG_INTERNAL_DEVICE = 8192

        //the states for keys awaiting a double press
        private const val NOT_PRESSED = -1
        private const val SINGLE_PRESSED = 0
        private const val DOUBLE_PRESSED = 1

        private const val INDEX_LONG_PRESS_DELAY = 0
        private const val INDEX_DOUBLE_PRESS_DELAY = 1
        private const val INDEX_HOLD_DOWN_DELAY = 2
        private const val INDEX_REPEAT_DELAY = 3
        private const val INDEX_SEQUENCE_TRIGGER_TIMEOUT = 4
        private const val INDEX_VIBRATE_DURATION = 5

        private val TRIGGER_EXTRA_INDEX_MAP = mapOf(
            Extra.EXTRA_LONG_PRESS_DELAY to INDEX_LONG_PRESS_DELAY,
            Extra.EXTRA_DOUBLE_PRESS_DELAY to INDEX_DOUBLE_PRESS_DELAY,
            Extra.EXTRA_HOLD_DOWN_DELAY to INDEX_HOLD_DOWN_DELAY,
            Extra.EXTRA_REPEAT_DELAY to INDEX_REPEAT_DELAY,
            Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT to INDEX_SEQUENCE_TRIGGER_TIMEOUT,
            Extra.EXTRA_VIBRATION_DURATION to INDEX_VIBRATE_DURATION
        )

        private fun createDeviceDescriptorMap(descriptors: Set<String>): SparseArrayCompat<String> {
            var key = 16384
            val map = SparseArrayCompat<String>()

            descriptors.forEach {
                map.put(key, it)
                key *= 2
            }

            return map
        }

        private fun createActionMap(actions: Set<Action>): SparseArrayCompat<Action> {
            var key = 0

            val map = SparseArrayCompat<Action>()

            actions.forEach {
                map.put(key, it)
                key++
            }

            return map
        }

        /**
         * @return whether the actions assigned to this trigger will be performed on the down event of the final key
         * rather than the up event.
         */
        fun performActionOnDown(triggerKeys: List<Trigger.Key>, triggerMode: Int): Boolean {
            return (triggerKeys.size == 1 && triggerKeys.getOrNull(0)?.clickType != Trigger.DOUBLE_PRESS)
                || triggerMode == Trigger.PARALLEL
        }
    }

    /**
     * A cached copy of the keymaps in the database
     */
    var keyMapListCache: List<KeyMap> = listOf()
        set(value) {
            mDeviceDescriptorMap.clear()
            mActionMap.clear()

            // If there are no keymaps with actions then keys don't need to be detected.
            if (!value.any { it.actionList.isNotEmpty() }) {
                field = value
                mDetectKeymaps = false
                return
            }

            if (value.all { !it.isEnabled }) {
                mDetectKeymaps = false
                return
            }

            if (value.isEmpty()) {
                mDetectKeymaps = false
            } else {
                mDetectKeymaps = true

                val sequenceKeyMaps = mutableListOf<KeyMap>()
                val parallelKeyMaps = mutableListOf<KeyMap>()
                val longPressSequenceEvents = mutableListOf<Pair<Int, Int>>()

                val doublePressEvents = mutableListOf<Pair<Int, Int>>()

                mActionMap = createActionMap(value.flatMap { it.actionList }.toSet())

                // Extract all the external device descriptors used in enabled keymaps because the list is used later
                val deviceDescriptors = mutableSetOf<String>()
                val sequenceTriggerEvents = mutableListOf<IntArray>()
                val sequenceTriggerActions = mutableListOf<IntArray>()
                val sequenceTriggerKeymapFlags = mutableListOf<Int>()
                val sequenceTriggerOptions = mutableListOf<IntArray>()
                val sequenceTriggerConstraints = mutableListOf<Array<Constraint>>()
                val sequenceTriggerConstraintMode = mutableListOf<Int>()

                val parallelTriggerEvents = mutableListOf<IntArray>()
                val parallelTriggerActions = mutableListOf<IntArray>()
                val parallelTriggerKeymapFlags = mutableListOf<Int>()
                val parallelTriggerOptions = mutableListOf<IntArray>()
                val parallelTriggerConstraints = mutableListOf<Array<Constraint>>()
                val parallelTriggerConstraintMode = mutableListOf<Int>()

                for (keyMap in value) {
                    if (!keyMap.isEnabled) {
                        continue
                    }

                    keyMap.trigger.keys.forEach {

                        if (it.deviceId != Trigger.Key.DEVICE_ID_THIS_DEVICE &&
                            it.deviceId != Trigger.Key.DEVICE_ID_ANY_DEVICE) {
                            deviceDescriptors.add(it.deviceId)
                        }
                    }
                }

                mDeviceDescriptorMap = createDeviceDescriptorMap(deviceDescriptors)

                for (keyMap in value) {
                    // ignore the keymap if it has no action.
                    if (keyMap.actionList.isEmpty()) {
                        continue
                    }

                    if (!keyMap.isEnabled) {
                        continue
                    }

                    //TRIGGER STUFF
                    when (keyMap.trigger.mode) {
                        Trigger.PARALLEL -> parallelKeyMaps.add(keyMap)
                        Trigger.SEQUENCE -> sequenceKeyMaps.add(keyMap)
                    }

                    val encodedTriggerList = mutableListOf<Int>()

                    keyMap.trigger.keys.forEachIndexed { keyIndex, key ->

                        if (keyMap.trigger.mode == Trigger.SEQUENCE) {
                            val sequenceTriggerIndex = sequenceTriggerEvents.size

                            when (key.clickType) {
                                Trigger.LONG_PRESS -> {
                                    if (keyMap.trigger.keys.size > 1) {
                                        longPressSequenceEvents.add(
                                            encodeEvent(key.keyCode, key.clickType, key.deviceId) to sequenceTriggerIndex)
                                    }
                                }

                                Trigger.DOUBLE_PRESS -> {
                                    doublePressEvents.add(
                                        encodeEvent(key.keyCode, key.clickType, key.deviceId) to sequenceTriggerIndex)
                                }
                            }
                        }

                        when (key.deviceId) {
                            Trigger.Key.DEVICE_ID_THIS_DEVICE -> {
                                mDetectInternalEvents = true
                            }

                            Trigger.Key.DEVICE_ID_ANY_DEVICE -> {
                                mDetectInternalEvents = true
                                mDetectExternalEvents = true
                            }

                            else -> {
                                mDetectExternalEvents = true
                            }
                        }

                        encodedTriggerList.add(encodeEvent(key.keyCode, key.clickType, key.deviceId))
                    }

                    val encodedActionList = encodeActionList(keyMap.actionList)

                    if (keyMap.actionList.any { it.mappedToModifier }) {
                        mModifierKeyEventActions = true
                    }

                    if (keyMap.actionList.any { it.type == ActionType.KEY_EVENT && !it.mappedToModifier }) {
                        mNotModifierKeyEventActions = true
                    }

                    val optionsArray = IntArray(TRIGGER_EXTRA_INDEX_MAP.size) { -1 }
                    TRIGGER_EXTRA_INDEX_MAP.forEach { pair ->
                        val extraId = pair.key
                        val indexToStore = pair.value

                        keyMap.trigger.getExtraData(extraId).onSuccess {
                            optionsArray[indexToStore] = it.toInt()
                        }
                    }

                    val constraints = sequence {
                        keyMap.constraintList.forEach {
                            val data = when (it.type) {
                                APP_FOREGROUND, APP_NOT_FOREGROUND ->
                                    it.getExtraData(Extra.EXTRA_PACKAGE_NAME).valueOrNull()

                                BT_DEVICE_CONNECTED, BT_DEVICE_DISCONNECTED ->
                                    it.getExtraData(Extra.EXTRA_BT_ADDRESS).valueOrNull()
                                else -> null
                            } ?: return@forEach

                            yield(it.type to data)
                        }
                    }.toList().toTypedArray()

                    if (performActionOnDown(keyMap.trigger.keys, keyMap.trigger.mode)) {
                        parallelTriggerEvents.add(encodedTriggerList.toIntArray())
                        parallelTriggerActions.add(encodedActionList)
                        parallelTriggerKeymapFlags.add(keyMap.flags)
                        parallelTriggerOptions.add(optionsArray)
                        parallelTriggerConstraints.add(constraints)
                        parallelTriggerConstraintMode.add(keyMap.constraintMode)

                    } else {
                        sequenceTriggerEvents.add(encodedTriggerList.toIntArray())
                        sequenceTriggerActions.add(encodedActionList)
                        sequenceTriggerKeymapFlags.add(keyMap.flags)
                        sequenceTriggerOptions.add(optionsArray)
                        sequenceTriggerConstraints.add(constraints)
                        sequenceTriggerConstraintMode.add(keyMap.constraintMode)
                    }
                }

                mDetectSequenceTriggers = sequenceTriggerEvents.isNotEmpty()
                mSequenceTriggerEvents = sequenceTriggerEvents.toTypedArray()
                mSequenceTriggerActions = sequenceTriggerActions.toTypedArray()
                mSequenceTriggerKeymapFlags = sequenceTriggerKeymapFlags.toIntArray()
                mSequenceTriggerOptions = sequenceTriggerOptions.toTypedArray()
                mSequenceTriggerConstraints = sequenceTriggerConstraints.toTypedArray()
                mSequenceTriggerConstraintMode = sequenceTriggerConstraintMode.toIntArray()

                mDetectParallelTriggers = parallelTriggerEvents.isNotEmpty()
                mParallelTriggerEvents = parallelTriggerEvents.toTypedArray()
                mParallelTriggerActions = parallelTriggerActions.toTypedArray()
                mParallelTriggerKeymapFlags = parallelTriggerKeymapFlags.toIntArray()
                mParallelTriggerOptions = parallelTriggerOptions.toTypedArray()
                mParallelTriggerConstraints = parallelTriggerConstraints.toTypedArray()
                mParallelTriggerConstraintMode = parallelTriggerConstraintMode.toIntArray()

                mDetectSequenceLongPresses = longPressSequenceEvents.isNotEmpty()
                mLongPressSequenceEvents = longPressSequenceEvents.toTypedArray()

                mDetectSequenceDoublePresses = doublePressEvents.isNotEmpty()
                mDoublePressEvents = doublePressEvents.toTypedArray()

                reset()
            }

            field = value
        }

    private var mDetectKeymaps = false
    private var mDetectInternalEvents = false
    private var mDetectExternalEvents = false
    private var mDetectSequenceTriggers = false
    private var mDetectSequenceLongPresses = false
    private var mDetectSequenceDoublePresses = false

    private var mDetectParallelTriggers = false

    /**
     * All sequence events that have the long press click type.
     */
    private var mLongPressSequenceEvents = arrayOf<Pair<Int, Int>>()

    /**
     * All double press sequence events and the index of their corresponding trigger.
     */
    private var mDoublePressEvents = arrayOf<Pair<Int, Int>>()

    /**
     * order matches with [mDoublePressEvents]
     */
    private var mDoublePressEventStates = intArrayOf()

    /**
     * The user has an amount of time to double press a key before it is registered as a double press.
     * The order matches with [mDoublePressEvents]. This array stores the time when the corresponding trigger will
     * timeout. If the key isn't waiting to timeout, the value is -1.
     */
    private var mDoublePressTimeoutTimes = longArrayOf()

    private var mDeviceDescriptorMap = SparseArrayCompat<String>()
    private var mActionMap = SparseArrayCompat<Action>()

    /**
     * The events to detect for each sequence trigger.
     */
    private var mSequenceTriggerEvents = arrayOf<IntArray>()

    private var mSequenceTriggerKeymapFlags = intArrayOf()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [mSequenceTriggerEvents].
     */
    private var mSequenceTriggerActions = arrayOf<IntArray>()

    /**
     * Sequence triggers timeout after the first key has been pressed. The order matches with [mSequenceTriggerEvents].
     * This array stores the time when the corresponding trigger in will timeout. If the trigger in
     * isn't waiting to timeout, the value is -1.
     */
    private var mSequenceTriggersTimeoutTimes = longArrayOf()

    /**
     * An array of the index of the last matched event in each sequence trigger.
     */
    private var mLastMatchedSequenceEventIndices = intArrayOf()

    /**
     * A 2D array which stores the int values of options for sequence triggers. If the trigger is set to
     * use the default value, the value is -1.
     */
    private var mSequenceTriggerOptions = arrayOf<IntArray>()

    private var mSequenceTriggerConstraints = arrayOf<Array<Constraint>>()
    private var mSequenceTriggerConstraintMode = intArrayOf()

    /**
     * The events to detect for each parallel trigger.
     */
    private var mParallelTriggerEvents = arrayOf<IntArray>()

    private var mParallelTriggerKeymapFlags = intArrayOf()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [mParallelTriggerEvents].
     */
    private var mParallelTriggerActions = arrayOf<IntArray>()

    private var mParallelTriggerConstraints = arrayOf<Array<Constraint>>()
    private var mParallelTriggerConstraintMode = intArrayOf()

    /**
     * Stores whether each event in each parallel trigger need to be "released" after being held down.
     * The order matches with [mParallelTriggerEvents].
     */
    private var mParallelTriggerEventsAwaitingRelease = arrayOf<BooleanArray>()

    /**
     * An array of the index of the last matched event in each parallel trigger.
     */
    private var mLastMatchedParallelEventIndices = intArrayOf()

    /**
     * A 2D array which stores the int values of options for parallel triggers. If the trigger is set to
     * use the default value, the value is -1.
     */
    private var mParallelTriggerOptions = arrayOf<IntArray>()

    private var mModifierKeyEventActions = false
    private var mNotModifierKeyEventActions = false
    private var mUnmappedKeycodesToConsumeOnUp = mutableSetOf<Int>()
    private var mMetaStateFromActions = 0
    private var mMetaStateFromKeyEvent = 0

    private val mEventDownTimeMap = mutableMapOf<Int, Long>()

    /**
     * Maps repeat jobs to their corresponding parallel trigger index.
     */
    private val mRepeatJobs = SparseArrayCompat<Job>()

    /**
     * Maps jobs to perform an action after a long press to their corresponding parallel trigger index
     */
    private val mParallelTriggerLongPressJobs = SparseArrayCompat<Job>()

    val performAction: MutableLiveData<Event<PerformActionModel>> = MutableLiveData()
    val imitateButtonPress: MutableLiveData<Event<ImitateKeyModel>> = MutableLiveData()
    val vibrate: MutableLiveData<Event<Long>> = MutableLiveData()

    /**
     * @return whether to consume the [KeyEvent].
     */
    fun onKeyEvent(keyCode: Int, action: Int, descriptor: String, isExternal: Boolean, metaState: Int): Boolean {
        Timber.d("onKeyEvent $keyCode $action $descriptor $isExternal $metaState")
        if (!mDetectKeymaps) return false

        if ((isExternal && !mDetectExternalEvents) || (!isExternal && !mDetectInternalEvents)) {
            return false
        }

        mMetaStateFromKeyEvent = metaState

        val encodedEvent =
            if (isExternal) {
                encodeEvent(keyCode, Trigger.UNDETERMINED, descriptor)
            } else {
                encodeEvent(keyCode, Trigger.UNDETERMINED, Trigger.Key.DEVICE_ID_THIS_DEVICE)
            }

        when (action) {
            KeyEvent.ACTION_DOWN -> return onKeyDown(keyCode, encodedEvent)
            KeyEvent.ACTION_UP -> return onKeyUp(keyCode, encodedEvent)
        }

        return false
    }

    /**
     * @return whether to consume the [KeyEvent].
     */
    private fun onKeyDown(keyCode: Int, encodedEvent: Int): Boolean {
        mEventDownTimeMap[encodedEvent] = currentTime

        var consumeEvent = false
        val isModifierKeyCode = isModifierKey(keyCode)
        var mappedToParallelTriggerAction = false

        //consume sequence trigger keys until their timeout has been reached
        mSequenceTriggersTimeoutTimes.forEachIndexed { triggerIndex, timeoutTime ->
            if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) return@forEachIndexed

            if (timeoutTime != -1L && currentTime >= timeoutTime) {
                mLastMatchedSequenceEventIndices[triggerIndex] = -1
                mSequenceTriggersTimeoutTimes[triggerIndex] = -1
            } else {
                //consume the event if the trigger contains this keycode.
                if (mSequenceTriggerEvents[triggerIndex].hasKeycode(keyCode)) {
                    consumeEvent = true
                }
            }
        }

        mDoublePressTimeoutTimes.forEachIndexed { doublePressEventIndex, timeoutTime ->
            if (currentTime >= timeoutTime) {
                mDoublePressTimeoutTimes[doublePressEventIndex] = -1
                mDoublePressEventStates[doublePressEventIndex] = NOT_PRESSED

            } else {
                consumeEvent = true
            }
        }

        if (mDetectParallelTriggers) {
            for ((triggerIndex, lastMatchedIndex) in mLastMatchedParallelEventIndices.withIndex()) {
                val constraints = mParallelTriggerConstraints[triggerIndex]
                val constraintMode = mParallelTriggerConstraintMode[triggerIndex]

                if (!constraints.constraintsSatisfied(constraintMode)) continue

                val nextIndex = lastMatchedIndex + 1

                //Perform short press action
                val encodedWithShortPress = encodedEvent.withFlag(FLAG_SHORT_PRESS)

                if (mParallelTriggerEvents[triggerIndex].hasEventAtIndex(encodedWithShortPress, nextIndex)) {

                    consumeEvent = true
                    mLastMatchedParallelEventIndices[triggerIndex] = nextIndex
                    mParallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true

                    if (nextIndex == mParallelTriggerEvents[triggerIndex].lastIndex) {
                        mappedToParallelTriggerAction = true

                        mParallelTriggerActions[triggerIndex].forEach {
                            val action = mActionMap[it] ?: return@forEach

                            if (action.type == ActionType.KEY_EVENT) {
                                val actionKeyCode = action.data.toInt()

                                if (isModifierKey(actionKeyCode)) {
                                    val actionMetaState = KeyEventUtils.modifierKeycodeToMetaState(actionKeyCode)
                                    mMetaStateFromActions = mMetaStateFromActions.withFlag(actionMetaState)
                                }
                            }

                            performAction(action, mParallelTriggerKeymapFlags.showPerformingActionToast(triggerIndex))

                            if (mParallelTriggerKeymapFlags.vibrate(triggerIndex) || preferences.forceVibrate) {
                                vibrate.value = Event(vibrateDuration(mParallelTriggerOptions[triggerIndex]))
                            }
                        }

                        val oldJob = mRepeatJobs[triggerIndex]
                        oldJob?.cancel()
                        mRepeatJobs.put(triggerIndex, repeatActions(triggerIndex))
                    }
                }

                //Perform long press action
                val encodedWithLongPress = encodedEvent.withFlag(FLAG_LONG_PRESS)

                if (mParallelTriggerEvents[triggerIndex].hasEventAtIndex(encodedWithLongPress, nextIndex)) {

                    consumeEvent = true
                    mLastMatchedParallelEventIndices[triggerIndex] = nextIndex
                    mParallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true

                    if (nextIndex == mParallelTriggerEvents[triggerIndex].lastIndex) {
                        val oldJob = mParallelTriggerLongPressJobs[triggerIndex]
                        oldJob?.cancel()
                        mParallelTriggerLongPressJobs.put(triggerIndex, performActionsAfterLongPressDelay(triggerIndex))
                    }
                }
            }
        }

        if (mModifierKeyEventActions && !isModifierKeyCode && mMetaStateFromActions != 0
            && !mappedToParallelTriggerAction) {

            consumeEvent = true
            mUnmappedKeycodesToConsumeOnUp.add(keyCode)

            imitateButtonPress.value = Event(ImitateKeyModel(keyCode,
                mMetaStateFromKeyEvent.withFlag(mMetaStateFromActions)))

            mCoroutineScope.launch {
                repeatImitatingKey(keyCode)
            }
        }

        if (consumeEvent) {
            Timber.d("consume down $keyCode")
            return true
        }

        if (mDetectSequenceDoublePresses) {
            mDoublePressEvents.forEach {
                val triggerIndex = it.second

                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) return@forEach

                val event = it.first

                if (event.matchesEvent(encodedEvent.withFlag(FLAG_DOUBLE_PRESS))) {
                    Timber.d("consume down $keyCode")
                    return true
                }
            }
        }

        if (mDetectSequenceLongPresses) {
            mLongPressSequenceEvents.forEach {
                val triggerIndex = it.second

                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) return@forEach

                val event = it.first

                if (event.matchesEvent(encodedEvent.withFlag(FLAG_LONG_PRESS))) {
                    Timber.d("consume down $keyCode")
                    return true
                }
            }
        }

        return false
    }

    /**
     * @return whether to consume the event.
     */
    private fun onKeyUp(keyCode: Int, encodedEvent: Int): Boolean {
        val downTime = mEventDownTimeMap[encodedEvent]!!
        mEventDownTimeMap.remove(encodedEvent)

        var consumeEvent = false
        var imitateButtonPress = false

        var hasShowToastFlag = false
        var successfulLongPress = false
        var successfulDoublePress = false
        var mappedToDoublePress = false

        var matchedDoublePressEventIndex = -1
        var shortPressSingleKeyTriggerJustReleased = false
        var longPressSingleKeyTriggerJustReleased = false

        val actionKeysToPerform = mutableSetOf<Int>()
        var vibrateDuration = -1L

        val imitateKeyAfterDoublePressTimeout = mutableListOf<Long>()

        if (mUnmappedKeycodesToConsumeOnUp.contains(keyCode)) {
            consumeEvent = true
            mUnmappedKeycodesToConsumeOnUp.remove(keyCode)
        }

        if (mDetectSequenceDoublePresses) {
            //iterate over each possible double press event to detect
            for ((index, pair) in mDoublePressEvents.withIndex()) {
                val event = pair.first
                val triggerIndex = pair.second

                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) continue

                if (event.matchesEvent(encodedEvent.withFlag(FLAG_DOUBLE_PRESS))) {
                    mappedToDoublePress = true
                    //increment the double press event state.
                    mDoublePressEventStates[index] = mDoublePressEventStates[index] + 1

                    when (mDoublePressEventStates[index]) {
                        /*if the key is in the single pressed state, set the timeout time and start the timer
                        * to imitate the key if it isn't double pressed in the end */
                        SINGLE_PRESSED -> {
                            val doublePressTimeout = doublePressTimeout(mSequenceTriggerOptions[triggerIndex])
                            mDoublePressTimeoutTimes[index] = currentTime + doublePressTimeout

                            imitateKeyAfterDoublePressTimeout.add(doublePressTimeout)
                            matchedDoublePressEventIndex = index

                            consumeEvent = true
                        }

                        /* When the key is double pressed */
                        DOUBLE_PRESSED -> {

                            successfulDoublePress = true
                            mDoublePressEventStates[index] = NOT_PRESSED
                            mDoublePressTimeoutTimes[index] = -1
                        }
                    }
                }
            }
        }

        if (mDetectSequenceTriggers) {
            for ((triggerIndex, lastMatchedEventIndex) in mLastMatchedSequenceEventIndices.withIndex()) {
                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) continue

                //the index of the next event to match in the trigger
                val nextIndex = lastMatchedEventIndex + 1

                if ((currentTime - downTime) >= longPressDelay(mSequenceTriggerOptions[triggerIndex])) {
                    successfulLongPress = true
                } else if (mDetectSequenceLongPresses &&
                    mLongPressSequenceEvents.any { it.first.matchesEvent(encodedEvent.withFlag(FLAG_LONG_PRESS)) }) {
                    imitateButtonPress = true
                }

                val encodedEventWithClickType = when {
                    successfulLongPress -> encodedEvent.withFlag(FLAG_LONG_PRESS)
                    successfulDoublePress -> encodedEvent.withFlag(FLAG_DOUBLE_PRESS)
                    else -> encodedEvent.withFlag(FLAG_SHORT_PRESS)
                }
                //if the next event matches the event just pressed
                if (mSequenceTriggerEvents[triggerIndex].hasEventAtIndex(encodedEventWithClickType, nextIndex)) {
                    consumeEvent = true

                    mLastMatchedSequenceEventIndices[triggerIndex] = nextIndex

                    /*
                    If the next index is 0, then the first event in the trigger has been matched, which means the timer
                    needs to start for this trigger.
                     */
                    if (nextIndex == 0) {
                        val startTime = currentTime
                        val timeout = sequenceTriggerTimeout(mSequenceTriggerOptions[triggerIndex])

                        mSequenceTriggersTimeoutTimes[triggerIndex] = startTime + timeout
                    }

                    /*
                    If the last event in a trigger has been matched, then the action needs to be performed and the timer
                    reset.
                     */
                    if (nextIndex == mSequenceTriggerEvents[triggerIndex].lastIndex) {

                        actionKeysToPerform.addAll(mSequenceTriggerActions[triggerIndex].toList())
                        vibrateDuration = vibrateDuration(mSequenceTriggerOptions[triggerIndex])
                        hasShowToastFlag = mSequenceTriggerKeymapFlags.showPerformingActionToast(triggerIndex)
                        mLastMatchedSequenceEventIndices[triggerIndex] = -1
                        mSequenceTriggersTimeoutTimes[triggerIndex] = -1
                    }
                }
            }
        }

        if (mDetectParallelTriggers) {
            triggerLoop@ for ((triggerIndex, events) in mParallelTriggerEvents.withIndex()) {
                val encodedWithShortPress = encodedEvent.withFlag(FLAG_SHORT_PRESS)
                val encodedWithLongPress = encodedEvent.withFlag(FLAG_LONG_PRESS)

                val singleKeyTrigger = mParallelTriggerEvents[triggerIndex].size == 1

                var lastHeldDownEventIndex = -1

                for (eventIndex in events.indices) {
                    val awaitingRelease = mParallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex]

                    //short press
                    if (awaitingRelease && events.hasEventAtIndex(encodedWithShortPress, eventIndex)) {

                        if (singleKeyTrigger) {
                            shortPressSingleKeyTriggerJustReleased = true
                        }

                        if (mModifierKeyEventActions) {
                            val actionKeys = mParallelTriggerActions[triggerIndex]
                            actionKeys.forEach { actionKey ->

                                mActionMap[actionKey]?.let { action ->
                                    val actionKeyCode = action.data.toInt()

                                    if (action.type == ActionType.KEY_EVENT && isModifierKey(actionKeyCode)) {
                                        val actionMetaState = KeyEventUtils.modifierKeycodeToMetaState(actionKeyCode)
                                        mMetaStateFromActions = mMetaStateFromActions.minusFlag(actionMetaState)
                                    }
                                }
                            }
                        }

                        mParallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex] = false

                        consumeEvent = true
                    }

                    //long press
                    if (awaitingRelease && events.hasEventAtIndex(encodedWithLongPress, eventIndex)) {

                        if ((currentTime - downTime) >= longPressDelay(mParallelTriggerOptions[triggerIndex])) {
                            successfulLongPress = true
                        }

                        mParallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex] = false

                        mParallelTriggerLongPressJobs[triggerIndex]?.cancel()

                        consumeEvent = true

                        val lastMatchedIndex = mLastMatchedParallelEventIndices[triggerIndex]

                        if (singleKeyTrigger && successfulLongPress) {
                            longPressSingleKeyTriggerJustReleased = true
                        }

                        if (!imitateButtonPress) {
                            if (singleKeyTrigger && !successfulLongPress) {
                                imitateButtonPress = true
                            } else if (lastMatchedIndex > -1 &&
                                lastMatchedIndex < mParallelTriggerEvents[triggerIndex].lastIndex) {
                                imitateButtonPress = true
                            }
                        }
                    }

                    if (mParallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex] &&
                        lastHeldDownEventIndex == eventIndex - 1) {

                        lastHeldDownEventIndex = eventIndex
                    }
                }

                mLastMatchedParallelEventIndices[triggerIndex] = lastHeldDownEventIndex

                if (lastHeldDownEventIndex != mParallelTriggerEvents[triggerIndex].lastIndex) {
                    mRepeatJobs[triggerIndex]?.cancel()
                }
            }
        }

        actionKeysToPerform.forEachIndexed { index, actionKey ->
            val action = mActionMap[actionKey] ?: return@forEachIndexed

            performAction(action, hasShowToastFlag)

            if (vibrateDuration != -1L || preferences.forceVibrate) {
                vibrate.value = Event(vibrateDuration)
            }
        }

        if (imitateKeyAfterDoublePressTimeout.isNotEmpty()
            && actionKeysToPerform.isEmpty()
            && !shortPressSingleKeyTriggerJustReleased && !longPressSingleKeyTriggerJustReleased) {
            imitateKeyAfterDoublePressTimeout.forEach { timeout ->
                mCoroutineScope.launch {
                    delay(timeout)

                    /*
                    If the key has still only been single pressed, imitate it.
                     */
                    if (mDoublePressEventStates[matchedDoublePressEventIndex] == SINGLE_PRESSED) {
                        this@KeymapDetectionDelegate.imitateButtonPress.value = Event(ImitateKeyModel(keyCode))
                    }
                }
            }
        }
        //only imitate a key if an action isn't going to be performed
        else if (imitateButtonPress && actionKeysToPerform.isEmpty() &&
            !shortPressSingleKeyTriggerJustReleased && !mappedToDoublePress) {

            this.imitateButtonPress.value = Event(ImitateKeyModel(keyCode))
        }

        if (consumeEvent) {
            Timber.d("consume up $keyCode")
        }

        return consumeEvent
    }

    fun reset() {
        mDoublePressEventStates = IntArray(mDoublePressEvents.size) { NOT_PRESSED }
        mDoublePressTimeoutTimes = LongArray(mDoublePressEvents.size) { -1L }

        mSequenceTriggersTimeoutTimes = LongArray(mSequenceTriggerEvents.size) { -1 }
        mLastMatchedSequenceEventIndices = IntArray(mSequenceTriggerEvents.size) { -1 }

        mLastMatchedParallelEventIndices = IntArray(mParallelTriggerEvents.size) { -1 }
        mParallelTriggerEventsAwaitingRelease = Array(mParallelTriggerEvents.size) {
            BooleanArray(mParallelTriggerEvents[it].size) { false }
        }

        mMetaStateFromActions = 0
        mMetaStateFromKeyEvent = 0
        mUnmappedKeycodesToConsumeOnUp = mutableSetOf()

        mRepeatJobs.valueIterator().forEach {
            it.cancel()
        }

        mRepeatJobs.clear()

        mParallelTriggerLongPressJobs.valueIterator().forEach {
            it.cancel()
        }

        mParallelTriggerLongPressJobs.clear()
    }

    /**
     * Key presses will be encoded as an integer to improve performance and simplify the data structures that
     * could be needed. Attributes will be stored as flags added to the keycode.
     *
     * - 0 to 1023 will be reserved for the keycode.
     * - 1024, 2048, 4096 are the click types.
     * - An 8192 flag means the event came from an internal device.
     * - If the key is from an external device, a flag greater than 8192 is for the key that points to the descriptor
     * in the [mDeviceDescriptorMap].
     */
    private fun encodeEvent(keyCode: Int, @Trigger.ClickType clickType: Int, deviceId: String): Int {
        val clickTypeFlag = when (clickType) {
            Trigger.SHORT_PRESS -> FLAG_SHORT_PRESS
            Trigger.LONG_PRESS -> FLAG_LONG_PRESS
            Trigger.DOUBLE_PRESS -> FLAG_DOUBLE_PRESS
            else -> 0
        }

        return when (deviceId) {
            Trigger.Key.DEVICE_ID_THIS_DEVICE ->
                keyCode.withFlag(clickTypeFlag).withFlag(FLAG_INTERNAL_DEVICE)

            Trigger.Key.DEVICE_ID_ANY_DEVICE ->
                keyCode.withFlag(clickTypeFlag)

            else -> {
                val descriptorKey = getDescriptorKey(deviceId)

                if (descriptorKey == -1) {
                    keyCode.withFlag(clickTypeFlag)
                } else {
                    keyCode.withFlag(clickTypeFlag).withFlag(descriptorKey)
                }
            }
        }
    }

    private fun encodeActionList(actions: List<Action>): IntArray {
        return actions.map { getActionKey(it) }.toIntArray()
    }

    /**
     * @return the key for the action in [mActionMap]. Returns -1 if the [action] can't be found.
     */
    private fun getActionKey(action: Action): Int {
        mActionMap.keyIterator().forEach { key ->
            if (mActionMap[key] == action) {
                return key
            }
        }

        throw Exception("Action ${action.uniqueId} not in the action map!")
    }

    /**
     * @return the key for the device descriptor in [mDeviceDescriptorMap]. Returns -1 if this descriptor isn't in
     * [mDeviceDescriptorMap]
     */
    private fun getDescriptorKey(descriptor: String): Int {
        mDeviceDescriptorMap.keyIterator().forEach { key ->
            if (mDeviceDescriptorMap[key] == descriptor) {
                return key
            }
        }

        return -1
    }

    private suspend fun repeatImitatingKey(keyCode: Int) {
        delay(400)

        while (mUnmappedKeycodesToConsumeOnUp.contains(keyCode)) {
            imitateButtonPress.postValue(Event(ImitateKeyModel(keyCode,
                mMetaStateFromKeyEvent.withFlag(mMetaStateFromActions))))

            delay(50)
        }
    }

    private fun repeatActions(triggerIndex: Int) = mCoroutineScope.launch {
        holdDownDelay(mParallelTriggerOptions[triggerIndex])

        while (true) {
            mParallelTriggerActions[triggerIndex].forEach {
                mActionMap[it]?.let { action ->
                    if (action.repeatable) {

                        if (action.type == ActionType.KEY_EVENT) {
                            if (isModifierKey(action.data.toInt())) return@let
                        }

                        performAction(action, false)
                    }
                }

                delay(repeatDelay(mParallelTriggerOptions[triggerIndex]).toLong())
            }
        }
    }

    /**
     * for parallel triggers only
     */
    private fun performActionsAfterLongPressDelay(triggerIndex: Int) = mCoroutineScope.launch {
        delay(longPressDelay(mParallelTriggerOptions[triggerIndex]))

        val showToast = mParallelTriggerKeymapFlags.showPerformingActionToast(triggerIndex)

        mParallelTriggerActions[triggerIndex].forEach {
            val action = mActionMap[it] ?: return@forEach

            performAction(action, showToast)

            if (mParallelTriggerKeymapFlags.vibrate(triggerIndex) || preferences.forceVibrate) {
                vibrate.value = Event(vibrateDuration(mParallelTriggerOptions[triggerIndex]))
            }
        }

        val job = mRepeatJobs[triggerIndex]
        job?.cancel()
        mRepeatJobs.put(triggerIndex, repeatActions(triggerIndex))
    }

    private val Int.internalDevice
        get() = this.hasFlag(FLAG_INTERNAL_DEVICE)

    private val Int.externalDevice
        get() = this > 16384

    private val Int.anyDevice
        get() = this < 8192

    private val Int.keyCode
        get() = this and 1023

    private val IntArray.keyCodes: IntArray
        get() {
            val array = IntArray(size)

            forEachIndexed { index, key ->
                array[index] = key.keyCode
            }

            return array
        }

    private fun IntArray.hasKeycode(keyCode: Int) = this.any { it.keyCode == keyCode }

    private fun IntArray.hasEvent(event: Int): Boolean {
        for (i in this.indices) {
            if (this.hasEventAtIndex(event, i)) {
                return true
            }
        }

        return false
    }

    private fun IntArray.hasEventAtIndex(event: Int, index: Int): Boolean {
        if (index >= size) return false

        val triggerEvent = this[index]

        return triggerEvent.matchesEvent(event)
    }

    private fun Int.matchesEvent(event: Int): Boolean {
        if (this.anyDevice || event.anyDevice) {
            if (this.keyCode == event.keyCode && this.clickType == event.clickType) {
                return true
            }
        } else {
            if (this == event) {
                return true
            }
        }

        return false
    }

    @MainThread
    private fun performAction(action: Action, showToast: Boolean) {
        val metaState = mMetaStateFromKeyEvent.withFlag(mMetaStateFromActions)
        Timber.d("current metastate = $metaState")
        //Don't use postValue because multiple actions can't be performed at the same time
        performAction.value = Event(PerformActionModel(action, metaState, showToast))
    }

    private val Int.clickType
        //bit shift right 10x and only keep last 3 bits
        get() = (this shr 10) and 7

    private val Int.deviceDescriptor
        get() = (this shr 13) shl 13

    private val Action.mappedToModifier
        get() = type == ActionType.KEY_EVENT && isModifierKey(data.toInt())

    private fun IntArray.vibrate(triggerIndex: Int) = this[triggerIndex].hasFlag(KeyMap.KEYMAP_FLAG_VIBRATE)
    private fun IntArray.showPerformingActionToast(triggerIndex: Int) =
        this[triggerIndex].hasFlag(KeyMap.KEYMAP_FLAG_SHOW_PERFORMING_ACTION_TOAST)

    private fun isModifierKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_META_LEFT,
            KeyEvent.KEYCODE_META_RIGHT,
            KeyEvent.KEYCODE_SYM,
            KeyEvent.KEYCODE_NUM,
            KeyEvent.KEYCODE_FUNCTION -> true

            else -> false
        }
    }

    private suspend fun holdDownDelay(options: IntArray) {
        if (options[INDEX_HOLD_DOWN_DELAY] == -1) {
            delay(preferences.defaultHoldDownDelay.toLong())
        } else {
            delay(options[INDEX_HOLD_DOWN_DELAY].toLong())
        }
    }

    private fun repeatDelay(options: IntArray): Long {
        return if (options[INDEX_REPEAT_DELAY] == -1) {
            preferences.defaultRepeatDelay.toLong()
        } else {
            options[INDEX_REPEAT_DELAY].toLong()
        }
    }

    private fun longPressDelay(options: IntArray): Long {
        return if (options[INDEX_LONG_PRESS_DELAY] == -1) {
            preferences.defaultLongPressDelay.toLong()
        } else {
            options[INDEX_LONG_PRESS_DELAY].toLong()
        }
    }

    private fun doublePressTimeout(options: IntArray): Long {
        return if (options[INDEX_DOUBLE_PRESS_DELAY] == -1) {
            preferences.defaultDoublePressDelay.toLong()
        } else {
            options[INDEX_DOUBLE_PRESS_DELAY].toLong()
        }
    }

    private fun vibrateDuration(options: IntArray): Long {
        return if (options[INDEX_VIBRATE_DURATION] == -1) {
            preferences.defaultVibrateDuration.toLong()
        } else {
            options[INDEX_VIBRATE_DURATION].toLong()
        }
    }

    private fun sequenceTriggerTimeout(options: IntArray): Long {
        return if (options[INDEX_SEQUENCE_TRIGGER_TIMEOUT] == -1) {
            preferences.defaultSequenceTriggerTimeout.toLong()
        } else {
            options[INDEX_SEQUENCE_TRIGGER_TIMEOUT].toLong()
        }
    }

    private fun Array<Constraint>.constraintsSatisfied(@ConstraintMode mode: Int): Boolean {
        if (this.isEmpty()) return true

        return if (mode == MODE_AND) {
            all { it.constraintSatisfied() }
        } else {
            any { it.constraintSatisfied() }
        }
    }

    private fun areSequenceTriggerConstraintsSatisfied(triggerIndex: Int): Boolean {
        val constraints = mSequenceTriggerConstraints[triggerIndex]
        val constraintMode = mSequenceTriggerConstraintMode[triggerIndex]

        return constraints.constraintsSatisfied(constraintMode)
    }

    private fun Constraint.constraintSatisfied(): Boolean {
        return when (first) {
            APP_FOREGROUND -> second == currentPackageName
            APP_NOT_FOREGROUND -> second != currentPackageName
            BT_DEVICE_CONNECTED -> isBluetoothDeviceConnected(second)
            BT_DEVICE_DISCONNECTED -> !isBluetoothDeviceConnected(second)
            else -> true
        }
    }
}

/**
 * first = type, second = data
 */
private typealias Constraint = Pair<String, String>