package io.github.sds100.keymapper.util.delegate

import androidx.annotation.MainThread
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.isFailure
import io.github.sds100.keymapper.util.result.valueOrNull
import kotlinx.coroutines.*

/**
 * Created by sds100 on 10/01/21.
 */
abstract class SimpleMappingController(
    private val coroutineScope: CoroutineScope,
    iConstraintDelegate: IConstraintDelegate,
    iActionError: IActionError
) : IActionError by iActionError, IConstraintDelegate by iConstraintDelegate {

    private val repeatJobs = mutableMapOf<String, List<RepeatJob>>()
    private val performActionJobs = mutableMapOf<String, Job>()
    private val actionsBeingHeldDown = mutableListOf<Action>()

    val performAction = LiveEvent<PerformAction>()
    val vibrateEvent: LiveEvent<Vibrate> = LiveEvent()
    val showTriggeredToastEvent = LiveEvent<Unit>()

    fun onDetected(
        mappingId: String,
        actionList: List<Action>,
        constraintList: List<Constraint>,
        constraintMode: Int,
        isEnabled: Boolean,
        extras: List<Extra>,
        vibrate: Boolean,
        showTriggeredToast: Boolean
    ) {
        if (!isEnabled) return
        if (actionList.isEmpty()) return
        if (!constraintList.toTypedArray().constraintsSatisfied(constraintMode)) return

        repeatJobs[mappingId]?.forEach { it.cancel() }

        performActionJobs[mappingId]?.cancel()

        performActionJobs[mappingId] = coroutineScope.launch {
            val repeatJobs = mutableListOf<RepeatJob>()

            actionList.forEach {
                if (canActionBePerformed(it).isFailure) return@forEach

                if (it.repeat) {
                    var alreadyRepeating = false

                    for (job in this@SimpleMappingController.repeatJobs[mappingId] ?: emptyList()) {
                        if (job.actionUuid == it.uid) {
                            alreadyRepeating = true
                            job.cancel()
                            break
                        }
                    }

                    if (!alreadyRepeating) {
                        val job = RepeatJob(it.uid) { repeatAction(it) }
                        repeatJobs.add(job)
                        job.start()
                    }
                } else {

                    val alreadyBeingHeldDown =
                        actionsBeingHeldDown.any { action -> action.uid == it.uid }

                    val keyEventAction = when {
                        it.holdDown && !alreadyBeingHeldDown -> KeyEventAction.DOWN
                        alreadyBeingHeldDown -> KeyEventAction.UP
                        else -> KeyEventAction.DOWN_UP
                    }

                    when {
                        it.holdDown -> actionsBeingHeldDown.add(it)
                        alreadyBeingHeldDown -> actionsBeingHeldDown.remove(it)
                    }

                    performAction(it, keyEventAction)
                }

                delay(it.delayBeforeNextAction?.toLong() ?: 0)
            }

            this@SimpleMappingController.repeatJobs[mappingId] = repeatJobs
        }

        if (vibrate) {
            val duration = extras.getData(FingerprintMap.EXTRA_VIBRATION_DURATION)
                .valueOrNull()?.toLong() ?: AppPreferences.vibrateDuration.toLong()

            vibrateEvent.value = Vibrate(duration)
        }

        if (showTriggeredToast) {
            showTriggeredToastEvent.value = Unit
        }
    }

    @MainThread
    private fun performAction(
        action: Action,
        keyEventAction: KeyEventAction = KeyEventAction.DOWN_UP
    ) {
        repeat(action.multiplier ?: 1) {
            performAction.value = PerformAction(
                action,
                0,
                keyEventAction)
        }
    }

    private fun repeatAction(action: Action) = coroutineScope.launch(start = CoroutineStart.LAZY) {
        val repeatRate = action.repeatRate?.toLong() ?: AppPreferences.repeatRate.toLong()

        val holdDownDuration = action.holdDownDuration?.toLong()
            ?: AppPreferences.holdDownDuration.toLong()

        val holdDown = action.holdDown

        while (true) {
            val keyEventAction = when {
                holdDown -> KeyEventAction.DOWN
                else -> KeyEventAction.DOWN_UP
            }

            performAction(action, keyEventAction)

            if (holdDown) {
                delay(holdDownDuration)
                performAction(action, KeyEventAction.UP)
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
            performAction(it, KeyEventAction.UP)
        }

        actionsBeingHeldDown.clear()
    }

    private class RepeatJob(val actionUuid: String, launch: () -> Job) : Job by launch.invoke()
}
