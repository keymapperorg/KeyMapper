package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
import androidx.collection.SparseArrayCompat
import androidx.collection.keyIterator
import androidx.collection.valueIterator
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.ConstraintSnapshot
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapTrigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
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
import kotlin.collections.set

/**
 * Created by sds100 on 05/05/2020.
 */

class KeyMapController(
    private val coroutineScope: CoroutineScope,
    private val useCase: DetectKeyMapsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
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

                val triggers = mutableListOf<KeyMapTrigger>()
                val sequenceTriggers = mutableListOf<Int>()
                val parallelTriggers = mutableListOf<Int>()

                val triggerActions = mutableListOf<IntArray>()
                val triggerConstraints = mutableListOf<ConstraintState>()

                val sequenceTriggerActionPerformers = mutableMapOf<Int, SequenceTriggerActionPerformer>()
                val parallelTriggerActionPerformers = mutableMapOf<Int, ParallelTriggerActionPerformer>()
                val parallelTriggerModifierKeyIndices = mutableListOf<Pair<Int, Int>>()

                //Only process key maps that can be triggered
                val validKeyMaps = value.filter {
                    it.actionList.isNotEmpty() && it.isEnabled
                }

                for ((triggerIndex, keyMap) in validKeyMaps.withIndex()) {

                    //TRIGGER STUFF
                    keyMap.trigger.keys.forEachIndexed { keyIndex, key ->
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
                            doublePressKeys.add(TriggerKeyLocation(triggerIndex, keyIndex))
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

                    if (keyMap.actionList.any { it.data is ActionData.InputKeyEvent && isModifierKey(it.data.keyCode) }) {
                        modifierKeyEventActions = true
                    }

                    if (keyMap.actionList.any { it.data is ActionData.InputKeyEvent && !isModifierKey(it.data.keyCode) }) {
                        notModifierKeyEventActions = true
                    }

                    triggers.add(keyMap.trigger)
                    triggerActions.add(encodedActionList)
                    triggerConstraints.add(keyMap.constraintState)

                    if (performActionOnDown(keyMap.trigger)) {
                        parallelTriggers.add(triggerIndex)
                        parallelTriggerActionPerformers[triggerIndex] = ParallelTriggerActionPerformer(
                            coroutineScope,
                            performActionsUseCase,
                            keyMap.actionList
                        )

                    } else {
                        sequenceTriggers.add(triggerIndex)
                        sequenceTriggerActionPerformers[triggerIndex] = SequenceTriggerActionPerformer(
                            coroutineScope,
                            performActionsUseCase,
                            keyMap.actionList
                        )
                    }
                }

                val sequenceTriggersOverlappingSequenceTriggers =
                    MutableList(triggers.size) { mutableSetOf<Int>() }

                for (triggerIndex in sequenceTriggers) {
                    val trigger = triggers[triggerIndex]

                    otherTriggerLoop@ for (otherTriggerIndex in sequenceTriggers) {
                        val otherTrigger = triggers[otherTriggerIndex]

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
                    MutableList(triggers.size) { mutableSetOf<Int>() }

                for (triggerIndex in parallelTriggers) {
                    val trigger = triggers[triggerIndex]

                    otherTriggerLoop@ for (otherTriggerIndex in sequenceTriggers) {
                        val otherTrigger = triggers[otherTriggerIndex]

                        //Don't compare a trigger to itself
                        if (triggerIndex == otherTriggerIndex) {
                            continue@otherTriggerLoop
                        }

                        for ((keyIndex, key) in trigger.keys.withIndex()) {
                            var lastMatchedIndex: Int? = null

                            for ((otherKeyIndex, otherKey) in otherTrigger.keys.withIndex()) {

                                if (key.matchesWithOtherKey(otherKey)) {

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

                                //if there were no matching keys in the other trigger then skip this trigger
                                if (lastMatchedIndex == null && otherKeyIndex == otherTrigger.keys.lastIndex) {
                                    continue@otherTriggerLoop
                                }
                            }
                        }
                    }
                }

                val parallelTriggersOverlappingParallelTriggers =
                    MutableList(triggers.size) { mutableSetOf<Int>() }

                for (triggerIndex in parallelTriggers) {
                    val trigger = triggers[triggerIndex]

                    otherTriggerLoop@ for (otherTriggerIndex in parallelTriggers) {
                        val otherTrigger = triggers[otherTriggerIndex]

                        //Don't compare a trigger to itself
                        if (triggerIndex == otherTriggerIndex) {
                            continue@otherTriggerLoop
                        }

                        //only check for overlapping if the other trigger has more keys
                        if (otherTrigger.keys.size <= trigger.keys.size) {
                            continue@otherTriggerLoop
                        }

                        for ((keyIndex, key) in trigger.keys.withIndex()) {
                            var lastMatchedIndex: Int? = null

                            for ((otherKeyIndex, otherKey) in otherTrigger.keys.withIndex()) {
                                if (otherKey.matchesWithOtherKey(key)) {

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

                                //if there were no matching keys in the other trigger then skip this trigger
                                if (lastMatchedIndex == null && otherKeyIndex == otherTrigger.keys.lastIndex) {
                                    continue@otherTriggerLoop
                                }
                            }
                        }
                    }
                }

                parallelTriggers.forEach { triggerIndex ->
                    val trigger = triggers[triggerIndex]

                    trigger.keys.forEachIndexed { keyIndex, key ->
                        if (isModifierKey(key.keyCode)) {
                            parallelTriggerModifierKeyIndices.add(triggerIndex to keyIndex)
                        }
                    }
                }

                reset()

                this.triggers = triggers.toTypedArray()
                this.triggerActions = triggerActions.toTypedArray()
                this.triggerConstraints = triggerConstraints.toTypedArray()

                this.sequenceTriggers = sequenceTriggers.toIntArray()
                this.sequenceTriggersOverlappingSequenceTriggers =
                    sequenceTriggersOverlappingSequenceTriggers.map { it.toIntArray() }
                        .toTypedArray()

                this.sequenceTriggersOverlappingParallelTriggers =
                    sequenceTriggersOverlappingParallelTriggers.map { it.toIntArray() }
                        .toTypedArray()

                this.parallelTriggers = parallelTriggers.toIntArray()
                this.parallelTriggerModifierKeyIndices =
                    parallelTriggerModifierKeyIndices.toTypedArray()

                this.parallelTriggersOverlappingParallelTriggers =
                    parallelTriggersOverlappingParallelTriggers
                        .map { it.toIntArray() }
                        .toTypedArray()

                parallelTriggersAwaitingReleaseAfterBeingTriggered =
                    BooleanArray(triggers.size)

                detectSequenceLongPresses = longPressSequenceTriggerKeys.isNotEmpty()
                this.longPressSequenceTriggerKeys = longPressSequenceTriggerKeys.toTypedArray()

                detectSequenceDoublePresses = doublePressKeys.isNotEmpty()
                this.doublePressTriggerKeys = doublePressKeys.toTypedArray()

                this.parallelTriggerActionPerformers = parallelTriggerActionPerformers
                this.sequenceTriggerActionPerformers = sequenceTriggerActionPerformers

                reset()
            }

            field = value
        }

    private var detectKeyMaps: Boolean = false
    private var detectInternalEvents: Boolean = false
    private var detectExternalEvents: Boolean = false
    private var detectSequenceLongPresses: Boolean = false
    private var detectSequenceDoublePresses: Boolean = false

    /**
     * All sequence events that have the long press click type.
     */
    private var longPressSequenceTriggerKeys: Array<TriggerKey> = arrayOf<TriggerKey>()

    /**
     * All double press keys and the index of their corresponding trigger. first is the event and second is
     * the trigger index.
     */
    private var doublePressTriggerKeys: Array<TriggerKeyLocation> = arrayOf<TriggerKeyLocation>()

    /**
     * order matches with [doublePressTriggerKeys]
     */
    private var doublePressEventStates: IntArray = intArrayOf()

    /**
     * The user has an amount of time to double press a key before it is registered as a double press.
     * The order matches with [doublePressTriggerKeys]. This array stores the time when the corresponding trigger will
     * timeout. If the key isn't waiting to timeout, the value is -1.
     */
    private var doublePressTimeoutTimes = longArrayOf()

    private var actionMap = SparseArrayCompat<KeyMapAction>()
    private var triggers: Array<KeyMapTrigger> = emptyArray()

    /**
     * The events to detect for each sequence trigger.
     */
    private var sequenceTriggers: IntArray = intArrayOf()

    /**
     * Sequence triggers timeout after the first key has been pressed.
     * This map stores the time when the corresponding trigger will timeout. If the trigger in
     * isn't waiting to timeout, the value is -1.
     * The index of a trigger matches with the index in [triggers]
     */
    private var sequenceTriggersTimeoutTimes: MutableMap<Int, Long> = mutableMapOf()

    /**
     * The indexes of triggers that overlap after the first element with each trigger in [sequenceTriggers]
     */
    private var sequenceTriggersOverlappingSequenceTriggers = arrayOf<IntArray>()

    private var sequenceTriggersOverlappingParallelTriggers = arrayOf<IntArray>()

    /**
     * An array of the index of the last matched event in each trigger.
     */
    private var lastMatchedEventIndices: IntArray = intArrayOf()

    /**
     * An array of the constraints for every trigger
     */
    private var triggerConstraints: Array<ConstraintState> = arrayOf<ConstraintState>()

    /**
     * The events to detect for each parallel trigger.
     */
    private var parallelTriggers: IntArray = intArrayOf()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [triggers].
     */
    private var triggerActions: Array<IntArray> = arrayOf<IntArray>()

    /**
     * Stores whether each event in each parallel trigger need to be released after being held down.
     * The index of a trigger matches with the index in [triggers]
     */
    private var parallelTriggerEventsAwaitingRelease: Array<BooleanArray> = emptyArray()

    /**
     * Whether each parallel trigger is awaiting to be released after performing an action.
     * This is only set to true if the trigger has been successfully triggered and *all* the keys
     * have not been released.
     * The index of a trigger matches with the index in [triggers]
     */
    private var parallelTriggersAwaitingReleaseAfterBeingTriggered: BooleanArray = booleanArrayOf()

    private var parallelTriggerModifierKeyIndices: Array<Pair<Int, Int>> = arrayOf()

    /**
     * The indexes of triggers that overlap after the first element with each trigger in [parallelTriggers]
     */
    private var parallelTriggersOverlappingParallelTriggers = arrayOf<IntArray>()

    private var modifierKeyEventActions: Boolean = false
    private var notModifierKeyEventActions: Boolean = false
    private var keyCodesToImitateUpAction: MutableSet<Int> = mutableSetOf<Int>()
    private var metaStateFromActions: Int = 0
    private var metaStateFromKeyEvent: Int = 0

    private val eventDownTimeMap: MutableMap<Event, Long> = mutableMapOf<Event, Long>()

    /**
     * The indexes of parallel triggers that didn't have their actions performed because there is a matching trigger but
     * for a long-press. These actions should only be performed if the long-press fails, otherwise when the user
     * holds down the trigger keys for the long-press trigger, actions from both triggers will be performed.
     */
    private val performActionsOnFailedLongPress: MutableSet<Int> = mutableSetOf<Int>()

    /**
     * The indexes of parallel triggers that didn't have their actions performed because there is a matching trigger but
     * for a double-press. These actions should only be performed if the double-press fails, otherwise each time the user
     * presses the keys for the double press, actions from both triggers will be performed.
     */
    private val performActionsOnFailedDoublePress: MutableSet<Int> = mutableSetOf<Int>()

    /**
     * Maps jobs to perform an action after a long press to their corresponding parallel trigger index
     */
    private val parallelTriggerLongPressJobs: SparseArrayCompat<Job> = SparseArrayCompat<Job>()

    private var parallelTriggerActionPerformers: Map<Int, ParallelTriggerActionPerformer> = emptyMap()
    private var sequenceTriggerActionPerformers: Map<Int, SequenceTriggerActionPerformer> = emptyMap()

    private val currentTime: Long
        get() = useCase.currentTime

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
        metaState: Int,
        scanCode: Int = 0,
        device: InputDeviceInfo?
    ): Boolean {
        if (!detectKeyMaps) return false

        if (device != null) {
            if ((device.isExternal && !detectExternalEvents) || (!device.isExternal && !detectInternalEvents)) {
                return false
            }
        }

        metaStateFromKeyEvent = metaState

        //remove the metastate from any modifier keys that remapped and are pressed down
        parallelTriggerModifierKeyIndices.forEach {
            val triggerIndex = it.first
            val eventIndex = it.second
            val event = triggers[triggerIndex].keys[eventIndex]

            if (parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex]) {
                metaStateFromKeyEvent =
                    metaStateFromKeyEvent.minusFlag(KeyEventUtils.modifierKeycodeToMetaState(event.keyCode))
            }
        }

        val event =
            if (device != null && device.isExternal) {
                Event(keyCode, null, device.descriptor)
            } else {
                Event(
                    keyCode,
                    null,
                    null
                )
            }

        when (action) {
            KeyEvent.ACTION_DOWN -> return onKeyDown(event, device?.id ?: 0, scanCode)
            KeyEvent.ACTION_UP -> return onKeyUp(event, device?.id ?: 0, scanCode)
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

        val constraintSnapshot: ConstraintSnapshot by lazy { detectConstraints.getSnapshot() }

        //consume sequence trigger keys until their timeout has been reached
        for (sequenceTriggerIndex in sequenceTriggers) {
            val timeoutTime = sequenceTriggersTimeoutTimes[sequenceTriggerIndex] ?: -1
            val trigger = triggers[sequenceTriggerIndex]
            val constraintState = triggerConstraints[sequenceTriggerIndex]

            if (constraintState.constraints.isNotEmpty()) {
                if (!constraintSnapshot.isSatisfied(constraintState)) continue
            }

            if (timeoutTime != -1L && currentTime >= timeoutTime) {
                lastMatchedEventIndices[sequenceTriggerIndex] = -1
                sequenceTriggersTimeoutTimes[sequenceTriggerIndex] = -1
            } else {
                //consume the event if the trigger contains this keycode.
                trigger.keys.forEachIndexed { keyIndex, key ->
                    if (key.keyCode == event.keyCode && trigger.keys[keyIndex].consumeKeyEvent) {
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
                    triggers[eventLocation.triggerIndex].keys[eventLocation.keyIndex]
                val triggerIndex = eventLocation.triggerIndex

                triggers[triggerIndex].keys.forEachIndexed { eventIndex, event ->
                    if (event == doublePressEvent
                        && triggers[triggerIndex].keys[eventIndex].consumeKeyEvent
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


        /*
        loop through triggers in a different loop first to increment the last matched index.
        Otherwise the order of the key maps affects the logic.
         */
        triggerLoop@ for (triggerIndex in parallelTriggers) {
            val trigger = triggers[triggerIndex]
            val lastMatchedIndex = lastMatchedEventIndices[triggerIndex]

            val constraintState = triggerConstraints[triggerIndex]

            if (constraintState.constraints.isNotEmpty()) {
                if (!constraintSnapshot.isSatisfied(constraintState)) {
                    continue
                }
            }

            for (actionKey in triggerActions[triggerIndex]) {
                if (canActionBePerformed[actionKey] == null) {
                    val action = actionMap[actionKey] ?: continue

                    val result = performActionsUseCase.getError(action.data)
                    canActionBePerformed.put(actionKey, result)

                    if (result != null) {
                        continue@triggerLoop
                    }
                } else if (canActionBePerformed.get(actionKey, null) is Error) {
                    continue@triggerLoop
                }
            }

            val nextIndex = lastMatchedIndex + 1

            if (trigger.matchingEventAtIndex(
                    event.withShortPress,
                    nextIndex
                )
            ) {
                lastMatchedEventIndices[triggerIndex] = nextIndex
                parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true
            }

            if (trigger.matchingEventAtIndex(
                    event.withLongPress,
                    nextIndex
                )
            ) {
                lastMatchedEventIndices[triggerIndex] = nextIndex
                parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true
            }
        }

        triggerLoop@ for (triggerIndex in parallelTriggers) {
            val trigger = triggers[triggerIndex]
            val lastMatchedIndex = lastMatchedEventIndices[triggerIndex]

            for (overlappingTriggerIndex in sequenceTriggersOverlappingParallelTriggers[triggerIndex]) {
                if (lastMatchedEventIndices[overlappingTriggerIndex] == triggers[overlappingTriggerIndex].keys.lastIndex) {
                    continue@triggerLoop
                }
            }

            for (overlappingTriggerIndex in parallelTriggersOverlappingParallelTriggers[triggerIndex]) {
                if (lastMatchedEventIndices[overlappingTriggerIndex] == triggers[overlappingTriggerIndex].keys.lastIndex) {
                    continue@triggerLoop
                }
            }

            if (lastMatchedIndex == -1) {
                continue@triggerLoop
            }

            //Perform short press action
            if (trigger.matchingEventAtIndex(
                    event.withShortPress,
                    lastMatchedIndex
                )
            ) {

                if (trigger.keys[lastMatchedIndex].consumeKeyEvent) {
                    consumeEvent = true
                }

                if (lastMatchedIndex == trigger.keys.lastIndex) {
                    mappedToParallelTriggerAction = true
                    parallelTriggersAwaitingReleaseAfterBeingTriggered[triggerIndex] = true

                    val actionKeys = triggerActions[triggerIndex]

                    actionKeys.forEach { actionKey ->
                        val action = actionMap[actionKey] ?: return@forEach

                        if (action.data is ActionData.InputKeyEvent) {
                            val actionKeyCode = action.data.keyCode

                            if (isModifierKey(actionKeyCode)) {
                                val actionMetaState = KeyEventUtils.modifierKeycodeToMetaState(actionKeyCode)
                                metaStateFromActions = metaStateFromActions.withFlag(actionMetaState)
                            }
                        }

                        detectedShortPressTriggers.add(triggerIndex)

                        val vibrateDuration = when {
                            trigger.vibrate -> {
                                vibrateDuration(trigger)
                            }

                            forceVibrate.value -> defaultVibrateDuration.value
                            else -> -1L
                        }

                        vibrateDurations.add(vibrateDuration)
                    }
                }
            }

            //Perform long press action
            if (trigger.matchingEventAtIndex(
                    event.withLongPress,
                    lastMatchedIndex
                )
            ) {
                if (trigger.keys[lastMatchedIndex].consumeKeyEvent) {
                    consumeEvent = true
                }

                if (lastMatchedIndex == trigger.keys.lastIndex) {
                    awaitingLongPress = true

                    if (trigger.longPressDoubleVibration
                    ) {
                        useCase.vibrate(vibrateDuration(trigger))
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
                triggers[it.triggerIndex].keys[it.keyIndex].matchesEvent(event.withDoublePress)
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

                    if (triggers[triggerIndex].showToast) {
                        showToast = true
                    }

                    parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
                        calledOnTriggerRelease = false,
                        metaState = metaStateFromKeyEvent.withFlag(metaStateFromActions)
                    )
                }
            }
        }

        if (showToast) {
            useCase.showTriggeredToast()
        }

        if (forceVibrate.value) {
            useCase.vibrate(defaultVibrateDuration.value)
        } else {
            vibrateDurations.maxOrNull()?.let {
                useCase.vibrate(it)
            }
        }

        if (consumeEvent) {
            return true
        }

        sequenceTriggers.forEach { triggerIndex ->
            val trigger = triggers[triggerIndex]
            val constraints = triggerConstraints[triggerIndex]

            if (!constraintSnapshot.isSatisfied(constraints)) return@forEach

            trigger.keys.forEachIndexed { keyIndex, key ->
                val matchingEvent = when {
                    key.matchesEvent(event.withShortPress) -> true
                    key.matchesEvent(event.withLongPress) -> true
                    key.matchesEvent(event.withDoublePress) -> true

                    else -> false
                }

                if (matchingEvent && key.consumeKeyEvent) {
                    return true
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

        var successfulLongPressTrigger = false
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

        val constraintSnapshot by lazy { detectConstraints.getSnapshot() }

        if (detectSequenceDoublePresses) {
            //iterate over each possible double press event to detect
            for (index in doublePressTriggerKeys.indices) {
                val eventLocation = doublePressTriggerKeys[index]
                val doublePressKey =
                    triggers[eventLocation.triggerIndex].keys[eventLocation.keyIndex]
                val triggerIndex = eventLocation.triggerIndex

                val constraintState = triggerConstraints[triggerIndex]

                if (constraintState.constraints.isNotEmpty()) {
                    if (!constraintSnapshot.isSatisfied(constraintState)) continue
                }

                if (lastMatchedEventIndices[triggerIndex] != eventLocation.keyIndex - 1) continue

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
                                doublePressTimeout(triggers[triggerIndex])
                            doublePressTimeoutTimes[index] = currentTime + doublePressTimeout

                            imitateKeyAfterDoublePressTimeout.add(doublePressTimeout)
                            matchedDoublePressEventIndex = index

                            triggers[triggerIndex].keys.forEachIndexed { keyIndex, key ->
                                if (key == doublePressKey
                                    && triggers[triggerIndex].keys[keyIndex].consumeKeyEvent
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

        triggerLoop@ for (triggerIndex in sequenceTriggers) {
            val trigger = triggers[triggerIndex]
            val constraintState = triggerConstraints[triggerIndex]
            val lastMatchedEventIndex = lastMatchedEventIndices[triggerIndex]

            if (constraintState.constraints.isNotEmpty()) {
                if (!constraintSnapshot.isSatisfied(constraintState)) continue
            }

            //the index of the next event to match in the trigger
            val nextIndex = lastMatchedEventIndex + 1

            if ((currentTime - downTime) >= longPressDelay(triggers[triggerIndex])) {
                successfulLongPressTrigger = true
            } else if (detectSequenceLongPresses &&
                longPressSequenceTriggerKeys.any { it.matchesEvent(event.withLongPress) }
            ) {
                imitateDownUpKeyEvent = true
            }

            val encodedEventWithClickType = when {
                successfulLongPressTrigger -> event.withLongPress
                successfulDoublePress -> event.withDoublePress
                else -> event.withShortPress
            }

            for (overlappingTriggerIndex in sequenceTriggersOverlappingSequenceTriggers[triggerIndex]) {
                if (lastMatchedEventIndices[overlappingTriggerIndex] != -1) {
                    continue@triggerLoop
                }
            }

            //if the next event matches the event just pressed
            if (triggers[triggerIndex].matchingEventAtIndex(
                    encodedEventWithClickType,
                    nextIndex
                )
            ) {

                if (triggers[triggerIndex].keys[nextIndex].consumeKeyEvent) {
                    consumeEvent = true
                }

                lastMatchedEventIndices[triggerIndex] = nextIndex

                /*
                If the next index is 0, then the first event in the trigger has been matched, which means the timer
                needs to start for this trigger.
                 */
                if (nextIndex == 0) {
                    val startTime = currentTime
                    val timeout = sequenceTriggerTimeout(triggers[triggerIndex])

                    sequenceTriggersTimeoutTimes[triggerIndex] = startTime + timeout
                }

                /*
                If the last event in a trigger has been matched, then the action needs to be performed and the timer
                reset.
                 */
                if (nextIndex == triggers[triggerIndex].keys.lastIndex) {
                    detectedSequenceTriggerIndexes.add(triggerIndex)

                    if (triggers[triggerIndex].showToast) {
                        showToast = true
                    }

                    triggerActions[triggerIndex].forEachIndexed { index, _ ->
                        if (triggers[triggerIndex].vibrate) {
                            vibrateDurations.add(vibrateDuration(triggers[triggerIndex]))
                        }
                    }

                    lastMatchedEventIndices[triggerIndex] = -1
                    sequenceTriggersTimeoutTimes[triggerIndex] = -1
                }
            }
        }

        /**
         * Whether a trigger that was triggered successfully has just been released.
         */
        var releasedSuccessfulTrigger = false

        for (triggerIndex in parallelTriggers) {

            val trigger = triggers[triggerIndex]

            if (parallelTriggersAwaitingReleaseAfterBeingTriggered[triggerIndex]) {
                releasedSuccessfulTrigger = true
            }

            for (keyIndex in trigger.keys.indices) {
                val keyAwaitingRelease =
                    parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex]

                //long press
                if (keyAwaitingRelease && trigger.matchingEventAtIndex(
                        event.withLongPress,
                        keyIndex
                    )
                ) {
                    if ((currentTime - downTime) >= longPressDelay(triggers[triggerIndex])) {
                        releasedSuccessfulTrigger = true
                        successfulLongPressTrigger = true
                    }
                }
            }
        }

        triggerLoop@ for (triggerIndex in parallelTriggers) {
            val trigger = triggers[triggerIndex]

            val triggeredSuccessfully =
                parallelTriggersAwaitingReleaseAfterBeingTriggered[triggerIndex]

            var lastHeldDownEventIndex = -1

            val isSingleKeyTrigger = triggers[triggerIndex].keys.size == 1

            for (keyIndex in trigger.keys.indices) {
                val keyAwaitingRelease =
                    parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex]

                //short press
                if (keyAwaitingRelease && trigger.matchingEventAtIndex(
                        event.withShortPress,
                        keyIndex
                    )
                ) {
                    if (isSingleKeyTrigger) {
                        shortPressSingleKeyTriggerJustReleased = true
                    }

                    if (!triggeredSuccessfully && !releasedSuccessfulTrigger) {
                        imitateDownUpKeyEvent = true
                    }

                    if (modifierKeyEventActions) {
                        val actionKeys = triggerActions[triggerIndex]
                        actionKeys.forEach { actionKey ->

                            actionMap[actionKey]?.let { action ->
                                if (action.data is ActionData.InputKeyEvent && isModifierKey(action.data.keyCode)) {
                                    val actionMetaState =
                                        KeyEventUtils.modifierKeycodeToMetaState(action.data.keyCode)

                                    metaStateFromActionsToRemove =
                                        metaStateFromActionsToRemove.withFlag(actionMetaState)
                                }
                            }
                        }
                    }

                    parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex] = false

                    if (triggers[triggerIndex].keys[keyIndex].consumeKeyEvent) {
                        consumeEvent = true
                    }
                }

                //long press
                if (keyAwaitingRelease && trigger.matchingEventAtIndex(
                        event.withLongPress,
                        keyIndex
                    )
                ) {
                    parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex] = false

                    parallelTriggerLongPressJobs[triggerIndex]?.cancel()

                    if (triggers[triggerIndex].keys[keyIndex].consumeKeyEvent) {
                        consumeEvent = true
                    }

                    val lastMatchedIndex = lastMatchedEventIndices[triggerIndex]

                    if (isSingleKeyTrigger && successfulLongPressTrigger) {
                        longPressSingleKeyTriggerJustReleased = true
                    }

                    if (!imitateDownUpKeyEvent) {
                        if (isSingleKeyTrigger && !successfulLongPressTrigger && !releasedSuccessfulTrigger) {
                            imitateDownUpKeyEvent = true
                        } else if (lastMatchedIndex > -1
                            && lastMatchedIndex < triggers[triggerIndex].keys.lastIndex
                            && !releasedSuccessfulTrigger
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

            if (parallelTriggerEventsAwaitingRelease[triggerIndex].all { !it }) {
                parallelTriggersAwaitingReleaseAfterBeingTriggered[triggerIndex] = false
            }

            lastMatchedEventIndices[triggerIndex] = lastHeldDownEventIndex
            metaStateFromActions = metaStateFromActions.minusFlag(metaStateFromActionsToRemove)

            //let actions know that the trigger has been released
            if (lastHeldDownEventIndex != triggers[triggerIndex].keys.lastIndex) {
                parallelTriggerActionPerformers[triggerIndex]?.onReleased(metaStateFromKeyEvent + metaStateFromActions)
            }
        }

        //perform actions on failed long press
        if (!successfulLongPressTrigger) {
            val iterator = performActionsOnFailedLongPress.iterator()

            while (iterator.hasNext()) {
                val triggerIndex = iterator.next()
                val trigger = triggers[triggerIndex]

                /*
                The last event in the trigger
                */
                val lastKey = trigger.keys.last()

                if (lastKey.matchesEvent(event.withShortPress)) {
                    detectedParallelTriggerIndexes.add(triggerIndex)

                    if (trigger.showToast) {
                        showToast = true
                    }

                    triggerActions[triggerIndex].forEachIndexed { _, _ ->
                        if (trigger.vibrate) {
                            vibrateDurations.add(vibrateDuration(trigger))
                        }
                    }
                }

                iterator.remove()
            }
        }

        detectedSequenceTriggerIndexes.forEach { triggerIndex ->
            sequenceTriggerActionPerformers[triggerIndex]?.onTriggered(
                metaState = metaStateFromActions.withFlag(
                    metaStateFromKeyEvent
                )
            )
        }

        detectedParallelTriggerIndexes.forEach { triggerIndex ->
            parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
                calledOnTriggerRelease = true,
                metaState = metaStateFromActions.withFlag(metaStateFromKeyEvent)
            )
        }

        if (forceVibrate.value) {
            useCase.vibrate(defaultVibrateDuration.value)
        } else {
            vibrateDurations.maxOrNull()?.let {
                useCase.vibrate(it)
            }
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
                        inputEventType = InputEventType.DOWN_UP,
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
        lastMatchedEventIndices = IntArray(triggers.size) { -1 }

        doublePressEventStates = IntArray(doublePressTriggerKeys.size) { NOT_PRESSED }
        doublePressTimeoutTimes = LongArray(doublePressTriggerKeys.size) { -1L }

        sequenceTriggersTimeoutTimes = mutableMapOf()

        parallelTriggerEventsAwaitingRelease = Array(triggers.size) {
            BooleanArray(triggers[it].keys.size) { false }
        }

        parallelTriggersAwaitingReleaseAfterBeingTriggered = BooleanArray(triggers.size)

        performActionsOnFailedDoublePress.clear()
        performActionsOnFailedLongPress.clear()

        metaStateFromActions = 0
        metaStateFromKeyEvent = 0
        keyCodesToImitateUpAction = mutableSetOf()

        parallelTriggerLongPressJobs.valueIterator().forEach {
            it.cancel()
        }

        parallelTriggerLongPressJobs.clear()

        parallelTriggerActionPerformers.values.forEach { it.reset() }
        sequenceTriggerActionPerformers.values.forEach { it.reset() }
    }

    /**
     * @return whether any actions were performed.
     */
    private fun performActionsOnFailedDoublePress(event: Event): Boolean {
        var showToast = false
        val detectedTriggerIndexes = mutableListOf<Int>()
        val vibrateDurations = mutableListOf<Long>()

        performActionsOnFailedDoublePress.forEach { triggerIndex ->
            if (triggers[triggerIndex].keys.last().matchesEvent(event.withShortPress)) {
                detectedTriggerIndexes.add(triggerIndex)

                if (triggers[triggerIndex].showToast) {
                    showToast = true
                }

                if (triggers[triggerIndex].vibrate) {
                    vibrateDurations.add(vibrateDuration(triggers[triggerIndex]))
                }
            }
        }

        performActionsOnFailedDoublePress.clear()

        if (showToast) {
            useCase.showTriggeredToast()
        }

        if (forceVibrate.value) {
            useCase.vibrate(defaultVibrateDuration.value)
        } else {
            vibrateDurations.maxOrNull()?.let {
                useCase.vibrate(it)
            }
        }

        detectedTriggerIndexes.forEach { triggerIndex ->
            parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
                calledOnTriggerRelease = true,
                metaState = metaStateFromActions.withFlag(metaStateFromKeyEvent)
            )
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

    /**
     * For parallel triggers only.
     */
    private fun performActionsAfterLongPressDelay(triggerIndex: Int) = coroutineScope.launch {
        delay(longPressDelay(triggers[triggerIndex]))

        parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
            calledOnTriggerRelease = false,
            metaState = metaStateFromActions.withFlag(metaStateFromKeyEvent)
        )

        if (triggers[triggerIndex].vibrate || forceVibrate.value
            || triggers[triggerIndex].longPressDoubleVibration
        ) {
            useCase.vibrate(vibrateDuration(triggers[triggerIndex]))
        }

        if (triggers[triggerIndex].showToast) {
            useCase.showTriggeredToast()
        }
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

    private data class TriggerKeyLocation(val triggerIndex: Int, val keyIndex: Int)
}