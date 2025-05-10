package io.github.sds100.keymapper.mappings.keymaps

import android.database.sqlite.SQLiteConstraintException
import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.FloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.floating.FloatingButtonEntityMapper
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.FingerprintGestureType
import io.github.sds100.keymapper.mappings.GetDefaultKeyMapOptionsUseCase
import io.github.sds100.keymapper.mappings.GetDefaultKeyMapOptionsUseCaseImpl
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerType
import io.github.sds100.keymapper.mappings.keymaps.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.FloatingButtonKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.mappings.keymaps.trigger.Trigger
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerKeyDevice
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceUtils
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.ServiceEvent
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.moveElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.LinkedList

/**
 * Created by sds100 on 16/02/2021.
 */
class ConfigKeyMapUseCaseController(
    private val coroutineScope: CoroutineScope,
    private val keyMapRepository: KeyMapRepository,
    private val devicesAdapter: DevicesAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val floatingLayoutRepository: FloatingLayoutRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val serviceAdapter: ServiceAdapter,
) : ConfigKeyMapUseCase,
    GetDefaultKeyMapOptionsUseCase by GetDefaultKeyMapOptionsUseCaseImpl(
        coroutineScope,
        preferenceRepository,
    ) {

    private var originalKeyMap: KeyMap? = null
    override val keyMap = MutableStateFlow<State<KeyMap>>(State.Loading)

    override val floatingButtonToUse: MutableStateFlow<String?> = MutableStateFlow(null)

    private val showDeviceDescriptors: Flow<Boolean> =
        preferenceRepository.get(Keys.showDeviceDescriptors).map { it == true }

    /**
     * The most recently used is first.
     */
    override val recentlyUsedActions: StateFlow<List<ActionData>> =
        preferenceRepository.get(Keys.recentlyUsedActions)
            .map(::getActionShortcuts)
            .map { it.take(5) }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                emptyList(),
            )

    /**
     * The most recently used is first.
     */
    override val recentlyUsedConstraints: StateFlow<List<Constraint>> =
        combine(
            preferenceRepository.get(Keys.recentlyUsedConstraints).map(::getConstraintShortcuts),
            keyMap.filterIsInstance<State.Data<KeyMap>>(),
        ) { shortcuts, keyMap ->

            // Do not include constraints that the key map already contains.
            shortcuts
                .filter { !keyMap.data.constraintState.constraints.contains(it) }
                .take(5)
        }.stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            emptyList(),
        )

    /**
     * Whether any changes were made to the key map.
     */
    override val isEdited: Boolean
        get() = when (val keyMap = keyMap.value) {
            is State.Data<KeyMap> -> originalKeyMap?.let { it != keyMap.data } ?: false
            State.Loading -> false
        }

    init {
        // Update button data in the key map whenever the floating buttons changes.
        coroutineScope.launch {
            floatingButtonRepository.buttonsList
                .filterIsInstance<State.Data<List<FloatingButtonEntityWithLayout>>>()
                .map { it.data }
                .collectLatest(::updateFloatingButtonTriggerKeys)
        }
    }

    private fun updateFloatingButtonTriggerKeys(buttons: List<FloatingButtonEntityWithLayout>) {
        keyMap.update { keyMapState ->
            if (keyMapState is State.Data) {
                val trigger = keyMapState.data.trigger
                val newKeyMap =
                    keyMapState.data.copy(trigger = trigger.updateFloatingButtonData(buttons))

                State.Data(newKeyMap)
            } else {
                keyMapState
            }
        }
    }

    override fun addConstraint(constraint: Constraint): Boolean {
        var containsConstraint = false

        keyMap.value.ifIsData { mapping ->
            val oldState = mapping.constraintState

            containsConstraint = oldState.constraints.contains(constraint)
            val newState = oldState.copy(constraints = oldState.constraints.plus(constraint))

            setConstraintState(newState)
        }

        preferenceRepository.update(
            Keys.recentlyUsedConstraints,
            { old ->
                val oldList: List<Constraint> = if (old == null) {
                    emptyList()
                } else {
                    Json.decodeFromString<List<Constraint>>(old)
                }

                val newShortcuts = LinkedList(oldList)
                    .also { it.addFirst(constraint) }
                    .distinct()

                Json.encodeToString(newShortcuts as List<Constraint>)
            },
        )

        return !containsConstraint
    }

    override fun removeConstraint(id: String) {
        keyMap.value.ifIsData { mapping ->
            val newList = mapping.constraintState.constraints.toMutableSet().apply {
                removeAll { it.uid == id }
            }

            setConstraintState(mapping.constraintState.copy(constraints = newList))
        }
    }

    override fun setAndMode() {
        keyMap.value.ifIsData { mapping ->
            setConstraintState(mapping.constraintState.copy(mode = ConstraintMode.AND))
        }
    }

    override fun setOrMode() {
        keyMap.value.ifIsData { mapping ->
            setConstraintState(mapping.constraintState.copy(mode = ConstraintMode.OR))
        }
    }

    override fun addAction(data: ActionData) = keyMap.value.ifIsData { mapping ->
        mapping.actionList.toMutableList().apply {
            add(createAction(data))
            setActionList(this)
        }

        preferenceRepository.update(
            Keys.recentlyUsedActions,
            { old ->
                val oldList: List<ActionData> = if (old == null) {
                    emptyList()
                } else {
                    Json.decodeFromString<List<ActionData>>(old)
                }

                val newShortcuts = LinkedList(oldList)
                    .also { it.addFirst(data) }
                    .distinct()

                Json.encodeToString(newShortcuts as List<ActionData>)
            },
        )
    }

    override fun moveAction(fromIndex: Int, toIndex: Int) {
        keyMap.value.ifIsData { mapping ->
            mapping.actionList.toMutableList().apply {
                moveElement(fromIndex, toIndex)
                setActionList(this)
            }
        }
    }

    override fun removeAction(uid: String) {
        keyMap.value.ifIsData { mapping ->
            mapping.actionList.toMutableList().apply {
                removeAll { it.uid == uid }
                setActionList(this)
            }
        }
    }

    override suspend fun addFloatingButtonTriggerKey(buttonUid: String) {
        floatingButtonToUse.update { null }

        editTrigger { trigger ->
            val clickType = when (trigger.mode) {
                is TriggerMode.Parallel -> trigger.mode.clickType
                TriggerMode.Sequence -> ClickType.SHORT_PRESS
                TriggerMode.Undefined -> ClickType.SHORT_PRESS
            }

            // Check whether the trigger already contains the key because if so
            // then it must be converted to a sequence trigger.
            val containsKey = trigger.keys
                .mapNotNull { it as? FloatingButtonKey }
                .any { keyToCompare -> keyToCompare.buttonUid == buttonUid }

            val button = floatingButtonRepository.get(buttonUid)
                ?.let { entity ->
                    FloatingButtonEntityMapper.fromEntity(
                        entity.button,
                        entity.layout.name,
                    )
                }

            val triggerKey = FloatingButtonKey(
                buttonUid = buttonUid,
                button = button,
                clickType = clickType,
            )

            var newKeys = trigger.keys.plus(triggerKey)

            val newMode = when {
                trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
                newKeys.size <= 1 -> TriggerMode.Undefined

                /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
                because this is what most users are expecting when they make a trigger with multiple keys */
                newKeys.size == 2 && !containsKey -> {
                    newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                    TriggerMode.Parallel(triggerKey.clickType)
                }

                else -> trigger.mode
            }

            trigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun addAssistantTriggerKey(type: AssistantTriggerType) = editTrigger { trigger ->
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsAssistantKey = trigger.keys.any { it is AssistantTriggerKey }

        val triggerKey = AssistantTriggerKey(type = type, clickType = clickType)

        val newKeys = trigger.keys.plus(triggerKey).map { it.setClickType(ClickType.SHORT_PRESS) }

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsAssistantKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys.

            It must be a short press because long pressing the assistant key isn't supported.
             */
            !containsAssistantKey -> TriggerMode.Parallel(ClickType.SHORT_PRESS)
            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun addFingerprintGesture(type: FingerprintGestureType) = editTrigger { trigger ->
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsFingerprintGesture = trigger.keys.any { it is FingerprintTriggerKey }

        val triggerKey = FingerprintTriggerKey(type = type, clickType = clickType)

        val newKeys = trigger.keys.plus(triggerKey).map { it.setClickType(ClickType.SHORT_PRESS) }

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsFingerprintGesture -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys.

            It must be a short press because long pressing the assistant key isn't supported.
             */
            !containsFingerprintGesture -> TriggerMode.Parallel(ClickType.SHORT_PRESS)
            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun addKeyCodeTriggerKey(
        keyCode: Int,
        device: TriggerKeyDevice,
        detectionSource: KeyEventDetectionSource,
    ) = editTrigger { trigger ->
        val clickType = when (trigger.mode) {
            is TriggerMode.Parallel -> trigger.mode.clickType
            TriggerMode.Sequence -> ClickType.SHORT_PRESS
            TriggerMode.Undefined -> ClickType.SHORT_PRESS
        }

        // Check whether the trigger already contains the key because if so
        // then it must be converted to a sequence trigger.
        val containsKey = trigger.keys
            .mapNotNull { it as? KeyCodeTriggerKey }
            .any { keyToCompare ->
                keyToCompare.keyCode == keyCode && keyToCompare.device.isSameDevice(device)
            }

        var consumeKeyEvent = true

        // Issue #753
        if (InputEventUtils.isModifierKey(keyCode)) {
            consumeKeyEvent = false
        }

        val triggerKey = KeyCodeTriggerKey(
            keyCode = keyCode,
            device = device,
            clickType = clickType,
            consumeEvent = consumeKeyEvent,
            detectionSource = detectionSource,
        )

        var newKeys = trigger.keys.plus(triggerKey)

        val newMode = when {
            trigger.mode != TriggerMode.Sequence && containsKey -> TriggerMode.Sequence
            newKeys.size <= 1 -> TriggerMode.Undefined

            /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
            because this is what most users are expecting when they make a trigger with multiple keys */
            newKeys.size == 2 && !containsKey -> {
                newKeys = newKeys.map { it.setClickType(triggerKey.clickType) }
                TriggerMode.Parallel(triggerKey.clickType)
            }

            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun removeTriggerKey(uid: String) = editTrigger { trigger ->
        val newKeys = trigger.keys.toMutableList().apply {
            removeAll { it.uid == uid }
        }

        val newMode = when {
            newKeys.size <= 1 -> TriggerMode.Undefined
            else -> trigger.mode
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun moveTriggerKey(fromIndex: Int, toIndex: Int) = editTrigger { trigger ->
        trigger.copy(
            keys = trigger.keys.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            },
        )
    }

    override fun getTriggerKey(uid: String): TriggerKey? {
        return keyMap.value.dataOrNull()?.trigger?.keys?.find { it.uid == uid }
    }

    override fun setParallelTriggerMode() = editTrigger { trigger ->
        if (trigger.mode is TriggerMode.Parallel) {
            return@editTrigger trigger
        }

        // undefined mode only allowed if one or no keys
        if (trigger.keys.size <= 1) {
            return@editTrigger trigger.copy(mode = TriggerMode.Undefined)
        }

        val oldKeys = trigger.keys
        var newKeys = oldKeys

        // set all the keys to a short press if coming from a non-parallel trigger
        // because they must all be the same click type and can't all be double pressed
        newKeys = newKeys
            .map { key -> key.setClickType(clickType = ClickType.SHORT_PRESS) }
            // remove duplicates of keys that have the same keycode and device id
            .distinctBy { key ->
                when (key) {
                    // You can't mix assistant trigger types in a parallel trigger because there is no notion of a "down" key event, which means they can't be pressed at the same time
                    is AssistantTriggerKey, is FingerprintTriggerKey -> 0
                    is KeyCodeTriggerKey -> Pair(key.keyCode, key.device)
                    is FloatingButtonKey -> key.buttonUid
                }
            }

        val newMode = if (newKeys.size <= 1) {
            TriggerMode.Undefined
        } else {
            TriggerMode.Parallel(newKeys[0].clickType)
        }

        trigger.copy(keys = newKeys, mode = newMode)
    }

    override fun setSequenceTriggerMode() = editTrigger { trigger ->
        if (trigger.mode == TriggerMode.Sequence) return@editTrigger trigger
        // undefined mode only allowed if one or no keys
        if (trigger.keys.size <= 1) {
            return@editTrigger trigger.copy(mode = TriggerMode.Undefined)
        }

        trigger.copy(mode = TriggerMode.Sequence)
    }

    override fun setUndefinedTriggerMode() = editTrigger { trigger ->
        if (trigger.mode == TriggerMode.Undefined) return@editTrigger trigger

        // undefined mode only allowed if one or no keys
        if (trigger.keys.size > 1) {
            return@editTrigger trigger
        }

        trigger.copy(mode = TriggerMode.Undefined)
    }

    override fun setTriggerShortPress() {
        editTrigger { oldTrigger ->
            if (oldTrigger.mode == TriggerMode.Sequence) {
                return@editTrigger oldTrigger
            }

            val newKeys = oldTrigger.keys.map { it.setClickType(clickType = ClickType.SHORT_PRESS) }
            val newMode = if (newKeys.size <= 1) {
                TriggerMode.Undefined
            } else {
                TriggerMode.Parallel(ClickType.SHORT_PRESS)
            }
            oldTrigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun setTriggerLongPress() {
        editTrigger { trigger ->
            if (trigger.mode == TriggerMode.Sequence) {
                return@editTrigger trigger
            }

            // You can't set the trigger to a long press if it contains a key
            // that isn't detected with key codes. This is because there aren't
            // separate key events for the up and down press that can be timed.
            if (trigger.keys.any { !it.allowedLongPress }) {
                return@editTrigger trigger
            }

            val newKeys = trigger.keys.map { it.setClickType(clickType = ClickType.LONG_PRESS) }
            val newMode = if (newKeys.size <= 1) {
                TriggerMode.Undefined
            } else {
                TriggerMode.Parallel(ClickType.LONG_PRESS)
            }

            trigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun setTriggerDoublePress() {
        editTrigger { trigger ->
            if (trigger.mode != TriggerMode.Undefined) {
                return@editTrigger trigger
            }

            if (trigger.keys.any { !it.allowedDoublePress }) {
                return@editTrigger trigger
            }

            val newKeys = trigger.keys.map { it.setClickType(clickType = ClickType.DOUBLE_PRESS) }
            val newMode = TriggerMode.Undefined

            trigger.copy(keys = newKeys, mode = newMode)
        }
    }

    override fun setTriggerKeyClickType(keyUid: String, clickType: ClickType) {
        editTriggerKey(keyUid) { key ->
            key.setClickType(clickType = clickType)
        }
    }

    override fun setTriggerKeyDevice(keyUid: String, device: TriggerKeyDevice) {
        editTriggerKey(keyUid) { key ->
            if (key is KeyCodeTriggerKey) {
                key.copy(device = device)
            } else {
                key
            }
        }
    }

    override fun setTriggerKeyConsumeKeyEvent(keyUid: String, consumeKeyEvent: Boolean) {
        editTriggerKey(keyUid) { key ->
            if (key is KeyCodeTriggerKey) {
                key.copy(consumeEvent = consumeKeyEvent)
            } else {
                key
            }
        }
    }

    override fun setAssistantTriggerKeyType(keyUid: String, type: AssistantTriggerType) {
        editTriggerKey(keyUid) { key ->
            if (key is AssistantTriggerKey) {
                key.copy(type = type)
            } else {
                key
            }
        }
    }

    override fun setFingerprintGestureType(keyUid: String, type: FingerprintGestureType) {
        editTriggerKey(keyUid) { key ->
            if (key is FingerprintTriggerKey) {
                key.copy(type = type)
            } else {
                key
            }
        }
    }

    override fun setVibrateEnabled(enabled: Boolean) = editTrigger { it.copy(vibrate = enabled) }

    override fun setVibrationDuration(duration: Int) = editTrigger { trigger ->
        if (duration == defaultVibrateDuration.value) {
            trigger.copy(vibrateDuration = null)
        } else {
            trigger.copy(vibrateDuration = duration)
        }
    }

    override fun setLongPressDelay(delay: Int) = editTrigger { trigger ->
        if (delay == defaultLongPressDelay.value) {
            trigger.copy(longPressDelay = null)
        } else {
            trigger.copy(longPressDelay = delay)
        }
    }

    override fun setDoublePressDelay(delay: Int) {
        editTrigger { trigger ->
            if (delay == defaultDoublePressDelay.value) {
                trigger.copy(doublePressDelay = null)
            } else {
                trigger.copy(doublePressDelay = delay)
            }
        }
    }

    override fun setSequenceTriggerTimeout(delay: Int) {
        editTrigger { trigger ->
            if (delay == defaultSequenceTriggerTimeout.value) {
                trigger.copy(sequenceTriggerTimeout = null)
            } else {
                trigger.copy(sequenceTriggerTimeout = delay)
            }
        }
    }

    override fun setLongPressDoubleVibrationEnabled(enabled: Boolean) {
        editTrigger { it.copy(longPressDoubleVibration = enabled) }
    }

    override fun setTriggerWhenScreenOff(enabled: Boolean) {
        editTrigger { it.copy(screenOffTrigger = enabled) }
    }

    override fun setTriggerFromOtherAppsEnabled(enabled: Boolean) {
        editTrigger { it.copy(triggerFromOtherApps = enabled) }
    }

    override fun setShowToastEnabled(enabled: Boolean) {
        editTrigger { it.copy(showToast = enabled) }
    }

    override fun getAvailableTriggerKeyDevices(): List<TriggerKeyDevice> {
        val externalTriggerKeyDevices = sequence {
            val inputDevices =
                devicesAdapter.connectedInputDevices.value.dataOrNull() ?: emptyList()

            val showDeviceDescriptors = showDeviceDescriptors.firstBlocking()

            inputDevices.forEach { device ->

                if (device.isExternal) {
                    val name = if (showDeviceDescriptors) {
                        InputDeviceUtils.appendDeviceDescriptorToName(
                            device.descriptor,
                            device.name,
                        )
                    } else {
                        device.name
                    }

                    yield(TriggerKeyDevice.External(device.descriptor, name))
                }
            }
        }

        return sequence {
            yield(TriggerKeyDevice.Internal)
            yield(TriggerKeyDevice.Any)
            yieldAll(externalTriggerKeyDevices)
        }.toList()
    }

    override fun setEnabled(enabled: Boolean) {
        editKeyMap { it.copy(isEnabled = enabled) }
    }

    override fun setActionData(uid: String, data: ActionData) {
        editKeyMap { keyMap ->
            val newActionList = keyMap.actionList.map { action ->
                if (action.uid == uid) {
                    action.copy(data = data)
                } else {
                    action
                }
            }

            keyMap.copy(
                actionList = newActionList,
            )
        }
    }

    override fun setActionRepeatEnabled(uid: String, repeat: Boolean) {
        setActionOption(uid) { action -> action.copy(repeat = repeat) }
    }

    override fun setActionRepeatRate(uid: String, repeatRate: Int) {
        setActionOption(uid) { action ->
            if (repeatRate == defaultRepeatRate.value) {
                action.copy(repeatRate = null)
            } else {
                action.copy(repeatRate = repeatRate)
            }
        }
    }

    override fun setActionRepeatDelay(uid: String, repeatDelay: Int) {
        setActionOption(uid) { action ->
            if (repeatDelay == defaultRepeatDelay.value) {
                action.copy(repeatDelay = null)
            } else {
                action.copy(repeatDelay = repeatDelay)
            }
        }
    }

    override fun setActionRepeatLimit(uid: String, repeatLimit: Int) {
        setActionOption(uid) { action ->
            if (action.repeatMode == RepeatMode.LIMIT_REACHED) {
                if (repeatLimit == 1) {
                    action.copy(repeatLimit = null)
                } else {
                    action.copy(repeatLimit = repeatLimit)
                }
            } else {
                if (repeatLimit == Int.MAX_VALUE) {
                    action.copy(repeatLimit = null)
                } else {
                    action.copy(repeatLimit = repeatLimit)
                }
            }
        }
    }

    override fun setActionHoldDownEnabled(uid: String, holdDown: Boolean) = setActionOption(uid) { it.copy(holdDown = holdDown) }

    override fun setActionHoldDownDuration(uid: String, holdDownDuration: Int) {
        setActionOption(uid) { action ->
            if (holdDownDuration == defaultHoldDownDuration.value) {
                action.copy(holdDownDuration = null)
            } else {
                action.copy(holdDownDuration = holdDownDuration)
            }
        }
    }

    override fun setActionStopRepeatingWhenTriggerPressedAgain(uid: String) = setActionOption(uid) { it.copy(repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN) }

    override fun setActionStopRepeatingWhenLimitReached(uid: String) = setActionOption(uid) { it.copy(repeatMode = RepeatMode.LIMIT_REACHED) }

    override fun setActionStopRepeatingWhenTriggerReleased(uid: String) = setActionOption(uid) { it.copy(repeatMode = RepeatMode.TRIGGER_RELEASED) }

    override fun setActionStopHoldingDownWhenTriggerPressedAgain(uid: String, enabled: Boolean) = setActionOption(uid) { it.copy(stopHoldDownWhenTriggerPressedAgain = enabled) }

    override fun setActionMultiplier(uid: String, multiplier: Int) {
        setActionOption(uid) { action ->
            if (multiplier == 1) {
                action.copy(multiplier = null)
            } else {
                action.copy(multiplier = multiplier)
            }
        }
    }

    override fun setDelayBeforeNextAction(uid: String, delay: Int) {
        setActionOption(uid) { action ->
            if (delay == 0) {
                action.copy(delayBeforeNextAction = null)
            } else {
                action.copy(delayBeforeNextAction = delay)
            }
        }
    }

    private fun createAction(data: ActionData): Action {
        var holdDown = false
        var repeat = false

        if (data is ActionData.InputKeyEvent) {
            val trigger = keyMap.value.dataOrNull()?.trigger

            val containsDpadKey: Boolean = if (trigger == null) {
                false
            } else {
                trigger.keys
                    .mapNotNull { it as? KeyCodeTriggerKey }
                    .any { InputEventUtils.isDpadKeyCode(it.keyCode) }
            }

            if (InputEventUtils.isModifierKey(data.keyCode) || containsDpadKey) {
                holdDown = true
                repeat = false
            } else {
                repeat = true
            }
        }

        if (data is ActionData.Volume.Down || data is ActionData.Volume.Up || data is ActionData.Volume.Stream) {
            repeat = true
        }

        if (data is ActionData.AnswerCall) {
            addConstraint(Constraint.PhoneRinging())
        }

        if (data is ActionData.EndCall) {
            addConstraint(Constraint.InPhoneCall())
        }

        return Action(
            data = data,
            repeat = repeat,
            holdDown = holdDown,
        )
    }

    private fun setActionList(actionList: List<Action>) {
        editKeyMap { it.copy(actionList = actionList) }
    }

    private fun setConstraintState(constraintState: ConstraintState) {
        editKeyMap { it.copy(constraintState = constraintState) }
    }

    override suspend fun loadKeyMap(uid: String) {
        keyMap.update { State.Loading }
        val entity = keyMapRepository.get(uid) ?: return
        val floatingButtons = floatingButtonRepository.buttonsList
            .filterIsInstance<State.Data<List<FloatingButtonEntityWithLayout>>>()
            .map { it.data }
            .first()

        val keyMap = KeyMapEntityMapper.fromEntity(entity, floatingButtons)
        this.keyMap.update { State.Data(keyMap) }
        originalKeyMap = keyMap
    }

    override fun loadNewKeyMap(groupUid: String?) {
        val keyMap = KeyMap(groupUid = groupUid)
        this.keyMap.update { State.Data(keyMap) }
        originalKeyMap = keyMap
    }

    override fun save() {
        val keyMap = keyMap.value.dataOrNull() ?: return

        if (keyMap.dbId == null) {
            val entity = KeyMapEntityMapper.toEntity(keyMap, 0)
            try {
                keyMapRepository.insert(entity)
            } catch (e: SQLiteConstraintException) {
                keyMapRepository.update(entity)
            }
        } else {
            keyMapRepository.update(KeyMapEntityMapper.toEntity(keyMap, keyMap.dbId))
        }
    }

    override fun restoreState(keyMap: KeyMap) {
        this.keyMap.value = State.Data(keyMap)
    }

    override suspend fun getFloatingLayoutCount(): Int {
        return floatingLayoutRepository.count()
    }

    override suspend fun sendServiceEvent(event: ServiceEvent): Result<*> {
        return serviceAdapter.send(event)
    }

    private fun setActionOption(
        uid: String,
        block: (action: Action) -> Action,
    ) {
        editKeyMap { keyMap ->
            val newActionList = keyMap.actionList.map { action ->
                if (action.uid == uid) {
                    block.invoke(action)
                } else {
                    action
                }
            }

            keyMap.copy(
                actionList = newActionList,
            )
        }
    }

    private suspend fun getActionShortcuts(json: String?): List<ActionData> {
        if (json == null) {
            return emptyList()
        }

        try {
            return withContext(Dispatchers.Default) {
                val list = Json.decodeFromString<List<ActionData>>(json)

                list.distinct()
            }
        } catch (_: Exception) {
            preferenceRepository.set(Keys.recentlyUsedActions, null)
            return emptyList()
        }
    }

    private suspend fun getConstraintShortcuts(json: String?): List<Constraint> {
        if (json == null) {
            return emptyList()
        }

        try {
            return withContext(Dispatchers.Default) {
                val list = Json.decodeFromString<List<Constraint>>(json)

                list.distinct()
            }
        } catch (_: Exception) {
            preferenceRepository.set(Keys.recentlyUsedConstraints, null)
            return emptyList()
        }
    }

    private inline fun editTrigger(block: (trigger: Trigger) -> Trigger) {
        editKeyMap { keyMap ->
            val newTrigger = block(keyMap.trigger)

            keyMap.copy(trigger = newTrigger)
        }
    }

    private fun editTriggerKey(uid: String, block: (key: TriggerKey) -> TriggerKey) {
        editTrigger { oldTrigger ->
            val newKeys = oldTrigger.keys.map {
                if (it.uid == uid) {
                    block.invoke(it)
                } else {
                    it
                }
            }

            oldTrigger.copy(keys = newKeys)
        }
    }

    private inline fun editKeyMap(block: (keymap: KeyMap) -> KeyMap) {
        keyMap.value.ifIsData { keyMap.value = State.Data(block.invoke(it)) }
    }
}

interface ConfigKeyMapUseCase : GetDefaultKeyMapOptionsUseCase {
    val keyMap: Flow<State<KeyMap>>
    val isEdited: Boolean

    fun save()

    fun setEnabled(enabled: Boolean)

    fun addAction(data: ActionData)
    fun moveAction(fromIndex: Int, toIndex: Int)
    fun removeAction(uid: String)

    val recentlyUsedActions: Flow<List<ActionData>>
    fun setActionData(uid: String, data: ActionData)
    fun setActionMultiplier(uid: String, multiplier: Int)
    fun setDelayBeforeNextAction(uid: String, delay: Int)
    fun setActionRepeatRate(uid: String, repeatRate: Int)
    fun setActionRepeatLimit(uid: String, repeatLimit: Int)
    fun setActionStopRepeatingWhenTriggerPressedAgain(uid: String)
    fun setActionStopRepeatingWhenLimitReached(uid: String)
    fun setActionRepeatEnabled(uid: String, repeat: Boolean)
    fun setActionRepeatDelay(uid: String, repeatDelay: Int)
    fun setActionHoldDownEnabled(uid: String, holdDown: Boolean)
    fun setActionHoldDownDuration(uid: String, holdDownDuration: Int)
    fun setActionStopRepeatingWhenTriggerReleased(uid: String)

    fun setActionStopHoldingDownWhenTriggerPressedAgain(uid: String, enabled: Boolean)

    val recentlyUsedConstraints: Flow<List<Constraint>>
    fun addConstraint(constraint: Constraint): Boolean
    fun removeConstraint(id: String)
    fun setAndMode()
    fun setOrMode()
    suspend fun sendServiceEvent(event: ServiceEvent): Result<*>

    // trigger
    fun addKeyCodeTriggerKey(
        keyCode: Int,
        device: TriggerKeyDevice,
        detectionSource: KeyEventDetectionSource,
    )

    suspend fun addFloatingButtonTriggerKey(buttonUid: String)
    fun addAssistantTriggerKey(type: AssistantTriggerType)
    fun addFingerprintGesture(type: FingerprintGestureType)
    fun removeTriggerKey(uid: String)
    fun getTriggerKey(uid: String): TriggerKey?
    fun moveTriggerKey(fromIndex: Int, toIndex: Int)

    fun restoreState(keyMap: KeyMap)
    suspend fun loadKeyMap(uid: String)
    fun loadNewKeyMap(groupUid: String?)

    fun setParallelTriggerMode()
    fun setSequenceTriggerMode()
    fun setUndefinedTriggerMode()

    fun setTriggerShortPress()
    fun setTriggerLongPress()
    fun setTriggerDoublePress()

    fun setTriggerKeyClickType(keyUid: String, clickType: ClickType)
    fun setTriggerKeyDevice(keyUid: String, device: TriggerKeyDevice)
    fun setTriggerKeyConsumeKeyEvent(keyUid: String, consumeKeyEvent: Boolean)
    fun setAssistantTriggerKeyType(keyUid: String, type: AssistantTriggerType)
    fun setFingerprintGestureType(keyUid: String, type: FingerprintGestureType)

    fun setVibrateEnabled(enabled: Boolean)
    fun setVibrationDuration(duration: Int)
    fun setLongPressDelay(delay: Int)
    fun setDoublePressDelay(delay: Int)
    fun setSequenceTriggerTimeout(delay: Int)
    fun setLongPressDoubleVibrationEnabled(enabled: Boolean)
    fun setTriggerWhenScreenOff(enabled: Boolean)
    fun setTriggerFromOtherAppsEnabled(enabled: Boolean)
    fun setShowToastEnabled(enabled: Boolean)

    fun getAvailableTriggerKeyDevices(): List<TriggerKeyDevice>

    val floatingButtonToUse: MutableStateFlow<String?>
    suspend fun getFloatingLayoutCount(): Int
}
