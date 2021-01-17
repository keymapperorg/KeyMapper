package io.github.sds100.keymapper.util.delegate

import android.view.KeyEvent
import androidx.annotation.MainThread
import androidx.collection.SparseArrayCompat
import androidx.collection.keyIterator
import androidx.collection.set
import androidx.collection.valueIterator
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.data.model.Trigger.Companion.DOUBLE_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.LONG_PRESS
import io.github.sds100.keymapper.data.model.Trigger.Companion.SHORT_PRESS
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 05/05/2020.
 */

class KeymapDetectionDelegate(private val coroutineScope: CoroutineScope,
                              val preferences: KeymapDetectionPreferences,
                              iClock: IClock,
                              iActionError: IActionError,
                              iConstraintDelegate: IConstraintDelegate
) : IClock by iClock, IActionError by iActionError,
    IConstraintDelegate by iConstraintDelegate {

    companion object {

        //the states for keys awaiting a double press
        private const val NOT_PRESSED = -1
        private const val SINGLE_PRESSED = 0
        private const val DOUBLE_PRESSED = 1

        private const val INDEX_TRIGGER_LONG_PRESS_DELAY = 0
        private const val INDEX_TRIGGER_DOUBLE_PRESS_DELAY = 1
        private const val INDEX_TRIGGER_SEQUENCE_TRIGGER_TIMEOUT = 2
        private const val INDEX_TRIGGER_VIBRATE_DURATION = 3

        private val TRIGGER_EXTRA_INDEX_MAP = mapOf(
            Trigger.EXTRA_LONG_PRESS_DELAY to INDEX_TRIGGER_LONG_PRESS_DELAY,
            Trigger.EXTRA_DOUBLE_PRESS_DELAY to INDEX_TRIGGER_DOUBLE_PRESS_DELAY,
            Trigger.EXTRA_SEQUENCE_TRIGGER_TIMEOUT to INDEX_TRIGGER_SEQUENCE_TRIGGER_TIMEOUT,
            Trigger.EXTRA_VIBRATION_DURATION to INDEX_TRIGGER_VIBRATE_DURATION
        )

        private const val INDEX_ACTION_REPEAT_RATE = 0
        private const val INDEX_ACTION_REPEAT_DELAY = 1
        private const val INDEX_STOP_REPEAT_BEHAVIOR = 2
        private const val INDEX_ACTION_MULTIPLIER = 3
        private const val INDEX_HOLD_DOWN_BEHAVIOR = 4
        private const val INDEX_DELAY_BEFORE_NEXT_ACTION = 5
        private const val INDEX_HOLD_DOWN_DURATION = 6

        private val ACTION_EXTRA_INDEX_MAP = mapOf(
            Action.EXTRA_REPEAT_RATE to INDEX_ACTION_REPEAT_RATE,
            Action.EXTRA_REPEAT_DELAY to INDEX_ACTION_REPEAT_DELAY,
            Action.EXTRA_CUSTOM_STOP_REPEAT_BEHAVIOUR to INDEX_STOP_REPEAT_BEHAVIOR,
            Action.EXTRA_MULTIPLIER to INDEX_ACTION_MULTIPLIER,
            Action.EXTRA_CUSTOM_HOLD_DOWN_BEHAVIOUR to INDEX_HOLD_DOWN_BEHAVIOR,
            Action.EXTRA_DELAY_BEFORE_NEXT_ACTION to INDEX_DELAY_BEFORE_NEXT_ACTION,
            Action.EXTRA_HOLD_DOWN_DURATION to INDEX_HOLD_DOWN_DURATION
        )

        /**
         * @return whether the actions assigned to this trigger will be performed on the down event of the final key
         * rather than the up event.
         */
        fun performActionOnDown(triggerKeys: List<Trigger.Key>, triggerMode: Int): Boolean {
            return (triggerKeys.size <= 1
                && triggerKeys.getOrNull(0)?.clickType != DOUBLE_PRESS
                && triggerMode == Trigger.UNDEFINED)

                || triggerMode == Trigger.PARALLEL
        }
    }

    /**
     * A cached copy of the keymaps in the database
     */
    var keymapListCache: List<KeyMap> = listOf()
        set(value) {
            actionMap.clear()

            // If there are no keymaps with actions then keys don't need to be detected.
            if (!value.any { it.actionList.isNotEmpty() }) {
                field = value
                detectKeymaps = false
                return
            }

            if (value.all { !it.isEnabled }) {
                detectKeymaps = false
                return
            }

            if (value.isEmpty()) {
                detectKeymaps = false
            } else {
                detectKeymaps = true

                val longPressSequenceEvents = mutableListOf<Pair<Event, Int>>()

                val doublePressEvents = mutableListOf<Pair<Event, Int>>()

                setActionMapAndOptions(value.flatMap { it.actionList }.toSet())

                // Extract all the external device descriptors used in enabled keymaps because the list is used later
                val sequenceTriggerEvents = mutableListOf<Array<Event>>()
                val sequenceTriggerActions = mutableListOf<IntArray>()
                val sequenceTriggerFlags = mutableListOf<Int>()
                val sequenceTriggerOptions = mutableListOf<IntArray>()
                val sequenceTriggerConstraints = mutableListOf<Array<Constraint>>()
                val sequenceTriggerConstraintMode = mutableListOf<Int>()
                val sequenceTriggerKeyFlags = mutableListOf<IntArray>()

                val parallelTriggerEvents = mutableListOf<Array<Event>>()
                val parallelTriggerActions = mutableListOf<IntArray>()
                val parallelTriggerFlags = mutableListOf<Int>()
                val parallelTriggerOptions = mutableListOf<IntArray>()
                val parallelTriggerConstraints = mutableListOf<Array<Constraint>>()
                val parallelTriggerConstraintMode = mutableListOf<Int>()
                val parallelTriggerModifierKeyIndices = mutableListOf<Pair<Int, Int>>()
                val parallelTriggerKeyFlags = mutableListOf<IntArray>()

                for (keymap in value) {
                    // ignore the keymap if it has no action.
                    if (keymap.actionList.isEmpty()) {
                        continue
                    }

                    if (!keymap.isEnabled) {
                        continue
                    }

                    //TRIGGER STUFF

                    val eventList = mutableListOf<Event>()

                    keymap.trigger.keys.forEachIndexed { _, key ->
                        val sequenceTriggerIndex = sequenceTriggerEvents.size

                        if (keymap.trigger.mode == Trigger.SEQUENCE && key.clickType == LONG_PRESS) {

                            if (keymap.trigger.keys.size > 1) {
                                longPressSequenceEvents.add(
                                    Event(key.keyCode, key.clickType, key.deviceId) to sequenceTriggerIndex)
                            }
                        }

                        if ((keymap.trigger.mode == Trigger.SEQUENCE || keymap.trigger.mode == Trigger.UNDEFINED)
                            && key.clickType == DOUBLE_PRESS) {
                            doublePressEvents.add(
                                Event(key.keyCode, key.clickType, key.deviceId) to sequenceTriggerIndex)
                        }

                        when (key.deviceId) {
                            Trigger.Key.DEVICE_ID_THIS_DEVICE -> {
                                detectInternalEvents = true
                            }

                            Trigger.Key.DEVICE_ID_ANY_DEVICE -> {
                                detectInternalEvents = true
                                detectExternalEvents = true
                            }

                            else -> {
                                detectExternalEvents = true
                            }
                        }

                        eventList.add(Event(key.keyCode, key.clickType, key.deviceId))
                    }

                    val encodedActionList = encodeActionList(keymap.actionList)

                    if (keymap.actionList.any { it.mappedToModifier }) {
                        modifierKeyEventActions = true
                    }

                    if (keymap.actionList.any { it.type == ActionType.KEY_EVENT && !it.mappedToModifier }) {
                        notModifierKeyEventActions = true
                    }

                    val triggerOptionsArray = IntArray(TRIGGER_EXTRA_INDEX_MAP.size) { -1 }

                    TRIGGER_EXTRA_INDEX_MAP.forEach { pair ->
                        val extraId = pair.key
                        val indexToStore = pair.value

                        keymap.trigger.extras.getData(extraId).onSuccess {
                            triggerOptionsArray[indexToStore] = it.toInt()
                        }
                    }

                    val constraints = keymap.constraintList.toTypedArray()

                    if (performActionOnDown(keymap.trigger.keys, keymap.trigger.mode)) {
                        parallelTriggerEvents.add(eventList.toTypedArray())
                        parallelTriggerActions.add(encodedActionList)
                        parallelTriggerFlags.add(keymap.trigger.flags)
                        parallelTriggerOptions.add(triggerOptionsArray)
                        parallelTriggerConstraints.add(constraints)
                        parallelTriggerConstraintMode.add(keymap.constraintMode)
                        parallelTriggerKeyFlags.add(keymap.trigger.keys.map { it.flags }.toIntArray())

                    } else {
                        sequenceTriggerEvents.add(eventList.toTypedArray())
                        sequenceTriggerActions.add(encodedActionList)
                        sequenceTriggerFlags.add(keymap.trigger.flags)
                        sequenceTriggerOptions.add(triggerOptionsArray)
                        sequenceTriggerConstraints.add(constraints)
                        sequenceTriggerConstraintMode.add(keymap.constraintMode)
                        sequenceTriggerKeyFlags.add(keymap.trigger.keys.map { it.flags }.toIntArray())
                    }
                }

                val sequenceTriggersOverlappingSequenceTriggers =
                    MutableList(sequenceTriggerEvents.size) { mutableSetOf<Int>() }

                for ((triggerIndex, trigger) in sequenceTriggerEvents.withIndex()) {

                    otherTriggerLoop@ for ((otherTriggerIndex, otherTrigger) in sequenceTriggerEvents.withIndex()) {

                        for ((eventIndex, event) in trigger.withIndex()) {
                            var lastMatchedIndex: Int? = null

                            for (otherIndex in otherTrigger.indices) {
                                if (otherTrigger.hasEventAtIndex(event, otherIndex)) {

                                    //the other trigger doesn't overlap after the first element
                                    if (otherIndex == 0) continue@otherTriggerLoop

                                    //make sure the overlap retains the order of the trigger
                                    if (lastMatchedIndex != null && lastMatchedIndex != otherIndex - 1) {
                                        continue@otherTriggerLoop
                                    }

                                    if (eventIndex == trigger.lastIndex) {
                                        sequenceTriggersOverlappingSequenceTriggers[triggerIndex].add(otherTriggerIndex)
                                    }

                                    lastMatchedIndex = otherIndex
                                }
                            }
                        }
                    }
                }

                val sequenceTriggersOverlappingParallelTriggers =
                    MutableList(parallelTriggerEvents.size) { mutableSetOf<Int>() }

                for ((triggerIndex, trigger) in parallelTriggerEvents.withIndex()) {

                    otherTriggerLoop@ for ((otherTriggerIndex, otherTrigger) in sequenceTriggerEvents.withIndex()) {

                        for ((eventIndex, event) in trigger.withIndex()) {
                            var lastMatchedIndex: Int? = null

                            for (otherIndex in otherTrigger.indices) {
                                if (otherTrigger.hasEventAtIndex(event, otherIndex)) {

                                    //the other trigger doesn't overlap after the first element
                                    if (otherIndex == 0) continue@otherTriggerLoop

                                    //make sure the overlap retains the order of the trigger
                                    if (lastMatchedIndex != null && lastMatchedIndex != otherIndex - 1) {
                                        continue@otherTriggerLoop
                                    }

                                    if (eventIndex == trigger.lastIndex) {
                                        sequenceTriggersOverlappingParallelTriggers[triggerIndex].add(otherTriggerIndex)
                                    }

                                    lastMatchedIndex = otherIndex
                                }
                            }
                        }
                    }
                }

                parallelTriggerEvents.forEachIndexed { triggerIndex, events ->
                    events.forEachIndexed { eventIndex, event ->
                        if (isModifierKey(event.keyCode)) {
                            parallelTriggerModifierKeyIndices.add(triggerIndex to eventIndex)
                        }
                    }
                }

                detectSequenceTriggers = sequenceTriggerEvents.isNotEmpty()
                this.sequenceTriggerEvents = sequenceTriggerEvents.toTypedArray()
                this.sequenceTriggerActions = sequenceTriggerActions.toTypedArray()
                this.sequenceTriggerFlags = sequenceTriggerFlags.toIntArray()
                this.sequenceTriggerOptions = sequenceTriggerOptions.toTypedArray()
                this.sequenceTriggerConstraints = sequenceTriggerConstraints.toTypedArray()
                this.sequenceTriggerConstraintMode = sequenceTriggerConstraintMode.toIntArray()
                this.sequenceTriggersOverlappingSequenceTriggers =
                    sequenceTriggersOverlappingSequenceTriggers.map { it.toIntArray() }.toTypedArray()

                this.sequenceTriggersOverlappingParallelTriggers =
                    sequenceTriggersOverlappingParallelTriggers.map { it.toIntArray() }.toTypedArray()

                this.sequenceTriggerKeyFlags = sequenceTriggerKeyFlags.toTypedArray()

                detectParallelTriggers = parallelTriggerEvents.isNotEmpty()
                this.parallelTriggerEvents = parallelTriggerEvents.toTypedArray()
                this.parallelTriggerActions = parallelTriggerActions.toTypedArray()
                this.parallelTriggerFlags = parallelTriggerFlags.toIntArray()
                this.parallelTriggerOptions = parallelTriggerOptions.toTypedArray()
                this.parallelTriggerConstraints = parallelTriggerConstraints.toTypedArray()
                this.parallelTriggerConstraintMode = parallelTriggerConstraintMode.toIntArray()
                this.parallelTriggerKeyFlags = parallelTriggerKeyFlags.toTypedArray()
                this.parallelTriggerModifierKeyIndices = parallelTriggerModifierKeyIndices.toTypedArray()

                detectSequenceLongPresses = longPressSequenceEvents.isNotEmpty()
                this.longPressSequenceEvents = longPressSequenceEvents.toTypedArray()

                detectSequenceDoublePresses = doublePressEvents.isNotEmpty()
                this.doublePressEvents = doublePressEvents.toTypedArray()

                reset()
            }

            field = value
        }

    private var detectKeymaps = false
    private var detectInternalEvents = false
    private var detectExternalEvents = false
    private var detectSequenceTriggers = false
    private var detectSequenceLongPresses = false
    private var detectSequenceDoublePresses = false

    private var detectParallelTriggers = false

    /**
     * All sequence events that have the long press click type.
     */
    private var longPressSequenceEvents = arrayOf<Pair<Event, Int>>()

    /**
     * All double press sequence events and the index of their corresponding trigger. first is the event and second is
     * the trigger index.
     */
    private var doublePressEvents = arrayOf<Pair<Event, Int>>()

    /**
     * order matches with [doublePressEvents]
     */
    private var doublePressEventStates = intArrayOf()

    /**
     * The user has an amount of time to double press a key before it is registered as a double press.
     * The order matches with [doublePressEvents]. This array stores the time when the corresponding trigger will
     * timeout. If the key isn't waiting to timeout, the value is -1.
     */
    private var doublePressTimeoutTimes = longArrayOf()

    private var actionMap = SparseArrayCompat<Action>()

    /**
     * A 2D array that stores the int values of options for each action in [mActionMap]
     */
    private var actionOptions = arrayOf<IntArray>()

    /**
     * Stores the flags for each action in [mActionMap]
     */
    private var actionFlags = intArrayOf()

    /**
     * The events to detect for each sequence trigger.
     */
    private var sequenceTriggerEvents = arrayOf<Array<Event>>()

    /**
     * The flags for each key associated with the events in [sequenceTriggerEvents]
     */
    private var sequenceTriggerKeyFlags = arrayOf<IntArray>()

    private var sequenceTriggerFlags = intArrayOf()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [sequenceTriggerEvents].
     */
    private var sequenceTriggerActions = arrayOf<IntArray>()

    /**
     * Sequence triggers timeout after the first key has been pressed. The order matches with [sequenceTriggerEvents].
     * This array stores the time when the corresponding trigger in will timeout. If the trigger in
     * isn't waiting to timeout, the value is -1.
     */
    private var sequenceTriggersTimeoutTimes = longArrayOf()

    /**
     * The indexes of triggers that overlap after the first element with each trigger in [sequenceTriggerEvents]
     */
    private var sequenceTriggersOverlappingSequenceTriggers = arrayOf<IntArray>()

    private var sequenceTriggersOverlappingParallelTriggers = arrayOf<IntArray>()

    /**
     * An array of the index of the last matched event in each sequence trigger.
     */
    private var lastMatchedSequenceEventIndices = intArrayOf()

    /**
     * A 2D array that stores the int values of options for sequence triggers. If the trigger is set to
     * use the default value, the value is -1.
     */
    private var sequenceTriggerOptions = arrayOf<IntArray>()

    private var sequenceTriggerConstraints = arrayOf<Array<Constraint>>()
    private var sequenceTriggerConstraintMode = intArrayOf()

    /**
     * The events to detect for each parallel trigger.
     */
    private var parallelTriggerEvents = arrayOf<Array<Event>>()

    /**
     * The flags for each key associated with the events in [parallelTriggerEvents]
     */
    private var parallelTriggerKeyFlags = arrayOf<IntArray>()

    private var parallelTriggerFlags = intArrayOf()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [parallelTriggerEvents].
     */
    private var parallelTriggerActions = arrayOf<IntArray>()

    private var parallelTriggerConstraints = arrayOf<Array<Constraint>>()
    private var parallelTriggerConstraintMode = intArrayOf()

    /**
     * Stores whether each event in each parallel trigger need to be "released" after being held down.
     * The order matches with [parallelTriggerEvents].
     */
    private var parallelTriggerEventsAwaitingRelease = arrayOf<BooleanArray>()

    /**
     * An array of the index of the last matched event in each parallel trigger.
     */
    private var lastMatchedParallelEventIndices = intArrayOf()

    /**
     * A 2D array which stores the int values of options for parallel triggers. If the trigger is set to
     * use the default value, the value is -1.
     */
    private var parallelTriggerOptions = arrayOf<IntArray>()

    private var parallelTriggerModifierKeyIndices = arrayOf<Pair<Int, Int>>()

    private var modifierKeyEventActions = false
    private var notModifierKeyEventActions = false
    private var unmappedKeycodesToConsumeOnUp = mutableSetOf<Int>()
    private var metaStateFromActions = 0
    private var metaStateFromKeyEvent = 0

    private val eventDownTimeMap = mutableMapOf<Event, Long>()

    /**
     * The indexes of parallel triggers that didn't have their actions performed because there is a matching trigger but
     * for a long-press. These actions should only be performed if the long-press fails, otherwise when the user
     * holds down the trigger keys for the long-press trigger, actions from both triggers will be performed.
     */
    private val performActionsOnFailedLongPress = mutableSetOf<Int>()

    /**
     * The indexes of parallel triggers that didn't have their actions performed because there is a matching trigger but
     * for a double-press. These actions should only be performed if the double-press fails, otherwise each time the user
     * presses the keys for the double press, actions from both triggers will be performed.
     */
    private val performActionsOnFailedDoublePress = mutableSetOf<Int>()

    /**
     * Maps repeat jobs to their corresponding parallel trigger index.
     */
    private val repeatJobs = SparseArrayCompat<List<RepeatJob>>()

    /**
     * Maps jobs to perform an action after a long press to their corresponding parallel trigger index
     */
    private val parallelTriggerLongPressJobs = SparseArrayCompat<Job>()

    private val parallelTriggerActionJobs = SparseArrayCompat<Job>()
    private val sequenceTriggerActionJobs = SparseArrayCompat<Job>()

    /**
     * A list of all the action keys that are being held down.
     */
    private var actionsBeingHeldDown = mutableSetOf<Int>()

    val performAction = LiveEvent<PerformAction>()
    val imitateButtonPress: LiveEvent<ImitateButtonPress> = LiveEvent()
    val vibrate: LiveEvent<Vibrate> = LiveEvent()

    /**
     * @return whether to consume the [KeyEvent].
     */
    fun onKeyEvent(
        keyCode: Int,
        action: Int,
        descriptor: String,
        isExternal: Boolean,
        metaState: Int,
        deviceId: Int
    ): Boolean {
        if (!detectKeymaps) return false

        if ((isExternal && !detectExternalEvents) || (!isExternal && !detectInternalEvents)) {
            return false
        }

        metaStateFromKeyEvent = metaState

        //remove the metastate from any modifier keys that remapped and are pressed down
        parallelTriggerModifierKeyIndices.forEach {
            val triggerIndex = it.first
            val eventIndex = it.second
            val event = parallelTriggerEvents[triggerIndex][eventIndex]

            if (parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex]) {
                metaStateFromKeyEvent =
                    metaStateFromKeyEvent.minusFlag(KeyEventUtils.modifierKeycodeToMetaState(event.keyCode))
            }
        }

        val event =
            if (isExternal) {
                Event(keyCode, Trigger.UNDETERMINED, descriptor)
            } else {
                Event(keyCode, Trigger.UNDETERMINED, Trigger.Key.DEVICE_ID_THIS_DEVICE)
            }

        when (action) {
            KeyEvent.ACTION_DOWN -> return onKeyDown(event, deviceId)
            KeyEvent.ACTION_UP -> return onKeyUp(event, deviceId)
        }

        return false
    }

    /**
     * @return whether to consume the [KeyEvent].
     */
    private fun onKeyDown(event: Event, deviceId: Int): Boolean {

        eventDownTimeMap[event] = currentTime

        var consumeEvent = false
        val isModifierKeyCode = isModifierKey(event.keyCode)
        var mappedToParallelTriggerAction = false

        //consume sequence trigger keys until their timeout has been reached
        sequenceTriggersTimeoutTimes.forEachIndexed { triggerIndex, timeoutTime ->
            if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) return@forEachIndexed

            if (timeoutTime != -1L && currentTime >= timeoutTime) {
                lastMatchedSequenceEventIndices[triggerIndex] = -1
                sequenceTriggersTimeoutTimes[triggerIndex] = -1
            } else {
                //consume the event if the trigger contains this keycode.
                sequenceTriggerEvents[triggerIndex].forEachIndexed { eventIndex, sequenceEvent ->
                    if (sequenceEvent.keyCode == event.keyCode && sequenceTriggerKeyFlags[triggerIndex][eventIndex].consume) {
                        consumeEvent = true
                    }
                }
            }
        }

        doublePressTimeoutTimes.forEachIndexed { doublePressEventIndex, timeoutTime ->
            if (currentTime >= timeoutTime) {
                doublePressTimeoutTimes[doublePressEventIndex] = -1
                doublePressEventStates[doublePressEventIndex] = NOT_PRESSED

            } else {
                val doublePressEvent = doublePressEvents[doublePressEventIndex].first
                val triggerIndex = doublePressEvents[doublePressEventIndex].second

                sequenceTriggerEvents[triggerIndex].forEachIndexed { eventIndex, event ->
                    if (event == doublePressEvent
                        && sequenceTriggerKeyFlags[triggerIndex][eventIndex].consume) {

                        consumeEvent = true
                    }
                }
            }
        }

        var awaitingLongPress = false
        var showPerformingActionToast = false
        val detectedShortPressTriggers = mutableSetOf<Int>()
        val vibrateDurations = mutableListOf<Long>()

        /* cache whether an action can be performed to avoid repeatedly checking when multiple triggers have the
        same action */
        val canActionBePerformed = SparseArrayCompat<Result<Action>>()

        if (detectParallelTriggers) {

            //only process keymaps if an action can be performed
            triggerLoop@ for ((triggerIndex, lastMatchedIndex) in lastMatchedParallelEventIndices.withIndex()) {

                for (overlappingTriggerIndex in sequenceTriggersOverlappingParallelTriggers[triggerIndex]) {
                    if (lastMatchedSequenceEventIndices[overlappingTriggerIndex] != -1) {
                        continue@triggerLoop
                    }
                }

                val constraints = parallelTriggerConstraints[triggerIndex]
                val constraintMode = parallelTriggerConstraintMode[triggerIndex]

                if (!constraints.constraintsSatisfied(constraintMode)) continue

                for (actionKey in parallelTriggerActions[triggerIndex]) {
                    if (canActionBePerformed[actionKey] == null) {
                        val action = actionMap[actionKey] ?: continue

                        val result = canActionBePerformed(action)
                        canActionBePerformed.put(actionKey, result)

                        if (result.isFailure) {
                            continue@triggerLoop
                        }
                    } else if (canActionBePerformed.get(actionKey, null) is Failure) {
                        continue@triggerLoop
                    }
                }

                val nextIndex = lastMatchedIndex + 1

                //Perform short press action

                if (parallelTriggerEvents[triggerIndex].hasEventAtIndex(event.withShortPress, nextIndex)) {

                    if (parallelTriggerKeyFlags[triggerIndex][nextIndex].consume) {
                        consumeEvent = true
                    }

                    lastMatchedParallelEventIndices[triggerIndex] = nextIndex
                    parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true

                    if (nextIndex == parallelTriggerEvents[triggerIndex].lastIndex) {
                        mappedToParallelTriggerAction = true

                        val actionKeys = parallelTriggerActions[triggerIndex]

                        actionKeys.forEach { actionKey ->
                            val action = actionMap[actionKey] ?: return@forEach

                            if (action.type == ActionType.KEY_EVENT) {
                                val actionKeyCode = action.data.toInt()

                                if (isModifierKey(actionKeyCode)) {
                                    val actionMetaState = KeyEventUtils.modifierKeycodeToMetaState(actionKeyCode)
                                    metaStateFromActions = metaStateFromActions.withFlag(actionMetaState)
                                }
                            }

                            if (showPerformingActionToast(actionKey)) {
                                showPerformingActionToast = true
                            }

                            detectedShortPressTriggers.add(triggerIndex)

                            val vibrateDuration = when {
                                parallelTriggerFlags.vibrate(triggerIndex) -> {
                                    vibrateDuration(parallelTriggerOptions[triggerIndex])
                                }

                                preferences.forceVibrate -> preferences.defaultVibrateDuration.toLong()
                                else -> -1L
                            }

                            vibrateDurations.add(vibrateDuration)
                        }
                    }
                }

                //Perform long press action
                if (parallelTriggerEvents[triggerIndex].hasEventAtIndex(event.withLongPress, nextIndex)) {

                    if (parallelTriggerKeyFlags[triggerIndex][nextIndex].consume) {
                        consumeEvent = true
                    }

                    lastMatchedParallelEventIndices[triggerIndex] = nextIndex
                    parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true

                    if (nextIndex == parallelTriggerEvents[triggerIndex].lastIndex) {
                        awaitingLongPress = true

                        if (parallelTriggerFlags[triggerIndex]
                                .hasFlag(Trigger.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION)) {
                            vibrate.value = Vibrate(vibrateDuration(parallelTriggerOptions[triggerIndex]))
                        }

                        val oldJob = parallelTriggerLongPressJobs[triggerIndex]
                        oldJob?.cancel()
                        parallelTriggerLongPressJobs.put(triggerIndex, performActionsAfterLongPressDelay(triggerIndex))
                    }
                }
            }
        }

        if (modifierKeyEventActions && !isModifierKeyCode && metaStateFromActions != 0
            && !mappedToParallelTriggerAction) {

            consumeEvent = true
            unmappedKeycodesToConsumeOnUp.add(event.keyCode)

            imitateButtonPress.value = ImitateButtonPress(event.keyCode,
                metaStateFromKeyEvent.withFlag(metaStateFromActions), deviceId)

            coroutineScope.launch {
                repeatImitatingKey(event.keyCode, deviceId)
            }
        }

        if (detectedShortPressTriggers.isNotEmpty()) {
            val matchingDoublePressEvent = doublePressEvents.any {
                it.first.matchesEvent(event.withDoublePress)
            }

            /* to prevent the actions of keys mapped to a short press and, a long press or a double press
             * from crossing over.
             */
            when {
                matchingDoublePressEvent -> {
                    performActionsOnFailedDoublePress.addAll(detectedShortPressTriggers)
                }

                awaitingLongPress -> {
                    performActionsOnFailedLongPress.addAll(detectedShortPressTriggers)
                }

                else -> detectedShortPressTriggers.forEach { triggerIndex ->

                    parallelTriggerActionJobs[triggerIndex]?.cancel()

                    parallelTriggerActionJobs[triggerIndex] = coroutineScope.launch {

                        parallelTriggerActions[triggerIndex].forEachIndexed { index, actionKey ->
                            val action = actionMap[actionKey] ?: return@forEachIndexed

                            var shouldPerformActionNormally = true

                            if (action.holdDown && action.repeat
                                && stopRepeatingWhenPressedAgain(actionKey)) {

                                shouldPerformActionNormally = false

                                if (actionsBeingHeldDown.contains(actionKey)) {
                                    actionsBeingHeldDown.remove(actionKey)

                                    performAction(
                                        action,
                                        showPerformingActionToast(actionKey),
                                        keyEventAction = KeyEventAction.UP,
                                        multiplier = actionMultiplier(actionKey)
                                    )

                                } else {
                                    actionsBeingHeldDown.add(actionKey)
                                }
                            }

                            if (holdDownUntilPressedAgain(actionKey)) {
                                if (actionsBeingHeldDown.contains(actionKey)) {
                                    actionsBeingHeldDown.remove(actionKey)

                                    performAction(
                                        action,
                                        showPerformingActionToast(actionKey),
                                        keyEventAction = KeyEventAction.UP,
                                        multiplier = actionMultiplier(actionKey))

                                    shouldPerformActionNormally = false
                                }
                            }

                            if (shouldPerformActionNormally) {
                                if (action.holdDown) {
                                    actionsBeingHeldDown.add(actionKey)
                                }

                                val keyEventAction =
                                    if (action.flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN)) {
                                        KeyEventAction.DOWN
                                    } else {
                                        KeyEventAction.DOWN_UP
                                    }

                                performAction(
                                    action,
                                    showPerformingActionToast,
                                    actionMultiplier(actionKey),
                                    keyEventAction
                                )

                                val vibrateDuration = vibrateDurations[index]

                                if (vibrateDuration != -1L) {
                                    vibrate.value = Vibrate(vibrateDuration)
                                }

                                if (action.repeat && action.holdDown) {
                                    delay(holdDownDuration(actionKey))

                                    performAction(
                                        action,
                                        false,
                                        1,
                                        KeyEventAction.UP
                                    )
                                }
                            }

                            delay(delayBeforeNextAction(actionKey))
                        }

                        initialiseRepeating(triggerIndex)
                    }
                }
            }
        }

        if (consumeEvent) {
            return true
        }

        if (detectSequenceTriggers) {
            sequenceTriggerEvents.forEachIndexed { triggerIndex, events ->
                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) return@forEachIndexed

                events.forEachIndexed { eventIndex, sequenceEvent ->
                    val matchingEvent = when {
                        sequenceEvent.matchesEvent(event.withShortPress) -> true
                        sequenceEvent.matchesEvent(event.withLongPress) -> true
                        sequenceEvent.matchesEvent(event.withDoublePress) -> true

                        else -> false
                    }

                    if (matchingEvent && sequenceTriggerKeyFlags[triggerIndex][eventIndex].consume) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * @return whether to consume the event.
     */
    private fun onKeyUp(event: Event, deviceId: Int): Boolean {
        val keyCode = event.keyCode

        val downTime = eventDownTimeMap[event] ?: currentTime
        eventDownTimeMap.remove(event)

        var consumeEvent = false
        var imitateButtonPress = false

        var successfulLongPress = false
        var successfulDoublePress = false
        var mappedToDoublePress = false
        var matchedDoublePressEventIndex = -1
        var shortPressSingleKeyTriggerJustReleased = false
        var longPressSingleKeyTriggerJustReleased = false

        var showPerformingActionToast = false

        val detectedSequenceTriggerIndexes = mutableListOf<Int>()
        val detectedParallelTriggerIndexes = mutableListOf<Int>()

        val vibrateDurations = mutableListOf<Long>()

        val imitateKeyAfterDoublePressTimeout = mutableListOf<Long>()

        if (unmappedKeycodesToConsumeOnUp.contains(keyCode)) {
            consumeEvent = true
            unmappedKeycodesToConsumeOnUp.remove(keyCode)
        }

        if (detectSequenceDoublePresses) {
            //iterate over each possible double press event to detect
            for ((index, pair) in doublePressEvents.withIndex()) {
                val doublePressEvent = pair.first
                val triggerIndex = pair.second

                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) continue

                if (doublePressEvent.matchesEvent(event.withDoublePress)) {
                    mappedToDoublePress = true
                    //increment the double press event state.
                    doublePressEventStates[index] = doublePressEventStates[index] + 1

                    when (doublePressEventStates[index]) {
                        /*if the key is in the single pressed state, set the timeout time and start the timer
                        * to imitate the key if it isn't double pressed in the end */
                        SINGLE_PRESSED -> {
                            val doublePressTimeout = doublePressTimeout(sequenceTriggerOptions[triggerIndex])
                            doublePressTimeoutTimes[index] = currentTime + doublePressTimeout

                            imitateKeyAfterDoublePressTimeout.add(doublePressTimeout)
                            matchedDoublePressEventIndex = index

                            sequenceTriggerEvents[triggerIndex].forEachIndexed { eventIndex, sequenceEvent ->
                                if (sequenceEvent == doublePressEvent
                                    && sequenceTriggerKeyFlags[triggerIndex][eventIndex].consume) {

                                    consumeEvent = true
                                }
                            }
                        }

                        /* When the key is double pressed */
                        DOUBLE_PRESSED -> {

                            successfulDoublePress = true
                            doublePressEventStates[index] = NOT_PRESSED
                            doublePressTimeoutTimes[index] = -1
                        }
                    }
                }
            }
        }

        if (detectSequenceTriggers) {
            triggerLoop@ for ((triggerIndex, lastMatchedEventIndex) in lastMatchedSequenceEventIndices.withIndex()) {
                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) continue

                //the index of the next event to match in the trigger
                val nextIndex = lastMatchedEventIndex + 1

                if ((currentTime - downTime) >= longPressDelay(sequenceTriggerOptions[triggerIndex])) {
                    successfulLongPress = true
                } else if (detectSequenceLongPresses &&
                    longPressSequenceEvents.any { it.first.matchesEvent(event.withLongPress) }) {
                    imitateButtonPress = true
                }

                val encodedEventWithClickType = when {
                    successfulLongPress -> event.withLongPress
                    successfulDoublePress -> event.withDoublePress
                    else -> event.withShortPress
                }

                for (overlappingTriggerIndex in sequenceTriggersOverlappingSequenceTriggers[triggerIndex]) {
                    if (lastMatchedSequenceEventIndices[overlappingTriggerIndex] != -1) {
                        continue@triggerLoop
                    }
                }

                //if the next event matches the event just pressed
                if (sequenceTriggerEvents[triggerIndex].hasEventAtIndex(encodedEventWithClickType, nextIndex)) {

                    if (sequenceTriggerKeyFlags[triggerIndex][nextIndex].consume) {
                        consumeEvent = true
                    }

                    lastMatchedSequenceEventIndices[triggerIndex] = nextIndex

                    /*
                    If the next index is 0, then the first event in the trigger has been matched, which means the timer
                    needs to start for this trigger.
                     */
                    if (nextIndex == 0) {
                        val startTime = currentTime
                        val timeout = sequenceTriggerTimeout(sequenceTriggerOptions[triggerIndex])

                        sequenceTriggersTimeoutTimes[triggerIndex] = startTime + timeout
                    }

                    /*
                    If the last event in a trigger has been matched, then the action needs to be performed and the timer
                    reset.
                     */
                    if (nextIndex == sequenceTriggerEvents[triggerIndex].lastIndex) {
                        detectedSequenceTriggerIndexes.add(triggerIndex)

                        sequenceTriggerActions[triggerIndex].forEachIndexed { index, key ->

                            val vibrateDuration =
                                if (sequenceTriggerFlags[triggerIndex].hasFlag(Trigger.TRIGGER_FLAG_VIBRATE)) {
                                    vibrateDuration(sequenceTriggerOptions[triggerIndex])
                                } else {
                                    -1
                                }

                            val showToast = showPerformingActionToast(key)

                            if (showToast) {
                                showPerformingActionToast = true
                            }

                            vibrateDurations.add(index, vibrateDuration)
                        }

                        lastMatchedSequenceEventIndices[triggerIndex] = -1
                        sequenceTriggersTimeoutTimes[triggerIndex] = -1
                    }
                }
            }
        }

        if (detectParallelTriggers) {
            triggerLoop@ for ((triggerIndex, events) in parallelTriggerEvents.withIndex()) {
                val singleKeyTrigger = parallelTriggerEvents[triggerIndex].size == 1

                var lastHeldDownEventIndex = -1

                for (eventIndex in events.indices) {
                    val awaitingRelease = parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex]

                    //short press
                    if (awaitingRelease && events.hasEventAtIndex(event.withShortPress, eventIndex)) {
                        if (singleKeyTrigger) {
                            shortPressSingleKeyTriggerJustReleased = true
                        }

                        if (modifierKeyEventActions) {
                            val actionKeys = parallelTriggerActions[triggerIndex]
                            actionKeys.forEach { actionKey ->

                                actionMap[actionKey]?.let { action ->
                                    if (action.type == ActionType.KEY_EVENT) {
                                        val actionKeyCode = action.data.toInt()

                                        if (action.type == ActionType.KEY_EVENT && isModifierKey(actionKeyCode)) {
                                            val actionMetaState = KeyEventUtils.modifierKeycodeToMetaState(actionKeyCode)
                                            metaStateFromActions = metaStateFromActions.minusFlag(actionMetaState)
                                        }
                                    }
                                }
                            }
                        }

                        parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex] = false

                        if (parallelTriggerKeyFlags[triggerIndex][eventIndex].consume) {
                            consumeEvent = true
                        }
                    }

                    //long press
                    if (awaitingRelease && events.hasEventAtIndex(event.withLongPress, eventIndex)) {

                        if ((currentTime - downTime) >= longPressDelay(parallelTriggerOptions[triggerIndex])) {
                            successfulLongPress = true
                        }

                        parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex] = false

                        parallelTriggerLongPressJobs[triggerIndex]?.cancel()

                        if (parallelTriggerKeyFlags[triggerIndex][eventIndex].consume) {
                            consumeEvent = true
                        }

                        val lastMatchedIndex = lastMatchedParallelEventIndices[triggerIndex]

                        if (singleKeyTrigger && successfulLongPress) {
                            longPressSingleKeyTriggerJustReleased = true
                        }

                        if (!imitateButtonPress) {
                            if (singleKeyTrigger && !successfulLongPress) {
                                imitateButtonPress = true
                            } else if (lastMatchedIndex > -1 &&
                                lastMatchedIndex < parallelTriggerEvents[triggerIndex].lastIndex) {
                                imitateButtonPress = true
                            }
                        }
                    }

                    if (parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex] &&
                        lastHeldDownEventIndex == eventIndex - 1) {

                        lastHeldDownEventIndex = eventIndex
                    }
                }

                lastMatchedParallelEventIndices[triggerIndex] = lastHeldDownEventIndex

                //cancel repeating action jobs for this trigger
                if (lastHeldDownEventIndex != parallelTriggerEvents[triggerIndex].lastIndex) {
                    repeatJobs[triggerIndex]?.forEach {
                        if (!stopRepeatingWhenPressedAgain(it.actionKey)) {
                            it.cancel()
                        }
                    }

                    val actionKeys = parallelTriggerActions[triggerIndex]

                    actionKeys.forEach { actionKey ->
                        val action = actionMap[actionKey] ?: return@forEach

                        if (!actionsBeingHeldDown.contains(actionKey)) return@forEach

                        if (action.flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN)
                            && !holdDownUntilPressedAgain(actionKey)) {

                            actionsBeingHeldDown.remove(actionKey)

                            performAction(
                                action,
                                showPerformingActionToast,
                                actionMultiplier(actionKey),
                                KeyEventAction.UP
                            )
                        }
                    }
                }
            }
        }

        //perform actions on failed long press
        if (!successfulLongPress) {
            val iterator = performActionsOnFailedLongPress.iterator()

            while (iterator.hasNext()) {
                val triggerIndex = iterator.next()

                /*
                The last event in the trigger
                */
                val lastEvent = parallelTriggerEvents[triggerIndex].last()

                if (event.withShortPress.matchesEvent(lastEvent)) {
                    detectedParallelTriggerIndexes.add(triggerIndex)

                    parallelTriggerActions[triggerIndex].forEachIndexed { index, key ->

                        val vibrateDuration = if (parallelTriggerFlags[index].hasFlag(Trigger.TRIGGER_FLAG_VIBRATE)) {
                            vibrateDuration(parallelTriggerOptions[triggerIndex])
                        } else {
                            -1
                        }
                        vibrateDurations.add(index, vibrateDuration)

                        val showToast = showPerformingActionToast(key)

                        if (showToast) {
                            showPerformingActionToast = true
                        }
                    }
                }

                iterator.remove()
            }
        }

        detectedSequenceTriggerIndexes.forEach { triggerIndex ->
            sequenceTriggerActionJobs[triggerIndex]?.cancel()

            sequenceTriggerActionJobs[triggerIndex] = coroutineScope.launch {
                sequenceTriggerActions[triggerIndex].forEachIndexed { index, actionKey ->

                    val action = actionMap[actionKey] ?: return@forEachIndexed

                    performAction(action, showPerformingActionToast, actionMultiplier(actionKey))

                    if (vibrateDurations[index] != -1L || preferences.forceVibrate) {
                        vibrate.value = Vibrate(vibrateDurations[index])
                    }

                    delay(delayBeforeNextAction(actionKey))
                }
            }
        }

        detectedParallelTriggerIndexes.forEach { triggerIndex ->
            parallelTriggerActionJobs[triggerIndex]?.cancel()

            parallelTriggerActionJobs[triggerIndex] = coroutineScope.launch {
                parallelTriggerActions[triggerIndex].forEachIndexed { index, actionKey ->

                    val action = actionMap[actionKey] ?: return@forEachIndexed

                    performAction(action, showPerformingActionToast, actionMultiplier(actionKey))

                    if (vibrateDurations[index] != -1L || preferences.forceVibrate) {
                        vibrate.value = Vibrate(vibrateDurations[index])
                    }

                    delay(delayBeforeNextAction(actionKey))
                }
            }
        }

        if (imitateKeyAfterDoublePressTimeout.isNotEmpty()
            && detectedSequenceTriggerIndexes.isEmpty()
            && detectedParallelTriggerIndexes.isEmpty()
            && !longPressSingleKeyTriggerJustReleased) {

            imitateKeyAfterDoublePressTimeout.forEach { timeout ->
                coroutineScope.launch {
                    delay(timeout)

                    /*
                    If no actions have just been performed and the key has still only been single pressed, imitate it.
                     */
                    if (doublePressEventStates[matchedDoublePressEventIndex] != SINGLE_PRESSED) {
                        return@launch
                    }

                    if (performActionsOnFailedDoublePress(event)) {
                        return@launch
                    }

                    this@KeymapDetectionDelegate.imitateButtonPress.value = ImitateButtonPress(keyCode)
                }
            }
        }
        //only imitate a key if an action isn't going to be performed
        else if (imitateButtonPress
            && detectedSequenceTriggerIndexes.isEmpty()
            && detectedParallelTriggerIndexes.isEmpty()
            && !shortPressSingleKeyTriggerJustReleased
            && !mappedToDoublePress) {

            this.imitateButtonPress.value = ImitateButtonPress(keyCode)
        }

        return consumeEvent
    }

    fun reset() {
        doublePressEventStates = IntArray(doublePressEvents.size) { NOT_PRESSED }
        doublePressTimeoutTimes = LongArray(doublePressEvents.size) { -1L }

        sequenceTriggersTimeoutTimes = LongArray(sequenceTriggerEvents.size) { -1 }
        lastMatchedSequenceEventIndices = IntArray(sequenceTriggerEvents.size) { -1 }

        lastMatchedParallelEventIndices = IntArray(parallelTriggerEvents.size) { -1 }
        parallelTriggerEventsAwaitingRelease = Array(parallelTriggerEvents.size) {
            BooleanArray(parallelTriggerEvents[it].size) { false }
        }

        performActionsOnFailedDoublePress.clear()
        performActionsOnFailedLongPress.clear()

        actionsBeingHeldDown.forEach {
            val action = actionMap[it] ?: return@forEach

            performAction(
                action,
                showPerformingActionToast = false,
                multiplier = 1,
                keyEventAction = KeyEventAction.UP
            )
        }

        actionsBeingHeldDown = mutableSetOf()

        metaStateFromActions = 0
        metaStateFromKeyEvent = 0
        unmappedKeycodesToConsumeOnUp = mutableSetOf()

        repeatJobs.valueIterator().forEach {
            it.forEach { job ->
                job.cancel()
            }
        }

        repeatJobs.clear()

        parallelTriggerLongPressJobs.valueIterator().forEach {
            it.cancel()
        }

        parallelTriggerLongPressJobs.clear()

        parallelTriggerActionJobs.valueIterator().forEach {
            it.cancel()
        }

        parallelTriggerActionJobs.clear()

        sequenceTriggerActionJobs.valueIterator().forEach {
            it.cancel()
        }

        sequenceTriggerActionJobs.clear()
    }

    /**
     * @return whether any actions were performed.
     */
    private fun performActionsOnFailedDoublePress(event: Event): Boolean {
        var showPerformingActionToast = false
        val detectedTriggerIndexes = mutableListOf<Int>()
        val vibrateDurations = mutableListOf<Long>()

        performActionsOnFailedDoublePress.forEach { triggerIndex ->
            if (event.withShortPress.matchesEvent(parallelTriggerEvents[triggerIndex].last())) {

                detectedTriggerIndexes.add(triggerIndex)

                parallelTriggerActions[triggerIndex].forEach { _ ->

                    if (showPerformingActionToast(triggerIndex)) {
                        showPerformingActionToast = true
                    }

                    val vibrateDuration =
                        if (parallelTriggerFlags[triggerIndex].hasFlag(Trigger.TRIGGER_FLAG_VIBRATE)) {
                            vibrateDuration(parallelTriggerOptions[triggerIndex])
                        } else {
                            -1
                        }

                    vibrateDurations.add(vibrateDuration)
                }
            }
        }

        performActionsOnFailedDoublePress.clear()

        detectedTriggerIndexes.forEach { triggerIndex ->
            parallelTriggerActionJobs[triggerIndex]?.cancel()

            parallelTriggerActionJobs[triggerIndex] = coroutineScope.launch {
                parallelTriggerActions[triggerIndex].forEachIndexed { index, actionKey ->

                    val action = actionMap[actionKey] ?: return@forEachIndexed

                    performAction(action, showPerformingActionToast, actionMultiplier(actionKey))

                    if (vibrateDurations[index] != -1L || preferences.forceVibrate) {
                        vibrate.value = Vibrate(vibrateDurations[index])
                    }

                    delay(delayBeforeNextAction(actionKey))
                }
            }
        }

        return detectedTriggerIndexes.isNotEmpty()
    }

    private fun encodeActionList(actions: List<Action>): IntArray {
        return actions.map { getActionKey(it) }.toIntArray()
    }

    /**
     * @return the key for the action in [mActionMap]. Returns -1 if the [action] can't be found.
     */
    private fun getActionKey(action: Action): Int {
        actionMap.keyIterator().forEach { key ->
            if (actionMap[key] == action) {
                return key
            }
        }

        throw Exception("Action $action not in the action map!")
    }

    private suspend fun repeatImitatingKey(keyCode: Int, deviceId: Int) {
        delay(400)

        while (unmappedKeycodesToConsumeOnUp.contains(keyCode)) {
            imitateButtonPress.postValue(ImitateButtonPress(keyCode,
                metaStateFromKeyEvent.withFlag(metaStateFromActions), deviceId))

            delay(50)
        }
    }

    private fun repeatAction(actionKey: Int) = RepeatJob(actionKey) {
        coroutineScope.launch {
            val repeat = actionFlags[actionKey].hasFlag(Action.ACTION_FLAG_REPEAT)
            if (!repeat) return@launch

            delay(repeatDelay(actionKey))

            while (true) {
                actionMap[actionKey]?.let { action ->

                    if (action.type == ActionType.KEY_EVENT) {
                        if (isModifierKey(action.data.toInt())) return@let
                    }

                    if (action.holdDown && action.repeat) {
                        val holdDownDuration = holdDownDuration(actionKey)

                        performAction(action, false, actionMultiplier(actionKey), KeyEventAction.DOWN)
                        delay(holdDownDuration)
                        performAction(action, false, actionMultiplier(actionKey), KeyEventAction.UP)
                    } else {
                        performAction(action, false, actionMultiplier(actionKey))
                    }
                }

                delay(repeatRate(actionKey))
            }
        }
    }

    /**
     * For parallel triggers only.
     */
    private fun performActionsAfterLongPressDelay(triggerIndex: Int) = coroutineScope.launch {
        delay(longPressDelay(parallelTriggerOptions[triggerIndex]))

        val actionKeys = parallelTriggerActions[triggerIndex]

        parallelTriggerActionJobs[triggerIndex]?.cancel()

        parallelTriggerActionJobs[triggerIndex] = coroutineScope.launch {

            actionKeys.forEach { actionKey ->
                val action = actionMap[actionKey] ?: return@forEach

                var performActionNormally = true

                if (holdDownUntilPressedAgain(actionKey)) {
                    if (actionsBeingHeldDown.contains(actionKey)) {
                        actionsBeingHeldDown.remove(actionKey)

                        performAction(
                            action,
                            showPerformingActionToast(actionKey),
                            keyEventAction = KeyEventAction.UP,
                            multiplier = actionMultiplier(actionKey))

                        performActionNormally = false
                    } else {
                        actionsBeingHeldDown.add(actionKey)
                    }
                }

                if (performActionNormally) {

                    if (action.holdDown) {
                        actionsBeingHeldDown.add(actionKey)
                    }

                    val keyEventAction =
                        if (action.flags.hasFlag(Action.ACTION_FLAG_HOLD_DOWN)) {
                            KeyEventAction.DOWN
                        } else {
                            KeyEventAction.DOWN_UP
                        }

                    performAction(action, showPerformingActionToast(actionKey), actionMultiplier(actionKey), keyEventAction)

                    if (parallelTriggerFlags.vibrate(triggerIndex) || preferences.forceVibrate
                        || parallelTriggerFlags[triggerIndex].hasFlag(Trigger.TRIGGER_FLAG_LONG_PRESS_DOUBLE_VIBRATION)) {
                        vibrate.value = Vibrate(vibrateDuration(parallelTriggerOptions[triggerIndex]))
                    }
                }

                delay(delayBeforeNextAction(actionKey))
            }

            initialiseRepeating(triggerIndex)
        }
    }

    /**
     * For parallel triggers only.
     */
    private fun initialiseRepeating(triggerIndex: Int) {
        val actionKeys = parallelTriggerActions[triggerIndex]
        val actionKeysToStartRepeating = actionKeys.toMutableSet()

        repeatJobs[triggerIndex]?.forEach {
            if (stopRepeatingWhenPressedAgain(it.actionKey)) {
                actionKeysToStartRepeating.remove(it.actionKey)
            }

            it.cancel()
        }

        val repeatJobs = mutableListOf<RepeatJob>()

        actionKeysToStartRepeating.forEach {
            repeatJobs.add(repeatAction(it))
        }

        this.repeatJobs.put(triggerIndex, repeatJobs)
    }

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

    private fun Array<Event>.hasEventAtIndex(event: Event, index: Int): Boolean {
        if (index >= size) return false

        val triggerEvent = this[index]

        return triggerEvent.matchesEvent(event)
    }

    private fun Event.matchesEvent(event: Event): Boolean {
        if (this.deviceId == Trigger.Key.DEVICE_ID_ANY_DEVICE
            || event.deviceId == Trigger.Key.DEVICE_ID_ANY_DEVICE) {

            if (this.keyCode == event.keyCode && this.clickType == event.clickType) {
                return true
            }

        } else {
            if (this.keyCode == event.keyCode
                && this.deviceId == event.deviceId
                && this.clickType == event.clickType) {
                return true
            }
        }

        return false
    }

    @MainThread
    private fun performAction(
        action: Action,
        showPerformingActionToast: Boolean,
        multiplier: Int,
        keyEventAction: KeyEventAction = KeyEventAction.DOWN_UP
    ) {
        val additionalMetaState = metaStateFromKeyEvent.withFlag(metaStateFromActions)

        repeat(multiplier) {
            performAction.value = PerformAction(action, showPerformingActionToast, additionalMetaState, keyEventAction)
        }
    }

    private fun setActionMapAndOptions(actions: Set<Action>) {
        var key = 0

        val map = SparseArrayCompat<Action>()
        val options = mutableListOf<IntArray>()
        val flags = mutableListOf<Int>()

        actions.forEach { action ->
            map.put(key, action)

            val optionValues = IntArray(ACTION_EXTRA_INDEX_MAP.size) { -1 }

            ACTION_EXTRA_INDEX_MAP.entries.forEach {
                val extraId = it.key
                val index = it.value

                action.extras.getData(extraId).onSuccess { value ->
                    optionValues[index] = value.toInt()
                }
            }

            flags.add(action.flags)
            options.add(optionValues)

            key++
        }

        actionFlags = flags.toIntArray()
        actionOptions = options.toTypedArray()
        actionMap = map
    }

    private val Int.consume
        get() = !this.hasFlag(Trigger.Key.FLAG_DO_NOT_CONSUME_KEY_EVENT)

    private val Action.mappedToModifier
        get() = type == ActionType.KEY_EVENT && isModifierKey(data.toInt())

    private fun IntArray.vibrate(triggerIndex: Int) = this[triggerIndex].hasFlag(Trigger.TRIGGER_FLAG_VIBRATE)

    private fun stopRepeatingWhenPressedAgain(actionKey: Int) =
        actionOptions.getOrNull(actionKey)?.getOrNull(INDEX_STOP_REPEAT_BEHAVIOR) == Action.STOP_REPEAT_BEHAVIOUR_TRIGGER_PRESSED_AGAIN

    private fun holdDownUntilPressedAgain(actionKey: Int) =
        actionOptions.getOrNull(actionKey)?.getOrNull(INDEX_HOLD_DOWN_BEHAVIOR) == Action.STOP_HOLD_DOWN_BEHAVIOR_TRIGGER_PRESSED_AGAIN

    private fun showPerformingActionToast(actionKey: Int) =
        actionFlags.getOrNull(actionKey)?.hasFlag(Action.ACTION_FLAG_SHOW_PERFORMING_ACTION_TOAST)
            ?: false

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

    private fun actionMultiplier(actionKey: Int): Int {
        return if (actionOptions[actionKey][INDEX_ACTION_MULTIPLIER] == -1) {
            1
        } else {
            actionOptions[actionKey][INDEX_ACTION_MULTIPLIER]
        }
    }

    private fun repeatDelay(actionKey: Int): Long {
        return if (actionOptions[actionKey][INDEX_ACTION_REPEAT_DELAY] == -1) {
            preferences.defaultRepeatDelay.toLong()
        } else {
            actionOptions[actionKey][INDEX_ACTION_REPEAT_DELAY].toLong()
        }
    }

    private fun repeatRate(actionKey: Int): Long {
        return if (actionOptions[actionKey][INDEX_ACTION_REPEAT_RATE] == -1) {
            preferences.defaultRepeatRate.toLong()
        } else {
            actionOptions[actionKey][INDEX_ACTION_REPEAT_RATE].toLong()
        }
    }

    private fun longPressDelay(options: IntArray): Long {
        return if (options[INDEX_TRIGGER_LONG_PRESS_DELAY] == -1) {
            preferences.defaultLongPressDelay.toLong()
        } else {
            options[INDEX_TRIGGER_LONG_PRESS_DELAY].toLong()
        }
    }

    private fun doublePressTimeout(options: IntArray): Long {
        return if (options[INDEX_TRIGGER_DOUBLE_PRESS_DELAY] == -1) {
            preferences.defaultDoublePressDelay.toLong()
        } else {
            options[INDEX_TRIGGER_DOUBLE_PRESS_DELAY].toLong()
        }
    }

    private fun vibrateDuration(options: IntArray): Long {
        return if (options[INDEX_TRIGGER_VIBRATE_DURATION] == -1) {
            preferences.defaultVibrateDuration.toLong()
        } else {
            options[INDEX_TRIGGER_VIBRATE_DURATION].toLong()
        }
    }

    private fun sequenceTriggerTimeout(options: IntArray): Long {
        return if (options[INDEX_TRIGGER_SEQUENCE_TRIGGER_TIMEOUT] == -1) {
            preferences.defaultSequenceTriggerTimeout.toLong()
        } else {
            options[INDEX_TRIGGER_SEQUENCE_TRIGGER_TIMEOUT].toLong()
        }
    }

    private fun delayBeforeNextAction(actionKey: Int): Long {
        return if (actionOptions[actionKey][INDEX_DELAY_BEFORE_NEXT_ACTION] == -1) {
            0
        } else {
            actionOptions[actionKey][INDEX_DELAY_BEFORE_NEXT_ACTION].toLong()
        }
    }

    private fun holdDownDuration(actionKey: Int): Long {
        return if (actionOptions[actionKey][INDEX_HOLD_DOWN_DURATION] == -1) {
            preferences.defaultHoldDownDuration.toLong()
        } else {
            actionOptions[actionKey][INDEX_HOLD_DOWN_DURATION].toLong()
        }
    }

    private fun areSequenceTriggerConstraintsSatisfied(triggerIndex: Int): Boolean {
        val constraints = sequenceTriggerConstraints[triggerIndex]
        val constraintMode = sequenceTriggerConstraintMode[triggerIndex]

        return constraints.constraintsSatisfied(constraintMode)
    }

    private val Event.withShortPress: Event
        get() = copy(clickType = SHORT_PRESS)

    private val Event.withLongPress: Event
        get() = copy(clickType = LONG_PRESS)

    private val Event.withDoublePress: Event
        get() = copy(clickType = DOUBLE_PRESS)

    private data class Event(val keyCode: Int, val clickType: Int, val deviceId: String)
    private class RepeatJob(val actionKey: Int, launch: () -> Job) : Job by launch.invoke()
}