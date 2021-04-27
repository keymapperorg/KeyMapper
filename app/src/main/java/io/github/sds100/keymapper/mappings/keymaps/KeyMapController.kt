package io.github.sds100.keymapper.mappings.keymaps

import android.view.KeyEvent
import androidx.collection.*
import io.github.sds100.keymapper.actions.KeyEventAction
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTrigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 05/05/2020.
 */

class KeyMapController(
    private val coroutineScope: CoroutineScope,
    private val useCase: DetectKeyMapsUseCase,
    private val performActions: PerformActionsUseCase,
    private val detectConstraints: DetectConstraintsUseCase
) {
    companion object {

        //the states for keys awaiting a double press
        private const val NOT_PRESSED = -1
        private const val SINGLE_PRESSED = 0
        private const val DOUBLE_PRESSED = 1

        /**
         * @return whether the actions assigned to this trigger will be performed on the down event of the final key
         * rather than the up event.
         */
        fun performActionOnDown(trigger: KeyMapTrigger): Boolean {
            return (trigger.keys.size <= 1
                && trigger.keys.getOrNull(0)?.clickType != ClickType.DOUBLE_PRESS
                && trigger.mode == TriggerMode.Undefined)

                || trigger.mode is TriggerMode.Parallel
        }
    }

    /**
     * A cached copy of the keymaps in the database
     */
    private var keyMapList: List<KeyMap> = listOf()
        set(value) {
            actionMap.clear()

            // If there are no keymaps with actions then keys don't need to be detected.
            if (!value.any { it.actionList.isNotEmpty() }) {
                field = value
                detectKeyMaps = false
                return
            }

            if (value.all { !it.isEnabled }) {
                detectKeyMaps = false
                return
            }

            if (value.isEmpty()) {
                detectKeyMaps = false
            } else {
                detectKeyMaps = true

                val longPressSequenceTriggerKeys = mutableListOf<TriggerKey>()

                val doublePressKeys = mutableListOf<TriggerKeyLocation>()

                setActionMapAndOptions(value.flatMap { it.actionList }.toSet())

                // Extract all the external device descriptors used in enabled keymaps because the list is used later
                val sequenceTriggers = mutableListOf<KeyMapTrigger>()
                val sequenceTriggerActions = mutableListOf<IntArray>()
                val sequenceTriggerConstraints = mutableListOf<ConstraintState>()

                val parallelTriggers = mutableListOf<KeyMapTrigger>()
                val parallelTriggerActions = mutableListOf<IntArray>()
                val parallelTriggerConstraints = mutableListOf<ConstraintState>()
                val parallelTriggerModifierKeyIndices = mutableListOf<Pair<Int, Int>>()

                for (keyMap in value) {
                    // ignore the keymap if it has no action.
                    if (keyMap.actionList.isEmpty()) {
                        continue
                    }

                    if (!keyMap.isEnabled) {
                        continue
                    }

                    //TRIGGER STUFF

                    keyMap.trigger.keys.forEachIndexed { keyIndex, key ->
                        val sequenceTriggerIndex = sequenceTriggers.size

                        if (keyMap.trigger.mode == TriggerMode.Sequence
                            && key.clickType == ClickType.LONG_PRESS
                        ) {

                            if (keyMap.trigger.keys.size > 1) {
                                longPressSequenceTriggerKeys.add(key)
                            }
                        }

                        if ((keyMap.trigger.mode == TriggerMode.Sequence
                                || keyMap.trigger.mode == TriggerMode.Undefined)
                            && key.clickType == ClickType.DOUBLE_PRESS
                        ) {
                            doublePressKeys.add(TriggerKeyLocation(sequenceTriggerIndex, keyIndex))
                        }

                        when (key.device) {
                            TriggerKeyDevice.Internal -> {
                                detectInternalEvents = true
                            }

                            TriggerKeyDevice.Any -> {
                                detectInternalEvents = true
                                detectExternalEvents = true
                            }

                            is TriggerKeyDevice.External -> {
                                detectExternalEvents = true
                            }
                        }
                    }

                    val encodedActionList = encodeActionList(keyMap.actionList)

                    if (keyMap.actionList.any { it.data is KeyEventAction && isModifierKey(it.data.keyCode) }) {
                        modifierKeyEventActions = true
                    }

                    if (keyMap.actionList.any { it.data is KeyEventAction && !isModifierKey(it.data.keyCode) }) {
                        notModifierKeyEventActions = true
                    }

                    if (performActionOnDown(keyMap.trigger)) {
                        parallelTriggers.add(keyMap.trigger)
                        parallelTriggerActions.add(encodedActionList)
                        parallelTriggerConstraints.add(keyMap.constraintState)

                    } else {
                        sequenceTriggers.add(keyMap.trigger)
                        sequenceTriggerActions.add(encodedActionList)
                        sequenceTriggerConstraints.add(keyMap.constraintState)
                    }
                }

                val sequenceTriggersOverlappingSequenceTriggers =
                    MutableList(sequenceTriggers.size) { mutableSetOf<Int>() }

                for ((triggerIndex, trigger) in sequenceTriggers.withIndex()) {

                    otherTriggerLoop@ for ((otherTriggerIndex, otherTrigger) in sequenceTriggers.withIndex()) {

                        for ((keyIndex, key) in trigger.keys.withIndex()) {
                            var lastMatchedIndex: Int? = null

                            for ((otherIndex, otherKey) in otherTrigger.keys.withIndex()) {
                                if (key.matchesWithOtherKey(otherKey)) {

                                    //the other trigger doesn't overlap after the first element
                                    if (otherIndex == 0) continue@otherTriggerLoop

                                    //make sure the overlap retains the order of the trigger
                                    if (lastMatchedIndex != null && lastMatchedIndex != otherIndex - 1) {
                                        continue@otherTriggerLoop
                                    }

                                    if (keyIndex == trigger.keys.lastIndex) {
                                        sequenceTriggersOverlappingSequenceTriggers[triggerIndex].add(
                                            otherTriggerIndex
                                        )
                                    }

                                    lastMatchedIndex = otherIndex
                                }
                            }
                        }
                    }
                }

                val sequenceTriggersOverlappingParallelTriggers =
                    MutableList(parallelTriggers.size) { mutableSetOf<Int>() }

                for ((triggerIndex, trigger) in parallelTriggers.withIndex()) {

                    otherTriggerLoop@ for ((otherTriggerIndex, otherTrigger) in sequenceTriggers.withIndex()) {

                        for ((keyIndex, key) in trigger.keys.withIndex()) {
                            var lastMatchedIndex: Int? = null

                            for ((otherKeyIndex, otherKey) in otherTrigger.keys.withIndex()) {

                                if (key.matchesWithOtherKey(otherKey)) {

                                    //the other trigger doesn't overlap after the first element
                                    if (otherKeyIndex == 0) continue@otherTriggerLoop

                                    //make sure the overlap retains the order of the trigger
                                    if (lastMatchedIndex != null && lastMatchedIndex != otherKeyIndex - 1) {
                                        continue@otherTriggerLoop
                                    }

                                    if (keyIndex == trigger.keys.lastIndex) {
                                        sequenceTriggersOverlappingParallelTriggers[triggerIndex].add(
                                            otherTriggerIndex
                                        )
                                    }

                                    lastMatchedIndex = otherKeyIndex
                                }
                            }
                        }
                    }
                }

                val parallelTriggersOverlappingParallelTriggers =
                    MutableList(parallelTriggers.size) { mutableSetOf<Int>() }

                for ((triggerIndex, trigger) in parallelTriggers.withIndex()) {

                    otherTriggerLoop@ for ((otherTriggerIndex, otherTrigger) in parallelTriggers.withIndex()) {

                        for ((keyIndex, key) in trigger.keys.withIndex()) {
                            var lastMatchedIndex: Int? = null

                            for ((otherKeyIndex, otherKey) in otherTrigger.keys.withIndex()) {
                                if (otherKey.matchesWithOtherKey(key)) {

                                    //the other trigger doesn't overlap after the first element
                                    if (otherKeyIndex == 0) continue@otherTriggerLoop

                                    //make sure the overlap retains the order of the trigger
                                    if (lastMatchedIndex != null && lastMatchedIndex != otherKeyIndex - 1) {
                                        continue@otherTriggerLoop
                                    }

                                    if (keyIndex == trigger.keys.lastIndex) {
                                        parallelTriggersOverlappingParallelTriggers[triggerIndex].add(
                                            otherTriggerIndex
                                        )
                                    }

                                    lastMatchedIndex = otherKeyIndex
                                }
                            }
                        }
                    }
                }

                parallelTriggers.forEachIndexed { triggerIndex, trigger ->
                    trigger.keys.forEachIndexed { keyIndex, key ->
                        if (isModifierKey(key.keyCode)) {
                            parallelTriggerModifierKeyIndices.add(triggerIndex to keyIndex)
                        }
                    }
                }

                detectSequenceTriggers = sequenceTriggers.isNotEmpty()
                this.sequenceTriggers = sequenceTriggers.toTypedArray()
                this.sequenceTriggerActions = sequenceTriggerActions.toTypedArray()
                this.sequenceTriggerConstraints = sequenceTriggerConstraints.toTypedArray()
                this.sequenceTriggersOverlappingSequenceTriggers =
                    sequenceTriggersOverlappingSequenceTriggers.map { it.toIntArray() }
                        .toTypedArray()

                this.sequenceTriggersOverlappingParallelTriggers =
                    sequenceTriggersOverlappingParallelTriggers.map { it.toIntArray() }
                        .toTypedArray()


                detectParallelTriggers = parallelTriggers.isNotEmpty()
                this.parallelTriggers = parallelTriggers.toTypedArray()
                this.parallelTriggerActions = parallelTriggerActions.toTypedArray()
                this.parallelTriggerConstraints = parallelTriggerConstraints.toTypedArray()
                this.parallelTriggerModifierKeyIndices =
                    parallelTriggerModifierKeyIndices.toTypedArray()
                this.parallelTriggersOverlappingParallelTriggers =
                    parallelTriggersOverlappingParallelTriggers
                        .map { it.toIntArray() }
                        .toTypedArray()

                detectSequenceLongPresses = longPressSequenceTriggerKeys.isNotEmpty()
                this.longPressSequenceTriggerKeys = longPressSequenceTriggerKeys.toTypedArray()

                detectSequenceDoublePresses = doublePressKeys.isNotEmpty()
                this.doublePressTriggerKeys = doublePressKeys.toTypedArray()

                reset()
            }

            field = value
        }

    private var detectKeyMaps = false
    private var detectInternalEvents = false
    private var detectExternalEvents = false
    private var detectSequenceTriggers = false
    private var detectSequenceLongPresses = false
    private var detectSequenceDoublePresses = false

    private var detectParallelTriggers = false

    /**
     * All sequence events that have the long press click type.
     */
    private var longPressSequenceTriggerKeys = arrayOf<TriggerKey>()

    /**
     * All double press keys and the index of their corresponding trigger. first is the event and second is
     * the trigger index.
     */
    private var doublePressTriggerKeys = arrayOf<TriggerKeyLocation>()

    /**
     * order matches with [doublePressTriggerKeys]
     */
    private var doublePressEventStates = intArrayOf()

    /**
     * The user has an amount of time to double press a key before it is registered as a double press.
     * The order matches with [doublePressTriggerKeys]. This array stores the time when the corresponding trigger will
     * timeout. If the key isn't waiting to timeout, the value is -1.
     */
    private var doublePressTimeoutTimes = longArrayOf()

    private var actionMap = SparseArrayCompat<KeyMapAction>()

    /**
     * The events to detect for each sequence trigger.
     */
    private var sequenceTriggers = arrayOf<KeyMapTrigger>()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [sequenceTriggers].
     */
    private var sequenceTriggerActions = arrayOf<IntArray>()

    /**
     * Sequence triggers timeout after the first key has been pressed. The order matches with [sequenceTriggers].
     * This array stores the time when the corresponding trigger in will timeout. If the trigger in
     * isn't waiting to timeout, the value is -1.
     */
    private var sequenceTriggersTimeoutTimes = longArrayOf()

    /**
     * The indexes of triggers that overlap after the first element with each trigger in [sequenceTriggers]
     */
    private var sequenceTriggersOverlappingSequenceTriggers = arrayOf<IntArray>()

    private var sequenceTriggersOverlappingParallelTriggers = arrayOf<IntArray>()

    /**
     * An array of the index of the last matched event in each sequence trigger.
     */
    private var lastMatchedSequenceEventIndices = intArrayOf()

    private var sequenceTriggerConstraints = arrayOf<ConstraintState>()

    /**
     * The events to detect for each parallel trigger.
     */
    private var parallelTriggers = arrayOf<KeyMapTrigger>()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [parallelTriggers].
     */
    private var parallelTriggerActions = arrayOf<IntArray>()

    private var parallelTriggerConstraints = arrayOf<ConstraintState>()

    /**
     * Stores whether each event in each parallel trigger need to be "released" after being held down.
     * The order matches with [parallelTriggers].
     */
    private var parallelTriggerEventsAwaitingRelease = arrayOf<BooleanArray>()

    /**
     * An array of the index of the last matched event in each parallel trigger.
     */
    private var lastMatchedParallelEventIndices = intArrayOf()

    private var parallelTriggerModifierKeyIndices = arrayOf<Pair<Int, Int>>()

    /**
     * The indexes of triggers that overlap after the first element with each trigger in [parallelTriggers]
     */
    private var parallelTriggersOverlappingParallelTriggers = arrayOf<IntArray>()

    private var modifierKeyEventActions = false
    private var notModifierKeyEventActions = false
    private var keyCodesToImitateUpAction = mutableSetOf<Int>()
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

    private val currentTime: Long
        get() = useCase.currentTime

    private val defaultRepeatDelay: StateFlow<Long> =
        useCase.defaultRepeatDelay.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.REPEAT_DELAY.toLong()
        )

    private val defaultRepeatRate: StateFlow<Long> =
        useCase.defaultRepeatRate.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.REPEAT_RATE.toLong()
        )

    private val defaultHoldDownDuration: StateFlow<Long> =
        useCase.defaultHoldDownDuration.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.HOLD_DOWN_DURATION.toLong()
        )

    private val defaultVibrateDuration: StateFlow<Long> =
        useCase.defaultVibrateDuration.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.VIBRATION_DURATION.toLong()
        )

    private val defaultSequenceTriggerTimeout: StateFlow<Long> =
        useCase.defaultSequenceTriggerTimeout.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT.toLong()
        )

    private val defaultLongPressDelay: StateFlow<Long> =
        useCase.defaultLongPressDelay.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.LONG_PRESS_DELAY.toLong()
        )

    private val defaultDoublePressDelay: StateFlow<Long> =
        useCase.defaultDoublePressDelay.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.DOUBLE_PRESS_DELAY.toLong()
        )

    private val forceVibrate: StateFlow<Boolean> =
        useCase.forceVibrate.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.FORCE_VIBRATE
        )

    init {
        coroutineScope.launch {
            useCase.allKeyMapList.collectLatest { keyMapList ->
                reset()
                this@KeyMapController.keyMapList = keyMapList
            }
        }
    }

    /**
     * @return whether to consume the [KeyEvent].
     */
    fun onKeyEvent(
        keyCode: Int,
        action: Int,
        descriptor: String,
        isExternal: Boolean,
        metaState: Int,
        deviceId: Int,
        scanCode: Int = 0
    ): Boolean {
        if (!detectKeyMaps) return false

        if ((isExternal && !detectExternalEvents) || (!isExternal && !detectInternalEvents)) {
            return false
        }

        metaStateFromKeyEvent = metaState

        //remove the metastate from any modifier keys that remapped and are pressed down
        parallelTriggerModifierKeyIndices.forEach {
            val triggerIndex = it.first
            val eventIndex = it.second
            val event = parallelTriggers[triggerIndex].keys[eventIndex]

            if (parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex]) {
                metaStateFromKeyEvent =
                    metaStateFromKeyEvent.minusFlag(KeyEventUtils.modifierKeycodeToMetaState(event.keyCode))
            }
        }

        val event =
            if (isExternal) {
                Event(keyCode, null, descriptor)
            } else {
                Event(
                    keyCode,
                    null,
                    null
                )
            }

        when (action) {
            KeyEvent.ACTION_DOWN -> return onKeyDown(event, deviceId, scanCode)
            KeyEvent.ACTION_UP -> return onKeyUp(event, deviceId, scanCode)
        }

        return false
    }

    /**
     * @return whether to consume the [KeyEvent].
     */
    private fun onKeyDown(event: Event, deviceId: Int, scanCode: Int): Boolean {

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
                sequenceTriggers[triggerIndex].keys.forEachIndexed { keyIndex, key ->
                    if (key.keyCode == event.keyCode && sequenceTriggers[triggerIndex].keys[keyIndex].consumeKeyEvent) {
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
                val eventLocation = doublePressTriggerKeys[doublePressEventIndex]
                val doublePressEvent =
                    sequenceTriggers[eventLocation.triggerIndex].keys[eventLocation.keyIndex]
                val triggerIndex = eventLocation.triggerIndex

                sequenceTriggers[triggerIndex].keys.forEachIndexed { eventIndex, event ->
                    if (event == doublePressEvent
                        && sequenceTriggers[triggerIndex].keys[eventIndex].consumeKeyEvent
                    ) {
                        consumeEvent = true
                    }
                }
            }
        }

        var awaitingLongPress = false
        var showToast = false
        val detectedShortPressTriggers = mutableSetOf<Int>()
        val vibrateDurations = mutableListOf<Long>()

        /* cache whether an action can be performed to avoid repeatedly checking when multiple triggers have the
        same action */
        val canActionBePerformed = SparseArrayCompat<Result<ActionEntity>>()

        if (detectParallelTriggers) {

            //only process keymaps if an action can be performed
            triggerLoop@ for ((triggerIndex, lastMatchedIndex) in lastMatchedParallelEventIndices.withIndex()) {

                for (overlappingTriggerIndex in sequenceTriggersOverlappingParallelTriggers[triggerIndex]) {
                    if (lastMatchedSequenceEventIndices[overlappingTriggerIndex] != -1) {
                        continue@triggerLoop
                    }
                }

                for (overlappingTriggerIndex in parallelTriggersOverlappingParallelTriggers[triggerIndex]) {
                    if (lastMatchedParallelEventIndices[overlappingTriggerIndex] != -1) {
                        continue@triggerLoop
                    }
                }

                val constraints = parallelTriggerConstraints[triggerIndex]

                if (!detectConstraints.isSatisfied(constraints)) continue

                for (actionKey in parallelTriggerActions[triggerIndex]) {
                    if (canActionBePerformed[actionKey] == null) {
                        val action = actionMap[actionKey] ?: continue

                        val result = performActions.getError(action.data)
                        canActionBePerformed.put(actionKey, result)

                        if (result != null) {
                            continue@triggerLoop
                        }
                    } else if (canActionBePerformed.get(actionKey, null) is Error) {
                        continue@triggerLoop
                    }
                }

                val nextIndex = lastMatchedIndex + 1

                //Perform short press action

                if (parallelTriggers[triggerIndex].matchingEventAtIndex(
                        event.withShortPress,
                        nextIndex
                    )
                ) {

                    if (parallelTriggers[triggerIndex].keys[nextIndex].consumeKeyEvent) {
                        consumeEvent = true
                    }

                    lastMatchedParallelEventIndices[triggerIndex] = nextIndex
                    parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true

                    if (nextIndex == parallelTriggers[triggerIndex].keys.lastIndex) {
                        mappedToParallelTriggerAction = true

                        val actionKeys = parallelTriggerActions[triggerIndex]

                        actionKeys.forEach { actionKey ->
                            val action = actionMap[actionKey] ?: return@forEach

                            if (action.data is KeyEventAction) {
                                val actionKeyCode = action.data.keyCode

                                if (isModifierKey(actionKeyCode)) {
                                    val actionMetaState =
                                        KeyEventUtils.modifierKeycodeToMetaState(actionKeyCode)
                                    metaStateFromActions =
                                        metaStateFromActions.withFlag(actionMetaState)
                                }
                            }

                            detectedShortPressTriggers.add(triggerIndex)

                            val vibrateDuration = when {
                                parallelTriggers[triggerIndex].vibrate -> {
                                    vibrateDuration(parallelTriggers[triggerIndex])
                                }

                                forceVibrate.value -> defaultVibrateDuration.value
                                else -> -1L
                            }

                            vibrateDurations.add(vibrateDuration)
                        }
                    }
                }

                //Perform long press action
                if (parallelTriggers[triggerIndex].matchingEventAtIndex(
                        event.withLongPress,
                        nextIndex
                    )
                ) {

                    if (parallelTriggers[triggerIndex].keys[nextIndex].consumeKeyEvent) {
                        consumeEvent = true
                    }

                    lastMatchedParallelEventIndices[triggerIndex] = nextIndex
                    parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true

                    if (nextIndex == parallelTriggers[triggerIndex].keys.lastIndex) {
                        awaitingLongPress = true

                        if (parallelTriggers[triggerIndex].longPressDoubleVibration
                        ) {
                            useCase.vibrate(vibrateDuration(parallelTriggers[triggerIndex]))
                        }

                        val oldJob = parallelTriggerLongPressJobs[triggerIndex]
                        oldJob?.cancel()
                        parallelTriggerLongPressJobs.put(
                            triggerIndex,
                            performActionsAfterLongPressDelay(triggerIndex)
                        )
                    }
                }
            }
        }

        if (modifierKeyEventActions
            && !isModifierKeyCode
            && metaStateFromActions != 0
            && !mappedToParallelTriggerAction
        ) {

            consumeEvent = true
            keyCodesToImitateUpAction.add(event.keyCode)

            useCase.imitateButtonPress(
                event.keyCode,
                metaStateFromKeyEvent.withFlag(metaStateFromActions),
                deviceId,
                InputEventType.DOWN,
                scanCode
            )

            coroutineScope.launch {
                repeatImitatingKey(event.keyCode, deviceId, scanCode)
            }
        }

        if (detectedShortPressTriggers.isNotEmpty()) {
            val matchingDoublePressEvent = doublePressTriggerKeys.any {
                sequenceTriggers[it.triggerIndex].keys[it.keyIndex].matchesEvent(event.withDoublePress)
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

                    if (parallelTriggers[triggerIndex].showToast) {
                        showToast = true
                    }

                    parallelTriggerActionJobs[triggerIndex]?.cancel()

                    parallelTriggerActionJobs[triggerIndex] = coroutineScope.launch {

                        parallelTriggerActions[triggerIndex].forEachIndexed { index, actionKey ->
                            val action = actionMap[actionKey] ?: return@forEachIndexed

                            var shouldPerformActionNormally = true

                            if (action.holdDown && action.repeat
                                && stopRepeatingWhenPressedAgain(actionKey)
                            ) {

                                shouldPerformActionNormally = false

                                if (actionsBeingHeldDown.contains(actionKey)) {
                                    actionsBeingHeldDown.remove(actionKey)

                                    performAction(
                                        action,
                                        inputEventType = InputEventType.UP,
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
                                        inputEventType = InputEventType.UP,
                                        multiplier = actionMultiplier(actionKey)
                                    )

                                    shouldPerformActionNormally = false
                                }
                            }

                            if (shouldPerformActionNormally) {
                                if (action.holdDown) {
                                    actionsBeingHeldDown.add(actionKey)
                                }

                                val keyEventAction =
                                    if (action.holdDown) {
                                        InputEventType.DOWN
                                    } else {
                                        InputEventType.DOWN_UP
                                    }

                                performAction(
                                    action,
                                    actionMultiplier(actionKey),
                                    keyEventAction
                                )

                                val vibrateDuration = vibrateDurations[index]

                                if (vibrateDuration != -1L) {
                                    useCase.vibrate(vibrateDuration)
                                }

                                if (action.repeat && action.holdDown) {
                                    delay(holdDownDuration(actionKey))

                                    performAction(
                                        action,
                                        1,
                                        InputEventType.UP
                                    )
                                }
                            }

                            delay(delayBeforeNextAction(actionKey))
                        }

                        initialiseRepeating(triggerIndex, calledOnTriggerRelease = false)
                    }
                }
            }
        }

        if (showToast) {
            useCase.showTriggeredToast()
        }

        if (consumeEvent) {
            return true
        }

        if (detectSequenceTriggers) {
            sequenceTriggers.forEachIndexed { triggerIndex, trigger ->
                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) return@forEachIndexed

                trigger.keys.forEachIndexed { keyIndex, key ->
                    val matchingEvent = when {
                        key.matchesEvent(event.withShortPress) -> true
                        key.matchesEvent(event.withLongPress) -> true
                        key.matchesEvent(event.withDoublePress) -> true

                        else -> false
                    }

                    if (matchingEvent && sequenceTriggers[triggerIndex].keys[keyIndex].consumeKeyEvent) {
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
    private fun onKeyUp(event: Event, deviceId: Int, scanCode: Int): Boolean {
        val keyCode = event.keyCode

        val downTime = eventDownTimeMap[event] ?: currentTime
        eventDownTimeMap.remove(event)

        var consumeEvent = false
        var imitateDownUpKeyEvent = false
        var imitateUpKeyEvent = false

        var successfulLongPress = false
        var successfulDoublePress = false
        var mappedToDoublePress = false
        var matchedDoublePressEventIndex = -1
        var shortPressSingleKeyTriggerJustReleased = false
        var longPressSingleKeyTriggerJustReleased = false

        var showToast = false

        val detectedSequenceTriggerIndexes = mutableListOf<Int>()
        val detectedParallelTriggerIndexes = mutableListOf<Int>()

        val vibrateDurations = mutableListOf<Long>()

        val imitateKeyAfterDoublePressTimeout = mutableListOf<Long>()

        var metaStateFromActionsToRemove = 0

        if (keyCodesToImitateUpAction.contains(keyCode)) {
            consumeEvent = true
            imitateUpKeyEvent = true
            keyCodesToImitateUpAction.remove(keyCode)
        }

        if (detectSequenceDoublePresses) {
            //iterate over each possible double press event to detect
            for (index in doublePressTriggerKeys.indices) {
                val eventLocation = doublePressTriggerKeys[index]
                val doublePressKey =
                    sequenceTriggers[eventLocation.triggerIndex].keys[eventLocation.keyIndex]
                val triggerIndex = eventLocation.triggerIndex

                if (!areSequenceTriggerConstraintsSatisfied(triggerIndex)) continue

                if (lastMatchedSequenceEventIndices[triggerIndex] != eventLocation.keyIndex - 1) continue

                if (doublePressKey.matchesEvent(event.withDoublePress)) {
                    mappedToDoublePress = true
                    //increment the double press event state.
                    doublePressEventStates[index] = doublePressEventStates[index] + 1

                    when (doublePressEventStates[index]) {
                        /*if the key is in the single pressed state, set the timeout time and start the timer
                        * to imitate the key if it isn't double pressed in the end */
                        SINGLE_PRESSED -> {

                            /*
                            I just realised that calculating the double press timeout is *SUPPOSED* to be in the onKeyDown
                            method but it has been this way for so long and no one has complained so leave it.
                             Changing this might affect people's key maps in ways that I can't fathom.
                             */

                            val doublePressTimeout =
                                doublePressTimeout(sequenceTriggers[triggerIndex])
                            doublePressTimeoutTimes[index] = currentTime + doublePressTimeout

                            imitateKeyAfterDoublePressTimeout.add(doublePressTimeout)
                            matchedDoublePressEventIndex = index

                            sequenceTriggers[triggerIndex].keys.forEachIndexed { keyIndex, key ->
                                if (key == doublePressKey
                                    && sequenceTriggers[triggerIndex].keys[keyIndex].consumeKeyEvent
                                ) {

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

                if ((currentTime - downTime) >= longPressDelay(sequenceTriggers[triggerIndex])) {
                    successfulLongPress = true
                } else if (detectSequenceLongPresses &&
                    longPressSequenceTriggerKeys.any { it.matchesEvent(event.withLongPress) }
                ) {
                    imitateDownUpKeyEvent = true
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
                if (sequenceTriggers[triggerIndex].matchingEventAtIndex(
                        encodedEventWithClickType,
                        nextIndex
                    )
                ) {

                    if (sequenceTriggers[triggerIndex].keys[nextIndex].consumeKeyEvent) {
                        consumeEvent = true
                    }

                    lastMatchedSequenceEventIndices[triggerIndex] = nextIndex

                    /*
                    If the next index is 0, then the first event in the trigger has been matched, which means the timer
                    needs to start for this trigger.
                     */
                    if (nextIndex == 0) {
                        val startTime = currentTime
                        val timeout = sequenceTriggerTimeout(sequenceTriggers[triggerIndex])

                        sequenceTriggersTimeoutTimes[triggerIndex] = startTime + timeout
                    }

                    /*
                    If the last event in a trigger has been matched, then the action needs to be performed and the timer
                    reset.
                     */
                    if (nextIndex == sequenceTriggers[triggerIndex].keys.lastIndex) {
                        detectedSequenceTriggerIndexes.add(triggerIndex)

                        if (sequenceTriggers[triggerIndex].showToast) {
                            showToast = true
                        }

                        sequenceTriggerActions[triggerIndex].forEachIndexed { index, _ ->

                            val vibrateDuration =
                                if (sequenceTriggers[triggerIndex].vibrate) {
                                    vibrateDuration(sequenceTriggers[triggerIndex])
                                } else {
                                    -1
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
            triggerLoop@ for ((triggerIndex, trigger) in parallelTriggers.withIndex()) {
                val isSingleKeyTrigger = parallelTriggers[triggerIndex].keys.size == 1

                var lastHeldDownEventIndex = -1

                for (keyIndex in trigger.keys.indices) {
                    val awaitingRelease =
                        parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex]

                    //short press
                    if (awaitingRelease && trigger.matchingEventAtIndex(
                            event.withShortPress,
                            keyIndex
                        )
                    ) {
                        if (isSingleKeyTrigger) {
                            shortPressSingleKeyTriggerJustReleased = true
                        }

                        if (modifierKeyEventActions) {
                            val actionKeys = parallelTriggerActions[triggerIndex]
                            actionKeys.forEach { actionKey ->

                                actionMap[actionKey]?.let { action ->
                                    if (action.data is KeyEventAction && isModifierKey(action.data.keyCode)) {
                                        val actionMetaState =
                                            KeyEventUtils.modifierKeycodeToMetaState(action.data.keyCode)
                                        metaStateFromActionsToRemove =
                                            metaStateFromActionsToRemove.withFlag(
                                                actionMetaState
                                            )
                                    }
                                }
                            }
                        }

                        parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex] = false

                        if (parallelTriggers[triggerIndex].keys[keyIndex].consumeKeyEvent) {
                            consumeEvent = true
                        }
                    }

                    //long press
                    if (awaitingRelease && trigger.matchingEventAtIndex(
                            event.withLongPress,
                            keyIndex
                        )
                    ) {

                        if ((currentTime - downTime) >= longPressDelay(parallelTriggers[triggerIndex])) {
                            successfulLongPress = true
                        }

                        parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex] = false

                        parallelTriggerLongPressJobs[triggerIndex]?.cancel()

                        if (parallelTriggers[triggerIndex].keys[keyIndex].consumeKeyEvent) {
                            consumeEvent = true
                        }

                        val lastMatchedIndex = lastMatchedParallelEventIndices[triggerIndex]

                        if (isSingleKeyTrigger && successfulLongPress) {
                            longPressSingleKeyTriggerJustReleased = true
                        }

                        if (!imitateDownUpKeyEvent) {
                            if (isSingleKeyTrigger && !successfulLongPress) {
                                imitateDownUpKeyEvent = true
                            } else if (lastMatchedIndex > -1 &&
                                lastMatchedIndex < parallelTriggers[triggerIndex].keys.lastIndex
                            ) {
                                imitateDownUpKeyEvent = true
                            }
                        }
                    }

                    if (parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex] &&
                        lastHeldDownEventIndex == keyIndex - 1
                    ) {

                        lastHeldDownEventIndex = keyIndex
                    }
                }

                lastMatchedParallelEventIndices[triggerIndex] = lastHeldDownEventIndex
                metaStateFromActions = metaStateFromActions.minusFlag(metaStateFromActionsToRemove)

                //cancel repeating action jobs for this trigger
                if (lastHeldDownEventIndex != parallelTriggers[triggerIndex].keys.lastIndex) {
                    repeatJobs[triggerIndex]?.forEach {
                        if (!stopRepeatingWhenPressedAgain(it.actionKey)) {
                            it.cancel()
                        }
                    }

                    val actionKeys = parallelTriggerActions[triggerIndex]

                    actionKeys.forEach { actionKey ->
                        val action = actionMap[actionKey] ?: return@forEach

                        if (!actionsBeingHeldDown.contains(actionKey)) return@forEach

                        if (action.holdDown && !holdDownUntilPressedAgain(actionKey)
                        ) {

                            actionsBeingHeldDown.remove(actionKey)

                            performAction(
                                action,
                                actionMultiplier(actionKey),
                                InputEventType.UP
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
                val lastKey = parallelTriggers[triggerIndex].keys.last()

                if (lastKey.matchesEvent(event.withShortPress)) {
                    detectedParallelTriggerIndexes.add(triggerIndex)

                    if (parallelTriggers[triggerIndex].showToast) {
                        showToast = true
                    }

                    parallelTriggerActions[triggerIndex].forEachIndexed { actionIndex, _ ->

                        val vibrateDuration =
                            if (parallelTriggers[triggerIndex].vibrate) {
                                vibrateDuration(parallelTriggers[triggerIndex])
                            } else {
                                -1
                            }
                        vibrateDurations.add(actionIndex, vibrateDuration)
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

                    performAction(action, actionMultiplier(actionKey))

                    if (vibrateDurations[index] != -1L || forceVibrate.value) {
                        useCase.vibrate(vibrateDurations[index])
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

                    performAction(action, actionMultiplier(actionKey))

                    if (vibrateDurations[index] != -1L || forceVibrate.value) {
                        useCase.vibrate(vibrateDurations[index])
                    }

                    delay(delayBeforeNextAction(actionKey))
                }
            }

            initialiseRepeating(triggerIndex, calledOnTriggerRelease = true)
        }

        if (showToast) {
            useCase.showTriggeredToast()
        }

        if (imitateKeyAfterDoublePressTimeout.isNotEmpty()
            && detectedSequenceTriggerIndexes.isEmpty()
            && detectedParallelTriggerIndexes.isEmpty()
            && !longPressSingleKeyTriggerJustReleased
        ) {

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

                    useCase.imitateButtonPress(
                        keyCode,
                        keyEventAction = InputEventType.DOWN_UP,
                        scanCode = scanCode
                    )
                }
            }
        }
        //only imitate a key if an action isn't going to be performed
        else if ((imitateDownUpKeyEvent || imitateUpKeyEvent)
            && detectedSequenceTriggerIndexes.isEmpty()
            && detectedParallelTriggerIndexes.isEmpty()
            && !shortPressSingleKeyTriggerJustReleased
            && !mappedToDoublePress
        ) {

            val keyEventAction = if (imitateUpKeyEvent) {
                InputEventType.UP
            } else {
                InputEventType.DOWN_UP
            }

            useCase.imitateButtonPress(
                keyCode,
                metaStateFromKeyEvent.withFlag(metaStateFromActions),
                deviceId,
                keyEventAction,
                scanCode
            )

            keyCodesToImitateUpAction.remove(event.keyCode)
        }

        return consumeEvent
    }

    fun reset() {
        doublePressEventStates = IntArray(doublePressTriggerKeys.size) { NOT_PRESSED }
        doublePressTimeoutTimes = LongArray(doublePressTriggerKeys.size) { -1L }

        sequenceTriggersTimeoutTimes = LongArray(sequenceTriggers.size) { -1 }
        lastMatchedSequenceEventIndices = IntArray(sequenceTriggers.size) { -1 }

        lastMatchedParallelEventIndices = IntArray(parallelTriggers.size) { -1 }
        parallelTriggerEventsAwaitingRelease = Array(parallelTriggers.size) {
            BooleanArray(parallelTriggers[it].keys.size) { false }
        }

        performActionsOnFailedDoublePress.clear()
        performActionsOnFailedLongPress.clear()

        actionsBeingHeldDown.forEach {
            val action = actionMap[it] ?: return@forEach

            performAction(
                action,
                multiplier = 1,
                inputEventType = InputEventType.UP
            )
        }

        actionsBeingHeldDown = mutableSetOf()

        metaStateFromActions = 0
        metaStateFromKeyEvent = 0
        keyCodesToImitateUpAction = mutableSetOf()

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
        var showToast = false
        val detectedTriggerIndexes = mutableListOf<Int>()
        val vibrateDurations = mutableListOf<Long>()

        performActionsOnFailedDoublePress.forEach { triggerIndex ->
            if (parallelTriggers[triggerIndex].keys.last().matchesEvent(event.withShortPress)) {
                detectedTriggerIndexes.add(triggerIndex)

                if (parallelTriggers[triggerIndex].showToast) {
                    showToast = true
                }

                parallelTriggerActions[triggerIndex].forEach { _ ->

                    val vibrateDuration =
                        if (parallelTriggers[triggerIndex].vibrate) {
                            vibrateDuration(parallelTriggers[triggerIndex])
                        } else {
                            -1
                        }

                    vibrateDurations.add(vibrateDuration)
                }
            }
        }

        performActionsOnFailedDoublePress.clear()

        if (showToast) {
            useCase.showTriggeredToast()
        }

        detectedTriggerIndexes.forEach { triggerIndex ->
            parallelTriggerActionJobs[triggerIndex]?.cancel()

            parallelTriggerActionJobs[triggerIndex] = coroutineScope.launch {
                parallelTriggerActions[triggerIndex].forEachIndexed { index, actionKey ->

                    val action = actionMap[actionKey] ?: return@forEachIndexed

                    performAction(action, actionMultiplier(actionKey))

                    if (vibrateDurations[index] != -1L || forceVibrate.value) {
                        useCase.vibrate(vibrateDurations[index])
                    }

                    delay(delayBeforeNextAction(actionKey))
                }
            }

            initialiseRepeating(triggerIndex, calledOnTriggerRelease = true)
        }

        return detectedTriggerIndexes.isNotEmpty()
    }

    private fun encodeActionList(actions: List<KeyMapAction>): IntArray {
        return actions.map { getActionKey(it) }.toIntArray()
    }

    /**
     * @return the key for the action in [actionMap]. Returns -1 if the [action] can't be found.
     */
    private fun getActionKey(action: KeyMapAction): Int {
        actionMap.keyIterator().forEach { key ->
            if (actionMap[key] == action) {
                return key
            }
        }

        throw Exception("Action $action not in the action map!")
    }

    private suspend fun repeatImitatingKey(keyCode: Int, deviceId: Int, scanCode: Int) {
        delay(400)

        while (keyCodesToImitateUpAction.contains(keyCode)) {
            useCase.imitateButtonPress(
                keyCode,
                metaStateFromKeyEvent.withFlag(metaStateFromActions),
                deviceId,
                InputEventType.DOWN,
                scanCode
            ) //use down action because this is what Android does

            delay(50)
        }
    }

    private fun repeatAction(actionKey: Int) = RepeatJob(actionKey) {
        coroutineScope.launch {
            val repeat = actionMap[actionKey]?.repeat ?: return@launch
            if (!repeat) return@launch

            delay(repeatDelay(actionKey))

            while (true) {
                actionMap[actionKey]?.let { action ->

                    if (action.data is KeyEventAction) {
                        if (isModifierKey(action.data.keyCode)) return@let
                    }

                    if (action.holdDown && action.repeat) {
                        val holdDownDuration = holdDownDuration(actionKey)

                        performAction(action, actionMultiplier(actionKey), InputEventType.DOWN)
                        delay(holdDownDuration)
                        performAction(action, actionMultiplier(actionKey), InputEventType.UP)
                    } else {
                        performAction(action, actionMultiplier(actionKey))
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
        delay(longPressDelay(parallelTriggers[triggerIndex]))

        val actionKeys = parallelTriggerActions[triggerIndex]
        var showToast = false

        parallelTriggerActionJobs[triggerIndex]?.cancel()

        parallelTriggerActionJobs[triggerIndex] = coroutineScope.launch {

            if (parallelTriggers[triggerIndex].showToast) {
                showToast = true
            }

            actionKeys.forEach { actionKey ->
                val action = actionMap[actionKey] ?: return@forEach

                var performActionNormally = true

                if (holdDownUntilPressedAgain(actionKey)) {
                    if (actionsBeingHeldDown.contains(actionKey)) {
                        actionsBeingHeldDown.remove(actionKey)

                        performAction(
                            action,
                            inputEventType = InputEventType.UP,
                            multiplier = actionMultiplier(actionKey)
                        )

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
                        if (action.holdDown) {
                            InputEventType.DOWN
                        } else {
                            InputEventType.DOWN_UP
                        }

                    performAction(action, actionMultiplier(actionKey), keyEventAction)

                    if (parallelTriggers[triggerIndex].vibrate || forceVibrate.value
                        || parallelTriggers[triggerIndex].longPressDoubleVibration
                    ) {
                        useCase.vibrate(vibrateDuration(parallelTriggers[triggerIndex]))
                    }
                }

                delay(delayBeforeNextAction(actionKey))
            }

            initialiseRepeating(triggerIndex, calledOnTriggerRelease = false)
        }

        if (showToast) {
            useCase.showTriggeredToast()
        }
    }

    /**
     * For parallel triggers only.
     *
     * @param [calledOnTriggerRelease] whether this is called when the trigger was released
     */
    private fun initialiseRepeating(triggerIndex: Int, calledOnTriggerRelease: Boolean) {
        val actionKeys = parallelTriggerActions[triggerIndex]
        val actionKeysToStartRepeating = actionKeys.toMutableSet()

        repeatJobs[triggerIndex]?.forEach {
            if (stopRepeatingWhenPressedAgain(it.actionKey)) {
                actionKeysToStartRepeating.remove(it.actionKey)
            }

            it.cancel()
        }

        val repeatJobs = mutableListOf<RepeatJob>()

        actionKeys.forEach { key ->
            //only start repeating when a trigger is released if it is to repeat until pressed again
            if (!stopRepeatingWhenPressedAgain(key) && calledOnTriggerRelease) {
                actionKeysToStartRepeating.remove(key)
            }
        }

        actionKeysToStartRepeating.forEach {
            repeatJobs.add(repeatAction(it))
        }

        this.repeatJobs.put(triggerIndex, repeatJobs)
    }

    private fun KeyMapTrigger.matchingEventAtIndex(event: Event, index: Int): Boolean {
        if (index >= this.keys.size) return false

        val key = this.keys[index]

        return key.matchesEvent(event)
    }

    private fun TriggerKey.matchesEvent(event: Event): Boolean {
        return when (this.device) {
            TriggerKeyDevice.Any -> this.keyCode == event.keyCode && this.clickType == event.clickType
            is TriggerKeyDevice.External ->
                this.keyCode == event.keyCode
                    && event.descriptor != null
                    && event.descriptor == this.device.descriptor
                    && this.clickType == event.clickType

            TriggerKeyDevice.Internal ->
                this.keyCode == event.keyCode
                    && event.descriptor == null
                    && this.clickType == event.clickType
        }
    }

    private fun TriggerKey.matchesWithOtherKey(otherKey: TriggerKey): Boolean {
        return when (this.device) {
            TriggerKeyDevice.Any -> this.keyCode == otherKey.keyCode
                && this.clickType == otherKey.clickType

            is TriggerKeyDevice.External ->
                this.keyCode == otherKey.keyCode
                    && this.device == otherKey.device
                    && this.clickType == otherKey.clickType

            TriggerKeyDevice.Internal ->
                this.keyCode == otherKey.keyCode
                    && otherKey.device == TriggerKeyDevice.Internal
                    && this.clickType == otherKey.clickType
        }
    }

    private fun performAction(
        action: KeyMapAction,
        multiplier: Int,
        inputEventType: InputEventType = InputEventType.DOWN_UP
    ) {
        val additionalMetaState = metaStateFromKeyEvent.withFlag(metaStateFromActions)

        repeat(multiplier) {
            performActions.perform(action.data, inputEventType, additionalMetaState)
        }
    }


    private fun stopRepeatingWhenPressedAgain(actionKey: Int) =
        actionMap.get(actionKey)?.stopRepeatingWhenTriggerPressedAgain ?: false

    private fun holdDownUntilPressedAgain(actionKey: Int) =
        actionMap.get(actionKey)?.stopHoldDownWhenTriggerPressedAgain ?: false

    private fun actionMultiplier(actionKey: Int): Int {
        return actionMap.get(actionKey)?.multiplier ?: 1
    }

    private fun repeatDelay(actionKey: Int): Long {
        return actionMap.get(actionKey)?.repeatDelay?.toLong() ?: defaultRepeatDelay.value
    }

    private fun repeatRate(actionKey: Int): Long {
        return actionMap.get(actionKey)?.repeatRate?.toLong() ?: defaultRepeatRate.value
    }

    private fun delayBeforeNextAction(actionKey: Int): Long {
        return actionMap.get(actionKey)?.delayBeforeNextAction?.toLong() ?: 0L

    }

    private fun holdDownDuration(actionKey: Int): Long {
        return actionMap.get(actionKey)?.holdDownDuration?.toLong() ?: defaultHoldDownDuration.value
    }

    private fun longPressDelay(trigger: KeyMapTrigger): Long {
        return trigger.longPressDelay?.toLong() ?: defaultLongPressDelay.value
    }

    private fun doublePressTimeout(trigger: KeyMapTrigger): Long {
        return trigger.doublePressDelay?.toLong() ?: defaultDoublePressDelay.value
    }

    private fun vibrateDuration(trigger: KeyMapTrigger): Long {
        return trigger.vibrateDuration?.toLong() ?: defaultVibrateDuration.value
    }

    private fun sequenceTriggerTimeout(trigger: KeyMapTrigger): Long {
        return trigger.sequenceTriggerTimeout?.toLong() ?: defaultSequenceTriggerTimeout.value
    }

    private fun setActionMapAndOptions(actions: Set<KeyMapAction>) {
        var key = 0

        val map = SparseArrayCompat<KeyMapAction>()

        actions.forEach { action ->
            map.put(key, action)

            key++
        }

        actionMap = map
    }

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

    private fun areSequenceTriggerConstraintsSatisfied(triggerIndex: Int): Boolean {
        val constraints = sequenceTriggerConstraints[triggerIndex]

        return detectConstraints.isSatisfied(constraints)
    }

    private val Event.withShortPress: Event
        get() = copy(clickType = ClickType.SHORT_PRESS)

    private val Event.withLongPress: Event
        get() = copy(clickType = ClickType.LONG_PRESS)

    private val Event.withDoublePress: Event
        get() = copy(clickType = ClickType.DOUBLE_PRESS)

    private data class Event(
        val keyCode: Int,
        val clickType: ClickType?,
        /**
         * null if not an external device
         */
        val descriptor: String?
    )

    private class RepeatJob(val actionKey: Int, launch: () -> Job) : Job by launch.invoke()
    private data class TriggerKeyLocation(val triggerIndex: Int, val keyIndex: Int)
}