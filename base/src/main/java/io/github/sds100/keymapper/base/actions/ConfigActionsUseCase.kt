package io.github.sds100.keymapper.base.actions

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsUseCase
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.keymaps.ConfigKeyMapState
import io.github.sds100.keymapper.base.keymaps.GetDefaultKeyMapOptionsUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.moveElement
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.LinkedList
import javax.inject.Inject

@ViewModelScoped
class ConfigActionsUseCaseImpl @Inject constructor(
    private val state: ConfigKeyMapState,
    private val preferenceRepository: PreferenceRepository,
    private val configConstraints: ConfigConstraintsUseCase,
    defaultKeyMapOptionsUseCase: GetDefaultKeyMapOptionsUseCase,
) : ConfigActionsUseCase, GetDefaultKeyMapOptionsUseCase by defaultKeyMapOptionsUseCase {

    override val keyMap: StateFlow<State<KeyMap>> = state.keyMap

    /**
     * The most recently used is first.
     */
    override val recentlyUsedActions: Flow<List<ActionData>> =
        preferenceRepository.get(Keys.recentlyUsedActions)
            .map(::getActionShortcuts)
            .map { it.take(5) }

    override fun addAction(data: ActionData) {
        state.update { keyMap ->
            val newActionList = keyMap.actionList.toMutableList().apply {
                add(createAction(keyMap, data))
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

                    Json.encodeToString(newShortcuts)
                },
            )

            keyMap.copy(actionList = newActionList)
        }
    }

    override fun moveAction(fromIndex: Int, toIndex: Int) {
        updateActionList { actionList ->
            actionList.toMutableList().apply {
                moveElement(fromIndex, toIndex)
            }
        }
    }

    override fun removeAction(uid: String) {
        updateActionList { actionList ->
            actionList.toMutableList().apply {
                removeAll { it.uid == uid }
            }
        }
    }

    override fun setActionData(uid: String, data: ActionData) {
        updateActionList { actionList ->
            actionList.map { action ->
                if (action.uid == uid) {
                    action.copy(data = data)
                } else {
                    action
                }
            }
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

    override fun setActionHoldDownEnabled(uid: String, holdDown: Boolean) =
        setActionOption(uid) { it.copy(holdDown = holdDown) }

    override fun setActionHoldDownDuration(uid: String, holdDownDuration: Int) {
        setActionOption(uid) { action ->
            if (holdDownDuration == defaultHoldDownDuration.value) {
                action.copy(holdDownDuration = null)
            } else {
                action.copy(holdDownDuration = holdDownDuration)
            }
        }
    }

    override fun setActionStopRepeatingWhenTriggerPressedAgain(uid: String) =
        setActionOption(uid) { it.copy(repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN) }

    override fun setActionStopRepeatingWhenLimitReached(uid: String) =
        setActionOption(uid) { it.copy(repeatMode = RepeatMode.LIMIT_REACHED) }

    override fun setActionStopRepeatingWhenTriggerReleased(uid: String) =
        setActionOption(uid) { it.copy(repeatMode = RepeatMode.TRIGGER_RELEASED) }

    override fun setActionStopHoldingDownWhenTriggerPressedAgain(uid: String, enabled: Boolean) =
        setActionOption(uid) { it.copy(stopHoldDownWhenTriggerPressedAgain = enabled) }

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

    private fun createAction(keyMap: KeyMap, data: ActionData): Action {
        var holdDown = false
        var repeat = false

        if (data is ActionData.InputKeyEvent) {
            val containsDpadKey: Boolean =
                keyMap.trigger.keys
                    .mapNotNull { it as? KeyEventTriggerKey }
                    .any { KeyEventUtils.isDpadKeyCode(it.keyCode) }

            if (KeyEventUtils.isModifierKey(data.keyCode) || containsDpadKey) {
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
            configConstraints.addConstraint(ConstraintData.PhoneRinging)
        }

        if (data is ActionData.EndCall) {
            configConstraints.addConstraint(ConstraintData.InPhoneCall)
        }

        return Action(
            data = data,
            repeat = repeat,
            holdDown = holdDown,
        )
    }

    private fun updateActionList(block: (actionList: List<Action>) -> List<Action>) {
        state.update { it.copy(actionList = block(it.actionList)) }
    }

    private fun setActionOption(
        uid: String,
        block: (action: Action) -> Action,
    ) {
        state.update { keyMap ->
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
}

interface ConfigActionsUseCase : GetDefaultKeyMapOptionsUseCase {
    val keyMap: StateFlow<State<KeyMap>>

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
}
