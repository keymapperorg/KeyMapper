package io.github.sds100.keymapper.util.delegate

import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.IConstraintDelegate
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.FingerprintMap
import io.github.sds100.keymapper.data.model.getData
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.isFailure
import io.github.sds100.keymapper.util.result.valueOrNull
import kotlinx.coroutines.*
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 11/12/20.
 */
@RequiresApi(Build.VERSION_CODES.O)
class FingerprintGestureMapController(
    private val mCoroutineScope: CoroutineScope,
    iConstraintDelegate: IConstraintDelegate,
    iActionError: IActionError
) : IActionError by iActionError, IConstraintDelegate by iConstraintDelegate {

    private val mRepeatJobs = mutableMapOf<String, List<RepeatJob>>()
    private val mPerformActionJobs = mutableMapOf<String, Job>()

    var fingerprintMaps: Map<String, FingerprintMap> = emptyMap()
        set(value) {
            stopJobs()

            field = value
        }

    val performAction = LiveEvent<PerformAction>()
    val vibrate: LiveEvent<Vibrate> = LiveEvent()

    fun onGesture(sdkGestureId: Int) {
        val keyMapperId = FingerprintMapUtils.SDK_ID_TO_KEY_MAPPER_ID[sdkGestureId] ?: return

        fingerprintMaps[keyMapperId]?.apply {
            if (!isEnabled) return
            if (actionList.isEmpty()) return
            if (!constraintList.toTypedArray().constraintsSatisfied(constraintMode)) return

            mRepeatJobs[keyMapperId]?.forEach { it.cancel() }

            mPerformActionJobs[keyMapperId]?.cancel()

            mPerformActionJobs[keyMapperId] = mCoroutineScope.launch {
                val repeatJobs = mutableListOf<RepeatJob>()

                actionList.forEach {
                    if (canActionBePerformed(it).isFailure) return@forEach

                    if (it.repeat) {
                        var alreadyRepeating = false

                        for (job in mRepeatJobs[keyMapperId] ?: emptyList()) {
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
                        performAction(it)
                    }

                    delay(it.delayBeforeNextAction?.toLong() ?: 0)
                }

                mRepeatJobs[keyMapperId] = repeatJobs
            }

            if (flags.hasFlag(FingerprintMap.FLAG_VIBRATE)) {
                val duration = extras.getData(FingerprintMap.EXTRA_VIBRATION_DURATION)
                    .valueOrNull()?.toLong() ?: AppPreferences.vibrateDuration.toLong()

                vibrate.value = Vibrate(duration)
            }
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
                action.showPerformingActionToast,
                0,
                keyEventAction
            )
        }
    }

    private fun repeatAction(action: Action) = mCoroutineScope.async(start = CoroutineStart.LAZY) {
        val repeatRate = action.repeatRate ?: AppPreferences.repeatRate

        while (true) {
            performAction(action)

            delay(repeatRate.toLong())
        }
    }

    fun stopJobs() {
        mRepeatJobs.values.forEach { jobs ->
            jobs.forEach { it.cancel() }
        }

        mRepeatJobs.clear()

        mPerformActionJobs.values.forEach {
            it.cancel()
        }

        mPerformActionJobs.clear()
    }

    private class RepeatJob(val actionUuid: String, launch: () -> Job) : Job by launch.invoke()
}