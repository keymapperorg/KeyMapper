package io.github.sds100.keymapper.base.detection

import android.view.KeyEvent
import androidx.collection.SparseArrayCompat
import androidx.collection.keyIterator
import androidx.collection.valueIterator
import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.PerformActionTriggerDevice
import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.constraints.ConstraintSnapshot
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.constraints.isSatisfied
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.base.trigger.AssistantTriggerType
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.base.trigger.FloatingButtonKey
import io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerDevice
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerKey
import io.github.sds100.keymapper.base.trigger.TriggerMode
import io.github.sds100.keymapper.base.trigger.detectWithScancode
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.minusFlag
import io.github.sds100.keymapper.common.utils.withFlag
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.system.inputevents.KMEvdevEvent
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.inputevents.KMInputEvent
import io.github.sds100.keymapper.system.inputevents.KMKeyEvent
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KeyMapAlgorithm(
    private val coroutineScope: CoroutineScope,
    private val useCase: DetectKeyMapsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectConstraints: DetectConstraintsUseCase,
) {
    companion object {

        // the states for keys awaiting a double press
        private const val NOT_PRESSED = -1
        private const val SINGLE_PRESSED = 0
        private const val DOUBLE_PRESSED = 1

        /**
         * @return whether the actions assigned to this trigger will be performed on the down event of the final key
         * rather than the up event.
         */
        fun performActionOnDown(trigger: Trigger): Boolean = (
            trigger.keys.size <= 1 &&
                trigger.keys.getOrNull(0)?.clickType != ClickType.DOUBLE_PRESS &&
                trigger.mode == TriggerMode.Undefined
            ) ||

            trigger.mode is TriggerMode.Parallel
    }

    private var detectKeyMaps: Boolean = false
    private var detectInternalEvents: Boolean = false
    private var detectExternalEvents: Boolean = false
    private var detectSequenceLongPresses: Boolean = false
    private var detectSequenceDoublePresses: Boolean = false

    /**
     * All sequence events that have the long press click type.
     */
    private var longPressSequenceTriggerKeys: Array<KeyCodeTriggerKey> = arrayOf()

    /**
     * All double press keys and the index of their corresponding trigger. first is the event and second is
     * the trigger index.
     */
    private var doublePressTriggerKeys: Array<TriggerKeyLocation> = arrayOf()

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

    var actionMap: SparseArrayCompat<Action> = SparseArrayCompat()
        private set
    var triggers: Array<Trigger> = emptyArray()
        private set

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
    private var sequenceTriggersOverlappingSequenceTriggers: Array<IntArray> = arrayOf()

    private var sequenceTriggersOverlappingParallelTriggers: Array<IntArray> = arrayOf()

    /**
     * An array of the index of the last matched event in each trigger.
     */
    private var lastMatchedEventIndices: IntArray = intArrayOf()

    /**
     * An array of the constraints for every trigger
     */
    private var triggerConstraints: Array<Array<ConstraintState>> = arrayOf()

    /**
     * The events to detect for each parallel trigger.
     */
    private var parallelTriggers: IntArray = intArrayOf()

    /**
     * The actions to perform when each trigger is detected. The order matches with
     * [triggers].
     */
    var triggerActions: Array<IntArray> = arrayOf()
        private set

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
    private var keyCodesToImitateUpAction: MutableSet<Int> = mutableSetOf()
    private var metaStateFromActions: Int = 0
    private var metaStateFromKeyEvent: Int = 0

    private val eventDownTimeMap: MutableMap<AlgoEvent, Long> = mutableMapOf()

    /**
     * This solves issue #1386. This stores the jobs that will wait until the sequence trigger
     * times out and check whether the overlapping sequence trigger was indeed triggered.
     */
    private val performActionsAfterSequenceTriggerTimeout: MutableMap<Int, Job> = mutableMapOf()

    /**
     * The indexes of parallel triggers that didn't have their actions performed because there is a matching trigger but
     * for a long-press. These actions should only be performed if the long-press fails, otherwise when the user
     * holds down the trigger keys for the long-press trigger, actions from both triggers will be performed.
     */
    private val performActionsOnFailedLongPress: MutableSet<Int> = mutableSetOf()

    /**
     * The indexes of parallel triggers that didn't have their actions performed because there is a matching trigger but
     * for a double-press. These actions should only be performed if the double-press fails, otherwise each time the user
     * presses the keys for the double press, actions from both triggers will be performed.
     */
    private val performActionsOnFailedDoublePress: MutableSet<Int> = mutableSetOf()

    /**
     * Maps jobs to perform an action after a long press to their corresponding parallel trigger index
     */
    private val parallelTriggerLongPressJobs: SparseArrayCompat<Job> = SparseArrayCompat()

    /**
     * Keys that are detected through an input method will potentially send multiple DOWN key events
     * with incremented repeatCounts, such as DPAD buttons. These repeated DOWN key events must
     * all be consumed and ignored because the UP key event is only sent once at the end. The action
     * must not be executed for each repeat. The user may potentially have many hundreds
     * of trigger keys so to reduce latency this set caches which keys
     * will be affected by this behavior.
     *
     * NOTE: This only contains the trigger keys that are flagged to consume the key event.
     */
    private var triggerKeysThatSendRepeatedKeyEvents: Set<KeyEventTriggerKey> = emptySet()

    private var parallelTriggerActionPerformers: Map<Int, ParallelTriggerActionPerformer> =
        emptyMap()
    private var sequenceTriggerActionPerformers: Map<Int, SequenceTriggerActionPerformer> =
        emptyMap()

    private val currentTime: Long
        get() = useCase.currentTime

    private val defaultVibrateDuration: StateFlow<Long> =
        useCase.defaultVibrateDuration.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.VIBRATION_DURATION.toLong(),
        )

    private val defaultSequenceTriggerTimeout: StateFlow<Long> =
        useCase.defaultSequenceTriggerTimeout.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT.toLong(),
        )

    private val defaultLongPressDelay: StateFlow<Long> =
        useCase.defaultLongPressDelay.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.LONG_PRESS_DELAY.toLong(),
        )

    private val defaultDoublePressDelay: StateFlow<Long> =
        useCase.defaultDoublePressDelay.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.DOUBLE_PRESS_DELAY.toLong(),
        )

    private val forceVibrate: StateFlow<Boolean> =
        useCase.forceVibrate.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.FORCE_VIBRATE,
        )

    private val dpadMotionEventTracker: DpadMotionEventTracker = DpadMotionEventTracker()

    fun loadKeyMaps(value: List<DetectKeyMapModel>) {
        actionMap.clear()

        // If there are no keymaps with actions then keys don't need to be detected.
        if (!value.any { it.keyMap.actionList.isNotEmpty() }) {
            detectKeyMaps = false
            return
        }

        if (value.all { !it.keyMap.isEnabled }) {
            detectKeyMaps = false
            return
        }

        if (value.isEmpty()) {
            detectKeyMaps = false
        } else {
            detectKeyMaps = true

            val longPressSequenceTriggerKeys = mutableListOf<KeyCodeTriggerKey>()

            val doublePressKeys = mutableListOf<TriggerKeyLocation>()

            setActionMapAndOptions(value.flatMap { it.keyMap.actionList }.toSet())

            val triggers = mutableListOf<Trigger>()
            val sequenceTriggers = mutableListOf<Int>()
            val parallelTriggers = mutableListOf<Int>()

            val triggerActions = mutableListOf<IntArray>()
            val triggerConstraints = mutableListOf<Array<ConstraintState>>()
            val triggerPerformActionDevices = mutableListOf<PerformActionTriggerDevice>()

            val sequenceTriggerActionPerformers =
                mutableMapOf<Int, SequenceTriggerActionPerformer>()
            val parallelTriggerActionPerformers =
                mutableMapOf<Int, ParallelTriggerActionPerformer>()
            val parallelTriggerModifierKeyIndices = mutableListOf<Pair<Int, Int>>()
            val triggerKeysThatSendRepeatedKeyEvents = mutableSetOf<KeyEventTriggerKey>()

            // Only process key maps that can be triggered
            val validKeyMaps = value.filter {
                it.keyMap.actionList.isNotEmpty() && it.keyMap.isEnabled
            }

            for ((triggerIndex, model) in validKeyMaps.withIndex()) {
                val keyMap = model.keyMap
                // TRIGGER STUFF
                for ((keyIndex, key) in keyMap.trigger.keys.withIndex()) {
                    if (key is KeyEventTriggerKey && key.requiresIme && key.consumeEvent) {
                        triggerKeysThatSendRepeatedKeyEvents.add(key)
                    }

                    if (keyMap.trigger.mode == TriggerMode.Sequence &&
                        key.clickType == ClickType.LONG_PRESS &&
                        key is KeyCodeTriggerKey
                    ) {
                        if (keyMap.trigger.keys.size > 1) {
                            longPressSequenceTriggerKeys.add(key)
                        }
                    }

                    if (keyMap.trigger.mode !is TriggerMode.Parallel &&
                        key.clickType == ClickType.DOUBLE_PRESS
                    ) {
                        doublePressKeys.add(TriggerKeyLocation(triggerIndex, keyIndex))
                    }

                    when (key) {
                        is KeyEventTriggerKey -> when (key.device) {
                            KeyEventTriggerDevice.Internal -> {
                                detectInternalEvents = true
                            }

                            KeyEventTriggerDevice.Any -> {
                                detectInternalEvents = true
                                detectExternalEvents = true
                            }

                            is KeyEventTriggerDevice.External -> {
                                detectExternalEvents = true
                            }
                        }

                        is EvdevTriggerKey -> {
                            detectInternalEvents = true
                            detectExternalEvents = true
                        }

                        else -> {}
                    }
                }

                val encodedActionList = encodeActionList(keyMap.actionList)

                if (keyMap.actionList.any {
                        it.data is ActionData.InputKeyEvent &&
                            isModifierKey(
                                it.data.keyCode,
                            )
                    }
                ) {
                    modifierKeyEventActions = true
                }

                if (keyMap.actionList.any {
                        it.data is ActionData.InputKeyEvent &&
                            !isModifierKey(
                                it.data.keyCode,
                            )
                    }
                ) {
                    notModifierKeyEventActions = true
                }

                triggers.add(keyMap.trigger)
                triggerActions.add(encodedActionList)

                val constraintStates =
                    model.groupConstraintStates.plus(keyMap.constraintState).toTypedArray()
                triggerConstraints.add(constraintStates)

                if (performActionOnDown(keyMap.trigger)) {
                    parallelTriggers.add(triggerIndex)
                    parallelTriggerActionPerformers[triggerIndex] =
                        ParallelTriggerActionPerformer(
                            coroutineScope,
                            performActionsUseCase,
                            keyMap.actionList,
                        )
                } else {
                    sequenceTriggers.add(triggerIndex)
                    sequenceTriggerActionPerformers[triggerIndex] =
                        SequenceTriggerActionPerformer(
                            coroutineScope,
                            performActionsUseCase,
                            keyMap.actionList,
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
                                // the other trigger doesn't overlap after the first element
                                if (otherIndex == 0) continue@otherTriggerLoop

                                // make sure the overlap retains the order of the trigger
                                if (lastMatchedIndex != null &&
                                    lastMatchedIndex != otherIndex - 1
                                ) {
                                    continue@otherTriggerLoop
                                }

                                if (keyIndex == trigger.keys.lastIndex) {
                                    sequenceTriggersOverlappingSequenceTriggers[triggerIndex].add(
                                        otherTriggerIndex,
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
                val parallelTrigger = triggers[triggerIndex]

                otherTriggerLoop@ for (otherTriggerIndex in sequenceTriggers) {
                    val otherTrigger = triggers[otherTriggerIndex]

                    // Don't compare a trigger to itself
                    if (triggerIndex == otherTriggerIndex) {
                        continue@otherTriggerLoop
                    }

                    for ((keyIndex, key) in parallelTrigger.keys.withIndex()) {
                        var lastMatchedIndex: Int? = null

                        for ((otherKeyIndex, otherKey) in otherTrigger.keys.withIndex()) {
                            if (key.matchesWithOtherKey(otherKey)) {
                                // make sure the overlap retains the order of the trigger
                                if (lastMatchedIndex != null &&
                                    lastMatchedIndex != otherKeyIndex - 1
                                ) {
                                    continue@otherTriggerLoop
                                }

                                if (keyIndex == parallelTrigger.keys.lastIndex) {
                                    sequenceTriggersOverlappingParallelTriggers[triggerIndex].add(
                                        otherTriggerIndex,
                                    )
                                }

                                lastMatchedIndex = otherKeyIndex
                            }

                            // if there were no matching keys in the other trigger then skip this trigger
                            if (lastMatchedIndex == null &&
                                otherKeyIndex == otherTrigger.keys.lastIndex
                            ) {
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

                    // Don't compare a trigger to itself
                    if (triggerIndex == otherTriggerIndex) {
                        continue@otherTriggerLoop
                    }

                    // only check for overlapping if the other trigger has more keys
                    if (otherTrigger.keys.size <= trigger.keys.size) {
                        continue@otherTriggerLoop
                    }

                    for ((keyIndex, key) in trigger.keys.withIndex()) {
                        var lastMatchedIndex: Int? = null

                        for ((otherKeyIndex, otherKey) in otherTrigger.keys.withIndex()) {
                            if (otherKey.matchesWithOtherKey(key)) {
                                // make sure the overlap retains the order of the trigger
                                if (lastMatchedIndex != null &&
                                    lastMatchedIndex != otherKeyIndex - 1
                                ) {
                                    continue@otherTriggerLoop
                                }

                                if (keyIndex == trigger.keys.lastIndex) {
                                    parallelTriggersOverlappingParallelTriggers[triggerIndex].add(
                                        otherTriggerIndex,
                                    )
                                }

                                lastMatchedIndex = otherKeyIndex
                            }

                            // if there were no matching keys in the other trigger then skip this trigger
                            if (lastMatchedIndex == null &&
                                otherKeyIndex == otherTrigger.keys.lastIndex
                            ) {
                                continue@otherTriggerLoop
                            }
                        }
                    }
                }
            }

            for (triggerIndex in parallelTriggers) {
                val trigger = triggers[triggerIndex]

                for ((keyIndex, key) in trigger.keys.withIndex()) {
                    if (key is KeyCodeTriggerKey && isModifierKey(key.keyCode)) {
                        parallelTriggerModifierKeyIndices.add(triggerIndex to keyIndex)
                    }
                }
            }

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

            this.triggerKeysThatSendRepeatedKeyEvents = triggerKeysThatSendRepeatedKeyEvents

            reset()
        }
    }

    fun onMotionEvent(event: KMGamePadEvent): Boolean {
        if (!detectKeyMaps) return false

        // See https://developer.android.com/develop/ui/views/touch-and-input/game-controllers/controller-input#dpad
        // Some controllers send motion events as well as key events when DPAD buttons
        // are pressed, while others just send key events.
        // The motion events must be consumed but this means the following key events are also
        // consumed so one must rely on converting these motion events oneself.

        val convertedKeyEvents = dpadMotionEventTracker.convertMotionEvent(event)

        var consume = false

        for (keyEvent in convertedKeyEvents) {
            if (onKeyEventPostFilter(keyEvent)) {
                consume = true
            }
        }

        return consume
    }

    /**
     * @return whether to consume the [KeyEvent].
     */
    fun onInputEvent(event: KMInputEvent): Boolean {
        if (!detectKeyMaps) return false

        if (event is KMKeyEvent) {
            if (dpadMotionEventTracker.onKeyEvent(event)) {
                return true
            }

            val device = event.device

            if ((device.isExternal && !detectExternalEvents) ||
                (!device.isExternal && !detectInternalEvents)
            ) {
                return false
            }
        }

        return onKeyEventPostFilter(event)
    }

    private fun onKeyEventPostFilter(inputEvent: KMInputEvent): Boolean {
        if (inputEvent is KMKeyEvent) {
            metaStateFromKeyEvent = inputEvent.metaState
        }

        // remove the metastate from any modifier keys that remapped and are pressed down
        for ((triggerIndex, eventIndex) in parallelTriggerModifierKeyIndices) {
            val key = triggers[triggerIndex].keys[eventIndex]

            if (key !is KeyCodeTriggerKey) {
                continue
            }

            if (parallelTriggerEventsAwaitingRelease[triggerIndex][eventIndex]) {
                metaStateFromKeyEvent =
                    metaStateFromKeyEvent.minusFlag(
                        KeyEventUtils.modifierKeycodeToMetaState(key.keyCode),
                    )
            }
        }

        when (inputEvent) {
            is KMEvdevEvent -> {
                val event = EvdevEventAlgo(
                    keyCode = inputEvent.androidCode,
                    clickType = null,
                    deviceId = inputEvent.deviceId,
                    device = inputEvent.deviceInfo,
                    scanCode = inputEvent.code,
                )

                if (inputEvent.isDownEvent) {
                    return onKeyDown(event)
                } else if (inputEvent.isUpEvent) {
                    return onKeyUp(event)
                }
            }

            is KMKeyEvent -> {
                val device = inputEvent.device

                val event = KeyEventAlgo(
                    keyCode = inputEvent.keyCode,
                    clickType = null,
                    descriptor = device.descriptor,
                    deviceId = device.id,
                    scanCode = inputEvent.scanCode,
                    repeatCount = inputEvent.repeatCount,
                    source = inputEvent.source,
                    isExternal = device.isExternal,
                )

                when (inputEvent.action) {
                    KeyEvent.ACTION_DOWN -> return onKeyDown(event)
                    KeyEvent.ACTION_UP -> return onKeyUp(event)
                }
            }

            is KMGamePadEvent -> {}
        }

        return false
    }

    /**
     * @return whether to consume the event.
     */
    private fun onKeyDown(event: AlgoEvent): Boolean {
        // Must come before saving the event down time because
        // there is no corresponding up key event for key events with a repeat count > 0
        if (event is KeyEventAlgo && event.repeatCount > 0) {
            val matchingTriggerKey = triggerKeysThatSendRepeatedKeyEvents.any {
                it.matchesEvent(event.withShortPress) ||
                    it.matchesEvent(event.withLongPress) ||
                    it.matchesEvent(event.withDoublePress)
            }

            if (matchingTriggerKey) {
                return true
            }
        }

        eventDownTimeMap[event] = currentTime

        var consumeEvent = false
        val isModifierKeyCode = event is KeyEventAlgo && isModifierKey(event.keyCode)
        var mappedToParallelTriggerAction = false

        val constraintSnapshot: ConstraintSnapshot by lazy { detectConstraints.getSnapshot() }

        /**
         * Store which triggers are currently satisfied by the constraints.
         * This is used to check later on whether to wait for a double press to complete
         * before executing a short press. See issue #1271.
         */
        val triggersSatisfiedByConstraints = mutableSetOf<Int>()

        for (triggerIndex in parallelTriggers.plus(sequenceTriggers)) {
            val constraintStates = triggerConstraints[triggerIndex]

            if (constraintSnapshot.isSatisfied(*constraintStates)) {
                triggersSatisfiedByConstraints.add(triggerIndex)
            }
        }

        // consume sequence trigger keys until their timeout has been reached
        for (triggerIndex in sequenceTriggers) {
            val timeoutTime = sequenceTriggersTimeoutTimes[triggerIndex] ?: -1

            if (!triggersSatisfiedByConstraints.contains(triggerIndex)) {
                continue
            }

            if (timeoutTime != -1L && currentTime >= timeoutTime) {
                lastMatchedEventIndices[triggerIndex] = -1
                sequenceTriggersTimeoutTimes[triggerIndex] = -1
            } else {
                val triggerKeys = triggers[triggerIndex].keys

                // consume the event if the trigger contains this keycode.
                for (key in triggerKeys) {
                    when {
                        key is AssistantTriggerKey && event is AssistantEvent ->
                            if (key.consumeEvent) {
                                consumeEvent = true
                            }

                        key is FingerprintTriggerKey && event is FingerprintGestureEvent ->
                            if (key.consumeEvent) {
                                consumeEvent = true
                            }

                        key is FloatingButtonKey && event is FloatingButtonEvent ->
                            if (key.consumeEvent) {
                                consumeEvent = true
                            }

                        key is KeyCodeTriggerKey && event is KeyEventAlgo ->
                            if (key.keyCode == event.keyCode && key.consumeEvent) {
                                consumeEvent = true
                            }
                    }
                }
            }
        }

        for ((doublePressEventIndex, timeoutTime) in doublePressTimeoutTimes.withIndex()) {
            if (currentTime >= timeoutTime) {
                doublePressTimeoutTimes[doublePressEventIndex] = -1
                doublePressEventStates[doublePressEventIndex] = NOT_PRESSED
            } else {
                val eventLocation = doublePressTriggerKeys[doublePressEventIndex]
                val triggerIndex = eventLocation.triggerIndex

                // Ignore this double press trigger if the constraint isn't satisfied.
                if (!triggersSatisfiedByConstraints.contains(triggerIndex)) {
                    continue
                }

                val doublePressEvent =
                    triggers[eventLocation.triggerIndex].keys[eventLocation.keyIndex]

                for ((eventIndex, event) in triggers[triggerIndex].keys.withIndex()) {
                    if (event == doublePressEvent &&
                        triggers[triggerIndex].keys[eventIndex].consumeEvent
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

        /*
        loop through triggers in a different loop first to increment the last matched index.
        Otherwise the order of the key maps affects the logic.
         */
        triggerLoop@ for (triggerIndex in parallelTriggers) {
            if (!triggersSatisfiedByConstraints.contains(triggerIndex)) {
                continue
            }

            val trigger = triggers[triggerIndex]

            val lastMatchedIndex = lastMatchedEventIndices[triggerIndex]

            val errorSnapshot = performActionsUseCase.getErrorSnapshot()

            val actionList = triggerActions[triggerIndex]
                .map { actionKey -> actionMap[actionKey]?.data }
                .filterNotNull()

            val actionErrors = errorSnapshot.getErrors(actionList)

            if (actionErrors.values.any { it != null }) {
                continue@triggerLoop
            }

            val nextIndex = lastMatchedIndex + 1

            if (trigger.matchingEventAtIndex(event.withShortPress, nextIndex)) {
                lastMatchedEventIndices[triggerIndex] = nextIndex
                parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true
            }

            if (trigger.matchingEventAtIndex(event.withLongPress, nextIndex)) {
                lastMatchedEventIndices[triggerIndex] = nextIndex
                parallelTriggerEventsAwaitingRelease[triggerIndex][nextIndex] = true
            }
        }

        triggerLoop@ for (triggerIndex in parallelTriggers) {
            if (!triggersSatisfiedByConstraints.contains(triggerIndex)) {
                continue
            }

            val trigger = triggers[triggerIndex]
            val lastMatchedIndex = lastMatchedEventIndices[triggerIndex]

            for (
            overlappingTriggerIndex in
            sequenceTriggersOverlappingParallelTriggers[triggerIndex]
            ) {
                if (lastMatchedEventIndices[overlappingTriggerIndex] ==
                    triggers[overlappingTriggerIndex].keys.lastIndex
                ) {
                    continue@triggerLoop
                }
            }

            for (
            overlappingTriggerIndex in
            parallelTriggersOverlappingParallelTriggers[triggerIndex]
            ) {
                if (lastMatchedEventIndices[overlappingTriggerIndex] ==
                    triggers[overlappingTriggerIndex].keys.lastIndex
                ) {
                    continue@triggerLoop
                }
            }

            if (lastMatchedIndex == -1) {
                continue@triggerLoop
            }

            // Perform short press action
            if (trigger.matchingEventAtIndex(event.withShortPress, lastMatchedIndex)) {
                if (trigger.keys[lastMatchedIndex].consumeEvent) {
                    consumeEvent = true
                }

                if (lastMatchedIndex == trigger.keys.lastIndex) {
                    mappedToParallelTriggerAction = true
                    parallelTriggersAwaitingReleaseAfterBeingTriggered[triggerIndex] = true

                    // See issue #1386.
                    val overlappingSequenceTrigger =
                        sequenceTriggersOverlappingParallelTriggers[triggerIndex]
                            // Only consider the sequence triggers where this
                            // short press trigger has been already pressed
                            // or will be pressed next.
                            .filter {
                                for (i in 0..(lastMatchedEventIndices[it] + 1)) {
                                    val matchingEvent = triggers[it].matchingEventAtIndex(
                                        event.withShortPress,
                                        i,
                                    )

                                    if (matchingEvent) {
                                        return@filter true
                                    }
                                }

                                return@filter false
                            }
                            .maxByOrNull { sequenceTriggerTimeout(triggers[it]) }

                    if (overlappingSequenceTrigger == null) {
                        val actionKeys = triggerActions[triggerIndex]

                        for (actionKey in actionKeys) {
                            val action = actionMap[actionKey] ?: continue

                            // If the key event is being injected with the system bridge
                            // then it will be passed back around through the accessibility
                            // service and processed again.
                            if (action.data is ActionData.InputKeyEvent &&
                                !useCase.injectKeyEventsWithSystemBridge.value
                            ) {
                                val actionKeyCode = action.data.keyCode

                                if (isModifierKey(actionKeyCode)) {
                                    val actionMetaState =
                                        KeyEventUtils.modifierKeycodeToMetaState(actionKeyCode)
                                    metaStateFromActions =
                                        metaStateFromActions.withFlag(actionMetaState)
                                }
                            }

                            detectedShortPressTriggers.add(triggerIndex)
                        }
                    } else {
                        performActionsAfterSequenceTriggerTimeout[triggerIndex]?.cancel()

                        performActionsAfterSequenceTriggerTimeout[triggerIndex] =
                            performActionsAfterSequenceTriggerTimeout(
                                event,
                                triggerIndex,
                                overlappingSequenceTrigger,
                            )
                    }
                }
            }

            // Perform long press action
            if (trigger.matchingEventAtIndex(event.withLongPress, lastMatchedIndex)) {
                if (trigger.keys[lastMatchedIndex].consumeEvent) {
                    consumeEvent = true
                }

                if (lastMatchedIndex == trigger.keys.lastIndex) {
                    awaitingLongPress = true

                    if (trigger.vibrate && trigger.longPressDoubleVibration) {
                        vibrateDurations.add(vibrateDuration(trigger))
                    }

                    val oldJob = parallelTriggerLongPressJobs[triggerIndex]
                    oldJob?.cancel()
                    parallelTriggerLongPressJobs.put(
                        triggerIndex,
                        performActionsAfterLongPressDelay(event, triggerIndex),
                    )
                }
            }
        }

        if (modifierKeyEventActions &&
            !isModifierKeyCode &&
            metaStateFromActions != 0 &&
            !mappedToParallelTriggerAction &&
            event is KeyEventAlgo
        ) {
            consumeEvent = true
            keyCodesToImitateUpAction.add(event.keyCode)

            useCase.imitateKeyEvent(
                keyCode = event.keyCode,
                metaState = metaStateFromKeyEvent.withFlag(metaStateFromActions),
                deviceId = event.deviceId,
                action = KeyEvent.ACTION_DOWN,
                scanCode = event.scanCode,
                source = event.source,
            )

            coroutineScope.launch {
                repeatImitatingKey(
                    keyCode = event.keyCode,
                    deviceId = event.deviceId,
                    scanCode = event.scanCode,
                    source = event.source,
                )
            }
        }

        if (detectedShortPressTriggers.isNotEmpty()) {
            val matchingDoublePressEvent = doublePressTriggerKeys.any { keyLocation ->
                // See issue #1271. Only consider the double press triggers that overlap
                // if the constraints allow it.

                if (!triggersSatisfiedByConstraints.contains(keyLocation.triggerIndex)) {
                    return@any false
                }

                val key = triggers[keyLocation.triggerIndex].keys[keyLocation.keyIndex]
                key.matchesEvent(event.withDoublePress)
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

                else -> {
                    for (triggerIndex in detectedShortPressTriggers) {
                        val trigger = triggers[triggerIndex]

                        parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
                            device = event.performActionDevice(),
                            calledOnTriggerRelease = false,
                            metaState = metaStateFromKeyEvent.withFlag(metaStateFromActions),
                        )

                        if (trigger.showToast) {
                            showToast = true
                        }

                        val vibrateDuration = when {
                            trigger.vibrate -> vibrateDuration(trigger)
                            forceVibrate.value -> defaultVibrateDuration.value
                            else -> -1L
                        }

                        vibrateDurations.add(vibrateDuration)
                    }
                }
            }
        }

        if (showToast) {
            useCase.showTriggeredToast()
        }

        if (vibrateDurations.isNotEmpty()) {
            if (forceVibrate.value) {
                useCase.vibrate(defaultVibrateDuration.value)
            } else {
                vibrateDurations.maxOrNull()?.let {
                    useCase.vibrate(it)
                }
            }
        }

        if (consumeEvent) {
            return true
        }

        // If don't consume the event then check if there is a sequence trigger that
        // uses this event.
        for (triggerIndex in sequenceTriggers) {
            if (!triggersSatisfiedByConstraints.contains(triggerIndex)) {
                continue
            }

            val trigger = triggers[triggerIndex]

            for (key in trigger.keys) {
                val matchingEvent = when {
                    key.matchesEvent(event.withShortPress) -> true
                    key.matchesEvent(event.withLongPress) -> true
                    key.matchesEvent(event.withDoublePress) -> true
                    else -> false
                }

                if (matchingEvent && key.consumeEvent) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * @return whether to consume the event.
     */
    private fun onKeyUp(event: AlgoEvent): Boolean {
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

        if (event is KeyEventAlgo) {
            if (keyCodesToImitateUpAction.contains(event.keyCode)) {
                consumeEvent = true
                imitateUpKeyEvent = true
                keyCodesToImitateUpAction.remove(event.keyCode)
            }
        }

        val constraintSnapshot by lazy { detectConstraints.getSnapshot() }

        if (detectSequenceDoublePresses) {
            // iterate over each possible double press event to detect
            for (index in doublePressTriggerKeys.indices) {
                val eventLocation = doublePressTriggerKeys[index]
                val doublePressKey =
                    triggers[eventLocation.triggerIndex].keys[eventLocation.keyIndex]
                val triggerIndex = eventLocation.triggerIndex

                val constraintState = triggerConstraints[triggerIndex]

                if (!constraintSnapshot.isSatisfied(*constraintState)) continue

                if (lastMatchedEventIndices[triggerIndex] != eventLocation.keyIndex - 1) continue

                if (doublePressKey.matchesEvent(event.withDoublePress)) {
                    mappedToDoublePress = true
                    // increment the double press event state.
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
                                if (key == doublePressKey &&
                                    triggers[triggerIndex].keys[keyIndex].consumeEvent
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

            if (!constraintSnapshot.isSatisfied(*constraintState)) continue

            // the index of the next event to match in the trigger
            val nextIndex = lastMatchedEventIndex + 1

            if ((currentTime - downTime) >= longPressDelay(trigger)) {
                successfulLongPressTrigger = true
            } else if (detectSequenceLongPresses &&
                longPressSequenceTriggerKeys.any { key ->
                    when (key) {
                        is EvdevTriggerKey -> key.matchesEvent(event.withLongPress)
                        is KeyEventTriggerKey -> key.matchesEvent(event.withLongPress)
                    }
                }
            ) {
                imitateDownUpKeyEvent = true
            }

            val encodedEventWithClickType = when {
                successfulLongPressTrigger -> event.withLongPress
                successfulDoublePress -> event.withDoublePress
                else -> event.withShortPress
            }

            for (
            overlappingTriggerIndex in
            sequenceTriggersOverlappingSequenceTriggers[triggerIndex]
            ) {
                if (lastMatchedEventIndices[overlappingTriggerIndex] != -1) {
                    continue@triggerLoop
                }
            }

            // if the next event matches the event just pressed
            if (trigger.matchingEventAtIndex(encodedEventWithClickType, nextIndex)) {
                if (trigger.keys[nextIndex].consumeEvent) {
                    consumeEvent = true
                }

                lastMatchedEventIndices[triggerIndex] = nextIndex

                /*
                If the next index is 0, then the first event in the trigger has been matched, which means the timer
                needs to start for this trigger.
                 */
                if (nextIndex == 0) {
                    val startTime = currentTime
                    val timeout = sequenceTriggerTimeout(trigger)

                    sequenceTriggersTimeoutTimes[triggerIndex] = startTime + timeout
                }

                /*
                If the last event in a trigger has been matched, then the action needs to be performed and the timer
                reset.
                 */
                if (nextIndex == trigger.keys.lastIndex) {
                    detectedSequenceTriggerIndexes.add(triggerIndex)

                    if (trigger.showToast) {
                        showToast = true
                    }

                    triggerActions[triggerIndex].forEach { _ ->
                        if (trigger.vibrate) {
                            vibrateDurations.add(vibrateDuration(trigger))
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

                // long press
                if (keyAwaitingRelease &&
                    trigger.matchingEventAtIndex(
                        event.withLongPress,
                        keyIndex,
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

                // short press
                if (keyAwaitingRelease &&
                    trigger.matchingEventAtIndex(event.withShortPress, keyIndex)
                ) {
                    if (isSingleKeyTrigger) {
                        shortPressSingleKeyTriggerJustReleased = true
                    }

                    if (!triggeredSuccessfully && !releasedSuccessfulTrigger) {
                        imitateDownUpKeyEvent = true
                    }

                    if (modifierKeyEventActions) {
                        val actionKeys = triggerActions[triggerIndex]
                        for (actionKey in actionKeys) {
                            actionMap[actionKey]?.let { action ->
                                if (action.data is ActionData.InputKeyEvent &&
                                    isModifierKey(action.data.keyCode)
                                ) {
                                    val actionMetaState =
                                        KeyEventUtils.modifierKeycodeToMetaState(
                                            action.data.keyCode,
                                        )

                                    metaStateFromActionsToRemove =
                                        metaStateFromActionsToRemove.withFlag(actionMetaState)
                                }
                            }
                        }
                    }

                    parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex] = false

                    if (triggers[triggerIndex].keys[keyIndex].consumeEvent) {
                        consumeEvent = true
                    }
                }

                // long press
                if (keyAwaitingRelease &&
                    trigger.matchingEventAtIndex(event.withLongPress, keyIndex)
                ) {
                    parallelTriggerEventsAwaitingRelease[triggerIndex][keyIndex] = false

                    parallelTriggerLongPressJobs[triggerIndex]?.cancel()

                    if (triggers[triggerIndex].keys[keyIndex].consumeEvent) {
                        consumeEvent = true
                    }

                    val lastMatchedIndex = lastMatchedEventIndices[triggerIndex]

                    if (isSingleKeyTrigger && successfulLongPressTrigger) {
                        longPressSingleKeyTriggerJustReleased = true
                    }

                    if (!imitateDownUpKeyEvent) {
                        if (isSingleKeyTrigger &&
                            !successfulLongPressTrigger &&
                            !releasedSuccessfulTrigger
                        ) {
                            imitateDownUpKeyEvent = true
                        } else if (lastMatchedIndex > -1 &&
                            lastMatchedIndex < triggers[triggerIndex].keys.lastIndex &&
                            !releasedSuccessfulTrigger
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

            // let actions know that the trigger has been released
            if (lastHeldDownEventIndex != triggers[triggerIndex].keys.lastIndex) {
                parallelTriggerActionPerformers[triggerIndex]?.onReleased(
                    metaStateFromKeyEvent + metaStateFromActions,
                    device = event.performActionDevice(),
                )
            }
        }

        // perform actions on failed long press
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
                }

                iterator.remove()
            }
        }

        detectedSequenceTriggerIndexes.forEach { triggerIndex ->
            sequenceTriggerActionPerformers[triggerIndex]?.onTriggered(
                device = event.performActionDevice(),
                metaState = metaStateFromActions.withFlag(
                    metaStateFromKeyEvent,
                ),
            )
        }

        detectedParallelTriggerIndexes.forEach { triggerIndex ->
            parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
                device = event.performActionDevice(),
                calledOnTriggerRelease = true,
                metaState = metaStateFromActions.withFlag(metaStateFromKeyEvent),
            )
        }

        if (detectedSequenceTriggerIndexes.isNotEmpty() ||
            detectedParallelTriggerIndexes.isNotEmpty()
        ) {
            if (forceVibrate.value) {
                useCase.vibrate(defaultVibrateDuration.value)
            } else {
                vibrateDurations.maxOrNull()?.let {
                    useCase.vibrate(it)
                }
            }
        }

        if (showToast) {
            useCase.showTriggeredToast()
        }

        if (imitateKeyAfterDoublePressTimeout.isNotEmpty() &&
            detectedSequenceTriggerIndexes.isEmpty() &&
            detectedParallelTriggerIndexes.isEmpty() &&
            !longPressSingleKeyTriggerJustReleased
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

                    if (event is KeyEventAlgo) {
                        useCase.imitateKeyEvent(
                            event.keyCode,
                            action = KeyEvent.ACTION_DOWN,
                            scanCode = event.scanCode,
                            source = event.source,
                        )

                        useCase.imitateKeyEvent(
                            event.keyCode,
                            action = KeyEvent.ACTION_UP,
                            scanCode = event.scanCode,
                            source = event.source,
                        )
                    } else if (event is EvdevEventAlgo) {
                        useCase.imitateEvdevEvent(
                            deviceId = event.deviceId,
                            KMEvdevEvent.TYPE_KEY_EVENT,
                            event.scanCode,
                            KMEvdevEvent.VALUE_DOWN,
                        )

                        useCase.imitateEvdevEvent(
                            deviceId = event.deviceId,
                            KMEvdevEvent.TYPE_KEY_EVENT,
                            event.scanCode,
                            KMEvdevEvent.VALUE_UP,
                        )
                    }
                }
            }
            // only imitate a key if an action isn't going to be performed
        } else if ((imitateDownUpKeyEvent || imitateUpKeyEvent) &&
            detectedSequenceTriggerIndexes.isEmpty() &&
            detectedParallelTriggerIndexes.isEmpty() &&
            !shortPressSingleKeyTriggerJustReleased &&
            !mappedToDoublePress
        ) {
            if (event is KeyEventAlgo) {
                if (imitateUpKeyEvent) {
                    useCase.imitateKeyEvent(
                        keyCode = event.keyCode,
                        metaState = metaStateFromKeyEvent.withFlag(metaStateFromActions),
                        deviceId = event.deviceId,
                        action = KeyEvent.ACTION_UP,
                        scanCode = event.scanCode,
                        source = event.source,
                    )
                } else {
                    useCase.imitateKeyEvent(
                        keyCode = event.keyCode,
                        metaState = metaStateFromKeyEvent.withFlag(metaStateFromActions),
                        deviceId = event.deviceId,
                        action = KeyEvent.ACTION_DOWN,
                        scanCode = event.scanCode,
                        source = event.source,
                    )
                    useCase.imitateKeyEvent(
                        keyCode = event.keyCode,
                        metaState = metaStateFromKeyEvent.withFlag(metaStateFromActions),
                        deviceId = event.deviceId,
                        action = KeyEvent.ACTION_UP,
                        scanCode = event.scanCode,
                        source = event.source,
                    )
                }
                keyCodesToImitateUpAction.remove(event.keyCode)
            } else if (event is EvdevEventAlgo) {
                if (imitateUpKeyEvent) {
                    useCase.imitateEvdevEvent(
                        deviceId = event.deviceId,
                        type = KMEvdevEvent.TYPE_KEY_EVENT,
                        code = event.scanCode,
                        value = KMEvdevEvent.VALUE_UP,
                    )
                } else {
                    useCase.imitateEvdevEvent(
                        deviceId = event.deviceId,
                        type = KMEvdevEvent.TYPE_KEY_EVENT,
                        code = event.scanCode,
                        value = KMEvdevEvent.VALUE_DOWN,
                    )
                    useCase.imitateEvdevEvent(
                        deviceId = event.deviceId,
                        type = KMEvdevEvent.TYPE_KEY_EVENT,
                        code = event.scanCode,
                        value = KMEvdevEvent.VALUE_UP,
                    )
                }
                keyCodesToImitateUpAction.remove(event.keyCode)
            }
        }

        return consumeEvent
    }

    fun onAssistantEvent(type: AssistantTriggerType) {
        val event = AssistantEvent(type, clickType = null)
        onKeyDown(event)
        onKeyUp(event)
    }

    fun onFingerprintGesture(type: FingerprintGestureType) {
        val event = FingerprintGestureEvent(type, clickType = null)
        onKeyDown(event)
        onKeyUp(event)
    }

    fun onFloatingButtonDown(buttonUid: String) {
        val event = FloatingButtonEvent(buttonUid, clickType = null)
        onKeyDown(event)
    }

    fun onFloatingButtonUp(buttonUid: String) {
        val event = FloatingButtonEvent(buttonUid, clickType = null)
        onKeyUp(event)
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

        parallelTriggerLongPressJobs.valueIterator().forEach { it.cancel() }
        parallelTriggerLongPressJobs.clear()

        parallelTriggerActionPerformers.values.forEach { it.reset() }
        sequenceTriggerActionPerformers.values.forEach { it.reset() }

        dpadMotionEventTracker.reset()

        performActionsAfterSequenceTriggerTimeout.forEach { (_, job) -> job.cancel() }
        performActionsAfterSequenceTriggerTimeout.clear()
    }

    /**
     * @return whether any actions were performed.
     */
    private fun performActionsOnFailedDoublePress(event: AlgoEvent): Boolean {
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

        detectedTriggerIndexes.forEach { triggerIndex ->
            parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
                device = event.performActionDevice(),
                calledOnTriggerRelease = true,
                metaState = metaStateFromActions.withFlag(metaStateFromKeyEvent),
            )
        }

        if (detectedTriggerIndexes.isNotEmpty()) {
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
        }

        return detectedTriggerIndexes.isNotEmpty()
    }

    private fun encodeActionList(actions: List<Action>): IntArray = actions.map {
        getActionKey(it)
    }.toIntArray()

    /**
     * @return the key for the action in [actionMap]. Returns -1 if the [action] can't be found.
     */
    private fun getActionKey(action: Action): Int {
        actionMap.keyIterator().forEach { key ->
            if (actionMap[key] == action) {
                return key
            }
        }

        throw Exception("Action $action not in the action map!")
    }

    private suspend fun repeatImitatingKey(
        keyCode: Int,
        deviceId: Int,
        scanCode: Int,
        source: Int,
    ) {
        delay(400)

        while (keyCodesToImitateUpAction.contains(keyCode)) {
            useCase.imitateKeyEvent(
                keyCode = keyCode,
                metaState = metaStateFromKeyEvent.withFlag(metaStateFromActions),
                deviceId = deviceId,
                action = KeyEvent.ACTION_DOWN,
                scanCode = scanCode,
                source = source,
            ) // use down action because this is what Android does

            delay(50)
        }
    }

    /**
     * For parallel triggers only.
     */
    private fun performActionsAfterLongPressDelay(event: AlgoEvent, triggerIndex: Int) =
        coroutineScope.launch {
            delay(longPressDelay(triggers[triggerIndex]))

            parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
                device = event.performActionDevice(),
                calledOnTriggerRelease = false,
                metaState = metaStateFromActions.withFlag(metaStateFromKeyEvent),
            )

            if (triggers[triggerIndex].vibrate ||
                forceVibrate.value ||
                triggers[triggerIndex].longPressDoubleVibration
            ) {
                useCase.vibrate(vibrateDuration(triggers[triggerIndex]))
            }

            if (triggers[triggerIndex].showToast) {
                useCase.showTriggeredToast()
            }
        }

    /**
     * For parallel triggers only.
     */
    private fun performActionsAfterSequenceTriggerTimeout(
        event: AlgoEvent,
        triggerIndex: Int,
        sequenceTriggerIndex: Int,
    ) = coroutineScope.launch {
        val timeout = sequenceTriggerTimeout(triggers[sequenceTriggerIndex])

        delay(timeout)

        // If it equals -1 then it means the sequence trigger was triggered
        // and it reset the counter.
        if (lastMatchedEventIndices[sequenceTriggerIndex] == -1) {
            return@launch
        }

        parallelTriggerActionPerformers[triggerIndex]?.onTriggered(
            device = event.performActionDevice(),
            calledOnTriggerRelease = true,
            metaState = metaStateFromActions.withFlag(metaStateFromKeyEvent),
        )

        if (triggers[triggerIndex].vibrate ||
            forceVibrate.value ||
            triggers[triggerIndex].longPressDoubleVibration
        ) {
            useCase.vibrate(vibrateDuration(triggers[triggerIndex]))
        }

        if (triggers[triggerIndex].showToast) {
            useCase.showTriggeredToast()
        }
    }

    private fun Trigger.matchingEventAtIndex(event: AlgoEvent, index: Int): Boolean {
        if (index >= this.keys.size) return false

        return this.keys[index].matchesEvent(event)
    }

    private fun TriggerKey.matchesEvent(event: AlgoEvent): Boolean {
        if (this is KeyEventTriggerKey && event is KeyEventAlgo) {
            val codeMatches = if (this.detectWithScancode()) {
                this.scanCode == event.scanCode
            } else {
                this.keyCode == event.keyCode
            }

            return when (this.device) {
                KeyEventTriggerDevice.Any -> codeMatches && this.clickType == event.clickType

                is KeyEventTriggerDevice.External ->
                    event.isExternal &&
                        codeMatches &&
                        event.descriptor == this.device.descriptor &&
                        this.clickType == event.clickType

                KeyEventTriggerDevice.Internal ->
                    !event.isExternal &&
                        codeMatches &&
                        this.clickType == event.clickType
            }
        } else if (this is EvdevTriggerKey && event is EvdevEventAlgo) {
            val codeMatches = if (this.detectWithScancode()) {
                this.scanCode == event.scanCode
            } else {
                this.keyCode == event.keyCode
            }

            return codeMatches && this.clickType == event.clickType && this.device == event.device
        } else if (this is AssistantTriggerKey && event is AssistantEvent) {
            return if (this.type == AssistantTriggerType.ANY ||
                event.type == AssistantTriggerType.ANY
            ) {
                this.clickType == event.clickType
            } else {
                this.type == event.type && this.clickType == event.clickType
            }
        } else if (this is FingerprintTriggerKey && event is FingerprintGestureEvent) {
            return this.type == event.type && this.clickType == event.clickType
        } else if (this is FloatingButtonKey && event is FloatingButtonEvent) {
            return this.buttonUid == event.buttonUid && this.clickType == event.clickType
        } else {
            return false
        }
    }

    private fun TriggerKey.matchesWithOtherKey(otherKey: TriggerKey): Boolean {
        if (this is KeyEventTriggerKey && otherKey is KeyEventTriggerKey) {
            val codeMatches = if (this.detectWithScancode()) {
                otherKey.detectWithScancode() && this.scanCode == otherKey.scanCode
            } else {
                this.keyCode == otherKey.keyCode
            }

            return when (this.device) {
                KeyEventTriggerDevice.Any ->
                    codeMatches &&
                        this.clickType == otherKey.clickType

                is KeyEventTriggerDevice.External ->
                    codeMatches &&
                        this.device == otherKey.device &&
                        this.clickType == otherKey.clickType

                KeyEventTriggerDevice.Internal ->
                    codeMatches &&
                        otherKey.device == KeyEventTriggerDevice.Internal &&
                        this.clickType == otherKey.clickType
            }
        } else if (this is EvdevTriggerKey && otherKey is EvdevTriggerKey) {
            val codeMatches = if (this.detectWithScancode()) {
                otherKey.detectWithScancode() && this.scanCode == otherKey.scanCode
            } else {
                this.keyCode == otherKey.keyCode
            }

            return codeMatches &&
                this.clickType == otherKey.clickType &&
                this.device == otherKey.device
        } else if (this is AssistantTriggerKey && otherKey is AssistantTriggerKey) {
            return this.type == otherKey.type && this.clickType == otherKey.clickType
        } else if (this is FloatingButtonKey && otherKey is FloatingButtonKey) {
            return this.buttonUid == otherKey.buttonUid && this.clickType == otherKey.clickType
        } else if (this is FingerprintTriggerKey && otherKey is FingerprintTriggerKey) {
            return this.type == otherKey.type && this.clickType == otherKey.clickType
        } else {
            return false
        }
    }

    private fun longPressDelay(trigger: Trigger): Long =
        trigger.longPressDelay?.toLong() ?: defaultLongPressDelay.value

    private fun doublePressTimeout(trigger: Trigger): Long =
        trigger.doublePressDelay?.toLong() ?: defaultDoublePressDelay.value

    private fun vibrateDuration(trigger: Trigger): Long =
        trigger.vibrateDuration?.toLong() ?: defaultVibrateDuration.value

    private fun sequenceTriggerTimeout(trigger: Trigger): Long =
        trigger.sequenceTriggerTimeout?.toLong() ?: defaultSequenceTriggerTimeout.value

    private fun setActionMapAndOptions(actions: Set<Action>) {
        var key = 0

        val map = SparseArrayCompat<Action>()

        actions.forEach { action ->
            map.put(key, action)

            key++
        }

        actionMap = map
    }

    private fun isModifierKey(keyCode: Int): Boolean = when (keyCode) {
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
        KeyEvent.KEYCODE_FUNCTION,
            -> true

        else -> false
    }

    private val AlgoEvent.withShortPress: AlgoEvent
        get() = setClickType(clickType = ClickType.SHORT_PRESS)

    private val AlgoEvent.withLongPress: AlgoEvent
        get() = setClickType(clickType = ClickType.LONG_PRESS)

    private val AlgoEvent.withDoublePress: AlgoEvent
        get() = setClickType(clickType = ClickType.DOUBLE_PRESS)

    private val TriggerKey.consumeEvent: Boolean
        get() {
            return when (this) {
                is AssistantTriggerKey -> true
                is EvdevTriggerKey -> consumeEvent
                is FingerprintTriggerKey -> true
                is FloatingButtonKey -> true
                is KeyEventTriggerKey -> consumeEvent
            }
        }

    /**
     * Represents the kind of event a trigger key is expecting to happen.
     */
    private sealed class AlgoEvent {
        abstract val clickType: ClickType?

        fun setClickType(clickType: ClickType?): AlgoEvent = when (this) {
            is EvdevEventAlgo -> this.copy(clickType = clickType)
            is KeyEventAlgo -> this.copy(clickType = clickType)
            is AssistantEvent -> this.copy(clickType = clickType)
            is FloatingButtonEvent -> this.copy(clickType = clickType)
            is FingerprintGestureEvent -> this.copy(clickType = clickType)
        }
    }

    private data class EvdevEventAlgo(
        val deviceId: Int,
        val device: EvdevDeviceInfo,
        val scanCode: Int,
        val keyCode: Int,
        override val clickType: ClickType?,
    ) : AlgoEvent()

    private data class KeyEventAlgo(
        val keyCode: Int,
        override val clickType: ClickType?,
        val descriptor: String,
        val deviceId: Int,
        val isExternal: Boolean,
        val scanCode: Int,
        val repeatCount: Int,
        val source: Int,
    ) : AlgoEvent()

    private data class AssistantEvent(
        val type: AssistantTriggerType,
        override val clickType: ClickType?,
    ) : AlgoEvent()

    private data class FingerprintGestureEvent(
        val type: FingerprintGestureType,
        override val clickType: ClickType?,
    ) : AlgoEvent()

    private data class FloatingButtonEvent(
        val buttonUid: String,
        override val clickType: ClickType?,
    ) : AlgoEvent()

    private data class TriggerKeyLocation(val triggerIndex: Int, val keyIndex: Int)

    private fun AlgoEvent.performActionDevice(): PerformActionTriggerDevice {
        return when (this) {
            is EvdevEventAlgo -> PerformActionTriggerDevice.Evdev(deviceId)
            else -> PerformActionTriggerDevice.Default
        }
    }
}
