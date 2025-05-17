package io.github.sds100.keymapper.mapping.keymaps.detection

import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.base.util.InputEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


class ParallelTriggerActionPerformer(
    private val coroutineScope: CoroutineScope,
    private val useCase: PerformActionsUseCase,
    private val actionList: List<Action>,
) {
    private var actionIsHeldDown = BooleanArray(actionList.size) { false }

    private var repeatJobs = Array<Job?>(actionList.size) { null }
    private var performActionsJob: Job? = null

    private val defaultHoldDownDuration: StateFlow<Long> =
        useCase.defaultHoldDownDuration.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.HOLD_DOWN_DURATION.toLong(),
        )

    private val defaultRepeatDelay: StateFlow<Long> =
        useCase.defaultRepeatDelay.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.REPEAT_DELAY.toLong(),
        )

    private val defaultRepeatRate: StateFlow<Long> =
        useCase.defaultRepeatRate.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.REPEAT_RATE.toLong(),
        )

    fun onTriggered(calledOnTriggerRelease: Boolean, metaState: Int) {
        performActionsJob?.cancel()
        /*
        this job shouldn't be cancelled when the trigger is released. all actions should be performed
        once before repeating (if configured).
         */
        performActionsJob = coroutineScope.launch {
            for ((actionIndex, action) in actionList.withIndex()) {
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
                    delay(action.holdDownDuration?.toLong() ?: defaultHoldDownDuration.value)
                }

                delay(action.delayBeforeNextAction?.toLong() ?: 0L)
            }
        }

        for (job in repeatJobs) {
            job?.cancel()
        }

        for ((actionIndex, action) in actionList.withIndex()) {
            if (!action.repeat) {
                continue
            }

            if (calledOnTriggerRelease && action.repeatMode == RepeatMode.TRIGGER_RELEASED) {
                continue
            }

            // don't start repeating if it is already repeating
            if (action.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN && repeatJobs[actionIndex] != null) {
                repeatJobs[actionIndex]?.cancel()
                repeatJobs[actionIndex] = null

                continue
            }

            if (action.data is ActionData.InputKeyEvent && InputEventUtils.isModifierKey(action.data.keyCode)) {
                continue
            }

            repeatJobs[actionIndex] = coroutineScope.launch {
                var continueRepeating = true
                var repeatCount = 0

                delay(action.repeatDelay?.toLong() ?: defaultRepeatDelay.value)

                while (isActive && continueRepeating) {
                    if (action.holdDown) {
                        performAction(action, InputEventType.DOWN, metaState)
                        delay(
                            action.holdDownDuration?.toLong() ?: defaultHoldDownDuration.value,
                        )
                        performAction(action, InputEventType.UP, metaState)
                    } else {
                        performAction(action, InputEventType.DOWN_UP, metaState)
                    }

                    repeatCount++

                    if (action.repeatLimit != null) {
                        continueRepeating = repeatCount < action.repeatLimit
                    }

                    delay(action.repeatRate?.toLong() ?: defaultRepeatRate.value)
                }
            }
        }
    }

    fun onReleased(metaState: Int) {
        repeatJobs.forEachIndexed { actionIndex, job ->
            if (actionList[actionIndex].repeatMode == RepeatMode.TRIGGER_RELEASED) {
                job?.cancel()
                repeatJobs[actionIndex] = null
            }
        }

        coroutineScope.launch {
            for ((actionIndex, action) in actionList.withIndex()) {
                if (action.holdDown && !action.stopHoldDownWhenTriggerPressedAgain) {
                    if (actionIsHeldDown[actionIndex]) {
                        actionIsHeldDown[actionIndex] = false

                        performAction(action, InputEventType.UP, metaState)
                    }
                }
            }
        }
    }

    fun reset() {
        performActionsJob?.cancel()
        performActionsJob = null

        coroutineScope.launch {
            for ((index, isHeldDown) in actionIsHeldDown.withIndex()) {
                if (isHeldDown) {
                    performAction(actionList[index], inputEventType = InputEventType.UP, 0)
                }
            }
        }

        actionIsHeldDown.indices.forEach {
            actionIsHeldDown[it] = false
        }

        repeatJobs.indices.forEach {
            repeatJobs[it]?.cancel()
            repeatJobs[it] = null
        }
    }

    private suspend fun performAction(
        action: Action,
        inputEventType: InputEventType,
        metaState: Int,
    ) {
        repeat(action.multiplier ?: 1) {
            useCase.perform(action.data, inputEventType, metaState)
        }
    }
}
