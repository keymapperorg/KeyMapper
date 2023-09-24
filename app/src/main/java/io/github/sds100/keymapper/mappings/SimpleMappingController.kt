package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 10/01/21.
 */
abstract class SimpleMappingController(
    private val coroutineScope: CoroutineScope,
    private val detectMappingUseCase: DetectMappingUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectConstraintsUseCase: DetectConstraintsUseCase
) {
    private val repeatJobs = mutableMapOf<String, List<RepeatJob>>()
    private val performActionJobs = mutableMapOf<String, Job>()
    private val actionsBeingHeldDown = mutableListOf<Action>()

    private val defaultRepeatRate: StateFlow<Long> =
        performActionsUseCase.defaultRepeatRate.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.REPEAT_RATE.toLong()
        )

    private val forceVibrate: StateFlow<Boolean> =
        detectMappingUseCase.forceVibrate.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.FORCE_VIBRATE
        )
    private val defaultHoldDownDuration: StateFlow<Long> =
        performActionsUseCase.defaultHoldDownDuration.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.HOLD_DOWN_DURATION.toLong()
        )

    private val defaultVibrateDuration: StateFlow<Long> =
        detectMappingUseCase.defaultVibrateDuration.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            PreferenceDefaults.VIBRATION_DURATION.toLong()
        )

    fun onDetected(
        mappingId: String,
        mapping: Mapping<*>
    ) {
        if (!mapping.isEnabled) return
        if (mapping.actionList.isEmpty()) return

        if (mapping.constraintState.constraints.isNotEmpty()) {
            val constraintSnapshot = detectConstraintsUseCase.getSnapshot()
            if (!constraintSnapshot.isSatisfied(mapping.constraintState)) return
        }

        repeatJobs[mappingId]?.forEach { it.cancel() }

        performActionJobs[mappingId]?.cancel()

        performActionJobs[mappingId] = coroutineScope.launch {
            val repeatJobs = mutableListOf<RepeatJob>()

            mapping.actionList.forEach { action ->
                if (performActionsUseCase.getError(action.data) != null) return@forEach

                if (action.repeat && action.repeatMode != RepeatMode.TRIGGER_RELEASED) {
                    var alreadyRepeating = false

                    for (job in this@SimpleMappingController.repeatJobs[mappingId] ?: emptyList()) {
                        if (job.actionUid == action.uid && action.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN) {
                            alreadyRepeating = true
                            job.cancel()
                            break
                        }
                    }

                    if (!alreadyRepeating) {
                        val job = RepeatJob(action.uid) { repeatAction(action) }
                        repeatJobs.add(job)
                        job.start()
                    }
                } else {

                    val alreadyBeingHeldDown = actionsBeingHeldDown.any { action.uid == it.uid }

                    val keyEventAction = when {
                        action.holdDown && !alreadyBeingHeldDown -> InputEventType.DOWN
                        alreadyBeingHeldDown -> InputEventType.UP
                        else -> InputEventType.DOWN_UP
                    }

                    when {
                        action.holdDown -> actionsBeingHeldDown.add(action)
                        alreadyBeingHeldDown -> actionsBeingHeldDown.remove(action)
                    }

                    performAction(action, keyEventAction)
                }

                delay(action.delayBeforeNextAction?.toLong() ?: 0)
            }

            this@SimpleMappingController.repeatJobs[mappingId] = repeatJobs
        }

        if (mapping.vibrate || forceVibrate.value) {
            detectMappingUseCase.vibrate(
                mapping.vibrateDuration?.toLong() ?: defaultVibrateDuration.value
            )
        }

        if (mapping.showToast) {
            detectMappingUseCase.showTriggeredToast()
        }
    }

    private fun performAction(
        action: Action,
        inputEventType: InputEventType = InputEventType.DOWN_UP
    ) {
        repeat(action.multiplier ?: 1) {
            performActionsUseCase.perform(action.data, inputEventType)
        }
    }

    private fun repeatAction(action: Action) = coroutineScope.launch(start = CoroutineStart.LAZY) {
        val repeatRate = action.repeatRate?.toLong() ?: defaultRepeatRate.value

        val holdDownDuration =
            action.holdDownDuration?.toLong() ?: defaultHoldDownDuration.value

        val holdDown = action.holdDown

        var continueRepeating = true
        var repeatCount = 0

        while (continueRepeating) {
            val keyEventAction = when {
                holdDown -> InputEventType.DOWN
                else -> InputEventType.DOWN_UP
            }

            performAction(action, keyEventAction)

            if (holdDown) {
                delay(holdDownDuration)
                performAction(action, InputEventType.UP)
            }

            repeatCount++

            if (action.repeatLimit != null) {
                continueRepeating =
                    repeatCount < action.repeatLimit!! + 1 //this value is how many times it should REPEAT. The first repeat happens after the first time it is performed
            }

            delay(repeatRate)
        }
    }

    fun reset() {
        repeatJobs.values.forEach { jobs ->
            jobs.forEach { it.cancel() }
        }

        repeatJobs.clear()

        performActionJobs.values.forEach {
            it.cancel()
        }

        performActionJobs.clear()

        actionsBeingHeldDown.forEach {
            performAction(it, InputEventType.UP)
        }

        actionsBeingHeldDown.clear()
    }

    private class RepeatJob(val actionUid: String, launch: () -> Job) : Job by launch.invoke()
}
