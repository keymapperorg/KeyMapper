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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 11/12/20.
 */
@RequiresApi(Build.VERSION_CODES.O)
class FingerprintMapManager(
    private val mCoroutineScope: CoroutineScope,
    iConstraintDelegate: IConstraintDelegate,
    iActionError: IActionError
) : IActionError by iActionError, IConstraintDelegate by iConstraintDelegate {

    var fingerprintMaps: Map<String, FingerprintMap> = emptyMap()

    val performAction = LiveEvent<PerformAction>()
    val vibrate: LiveEvent<Vibrate> = LiveEvent()

    fun onGesture(sdkGestureId: Int) {
        val keyMapperId = FingerprintMapUtils.SDK_ID_TO_KEY_MAPPER_ID[sdkGestureId] ?: return

        fingerprintMaps[keyMapperId]?.apply {
            if (!isEnabled) return
            if (actionList.isEmpty()) return
            if (!constraintList.toTypedArray().constraintsSatisfied(constraintMode)) return

            mCoroutineScope.launch {
                actionList.forEach {
                    if (canActionBePerformed(it).isFailure) return@forEach

                    performAction(
                        action = it,
                        it.showPerformingActionToast,
                        it.multiplier ?: 1
                    )

                    delay(it.delayBeforeNextAction?.toLong() ?: 0)
                }
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
        showPerformingActionToast: Boolean,
        multiplier: Int,
        keyEventAction: KeyEventAction = KeyEventAction.DOWN_UP
    ) {
        repeat(multiplier) {
            performAction.value = PerformAction(
                action,
                showPerformingActionToast,
                0,
                keyEventAction
            )
        }
    }
}