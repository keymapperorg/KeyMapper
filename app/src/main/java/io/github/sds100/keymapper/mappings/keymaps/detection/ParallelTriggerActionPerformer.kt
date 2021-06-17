package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.actions.KeyEventAction
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 16/06/2021.
 */
class ParallelTriggerActionPerformer(
    private val coroutineScope: CoroutineScope,
    private val useCase: PerformActionsUseCase,
    private val actionList: List<KeyMapAction>,
) {
    private var actionIsHeldDown = BooleanArray(actionList.size) { false }
    private var actionIsRepeating = BooleanArray(actionList.size) { false }

    private val onReleased = MutableSharedFlow<OnReleasedEvent>()

    fun onTriggered(calledOnTriggerRelease: Boolean, metaState: Int) {
        /*
        this job shouldn't be cancelled when the trigger is released. all actions should be performed
        once before repeating (if configured).
         */
        coroutineScope.launch {
            actionList.forEachIndexed { actionIndex, action ->
                var performUpAction = false

                if (action.holdDown && action.repeat && action.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN) {
                    if (actionIsHeldDown[actionIndex]) {
                        actionIsHeldDown[actionIndex] = false
                        performUpAction = true
                    }
                }

                if (action.stopHoldDownWhenTriggerPressedAgain) {
                    if (actionIsHeldDown[actionIndex]) {
                        actionIsHeldDown[actionIndex] = false
                        performUpAction = true
                    }
                }

                if (action.holdDown && !performUpAction) {
                    actionIsHeldDown[actionIndex] = true
                }

                val actionInputEventType = when {
                    performUpAction -> InputEventType.UP
                    action.holdDown -> InputEventType.DOWN
                    else -> InputEventType.DOWN_UP
                }

                performAction(action, actionInputEventType, metaState)

                if (action.repeat && action.holdDown) {
                    delay(action.holdDownDuration?.toLong() ?: useCase.defaultHoldDownDuration.first())
                }

                delay(action.delayBeforeNextAction?.toLong() ?: 0L)

                if (action.holdDown && !action.stopHoldDownWhenTriggerPressedAgain) {
                    launch {
                        val onReleasedEvent = onReleased.first() //wait for trigger to be released
                        if (!actionIsHeldDown[actionIndex]) {
                            return@launch
                        }

                        actionIsHeldDown[actionIndex] = false

                        performAction(action, InputEventType.UP, onReleasedEvent.metaState)
                    }
                }
            }

            actionList.forEachIndexed { actionIndex, action ->
                if (!action.repeat) {
                    return@forEachIndexed
                }

                if (calledOnTriggerRelease && action.repeatMode == RepeatMode.TRIGGER_RELEASED) {
                    return@forEachIndexed
                }

                //don't start repeating if it is already repeating
                if (action.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN && actionIsRepeating[actionIndex]) {
                    actionIsRepeating[actionIndex] = false
                    return@forEachIndexed
                }

                if (action.data is KeyEventAction && KeyEventUtils.isModifierKey(action.data.keyCode)) {
                    return@forEachIndexed
                }

                actionIsRepeating[actionIndex] = true

                delay(action.repeatDelay?.toLong() ?: useCase.defaultRepeatDelay.first())

                launch {
                    var continueRepeating = true
                    var repeatCount = 0

                    launch {
                        onReleased.first() //wait for trigger to be released

                        //stop repeating when the trigger is released only if it is configured to
                        if (action.repeatMode == RepeatMode.TRIGGER_RELEASED) {
                            continueRepeating = false
                        }
                    }

                    while (continueRepeating) {
                        if (action.holdDown && action.repeat) {
                            performAction(action, InputEventType.DOWN, metaState)
                            delay(action.holdDownDuration?.toLong() ?: useCase.defaultHoldDownDuration.first())
                            performAction(action, InputEventType.UP, metaState)
                        } else {
                            performAction(action, InputEventType.DOWN_UP, metaState)
                        }
                    }

                    repeatCount++

                    if (action.repeatLimit != null) {
                        continueRepeating = repeatCount < action.repeatLimit
                    }

                    delay(action.repeatRate?.toLong() ?: useCase.defaultRepeatRate.first())
                }
            }
        }
    }

    fun onReleased(metaState: Int) {
        onReleased.tryEmit(OnReleasedEvent(metaState))
    }

    fun reset() {
        actionIsHeldDown.forEachIndexed { index, isHeldDown ->
            if (isHeldDown) {
                performAction(actionList[index], inputEventType = InputEventType.UP, 0)
            }
        }

        actionIsHeldDown.indices.forEach {
            actionIsHeldDown[it] = false
        }

        actionIsRepeating.indices.forEach {
            actionIsRepeating[it] = false
        }
    }

    private fun performAction(action: KeyMapAction, inputEventType: InputEventType, metaState: Int) {
        repeat(action.multiplier ?: 1) {
            useCase.perform(action.data, inputEventType, metaState)
        }
    }

    private data class OnReleasedEvent(val metaState: Int)
}