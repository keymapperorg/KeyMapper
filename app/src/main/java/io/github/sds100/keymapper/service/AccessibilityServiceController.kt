package io.github.sds100.keymapper.service

import android.content.Context
import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.AppUpdateManager
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 18/01/21.
 */
class AccessibilityServiceController(
    ctx: Context,
    lifecycleOwner: LifecycleOwner,
    iAccessibilityService: IAccessibilityService,
    iConstraintState: IConstraintState,
    private val appUpdateManager: AppUpdateManager
) : IAccessibilityService by iAccessibilityService, LifecycleOwner by lifecycleOwner {

    private val ctx = ctx.applicationContext

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    init {
        appUpdateManager.lastVersionCodeAccessibilityService.observe(this, Observer { oldVersion ->
            if (oldVersion == Constants.VERSION_CODE) return@Observer

            requestFingerprintGestureDetection()

            val handledUpdateInHomeScreen =
                appUpdateManager.lastVersionCodeHomeScreen.value
                    ?.let { it == Constants.VERSION_CODE }
                    ?: false

            if (oldVersion < FingerprintMapUtils.FINGERPRINT_GESTURES_MIN_VERSION
                && fingerprintGestureDetectionAvailable
                && !handledUpdateInHomeScreen) {
                FingerprintMapUtils.showFeatureNotification(ctx)
            }

            denyFingerprintGestureDetection()

            lifecycleScope.launch {
                appUpdateManager.handledAppUpdateInAccessibilityService()
            }
        })
    }
}