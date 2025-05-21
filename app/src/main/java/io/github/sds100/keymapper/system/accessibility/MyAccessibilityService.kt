package io.github.sds100.keymapper.system.accessibility

import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.base.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityService
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityServiceController
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MyAccessibilityService : BaseAccessibilityService() {

    private var controller: AccessibilityServiceController? = null

    @Inject
    lateinit var coroutineScope: CoroutineScope

    @Inject
    lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapterImpl

    @Inject
    lateinit var detectConstraintsUseCase: DetectConstraintsUseCase

    @Inject
    lateinit var performActionsUseCase: PerformActionsUseCase

    @Inject
    lateinit var detectKeyMapsUseCase: DetectKeyMapsUseCase

    @Inject
    lateinit var fingerprintGesturesSupportedUseCase: FingerprintGesturesSupportedUseCase

    @Inject
    lateinit var rerouteKeyEventsUseCase: RerouteKeyEventsUseCase

    @Inject
    lateinit var pauseKeyMapsUseCase: PauseKeyMapsUseCase

    @Inject
    lateinit var devicesAdapter: DevicesAdapter

    @Inject
    lateinit var suAdapter: SuAdapter

    @Inject
    lateinit var inputMethodAdapter: InputMethodAdapter

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var nodeRepository: AccessibilityNodeRepository

    override fun getController(): BaseAccessibilityServiceController? {
        return controller
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        /*
        I would put this in onCreate but for some reason on some devices getting the application
        context would return null
         */
        if (controller == null) {
            controller = AccessibilityServiceController(
                coroutineScope = coroutineScope,
                accessibilityService = this,
                inputEvents = accessibilityServiceAdapter.eventReceiver,
                outputEvents = accessibilityServiceAdapter.eventsToService,
                detectConstraintsUseCase = detectConstraintsUseCase,
                performActionsUseCase = performActionsUseCase,
                detectKeyMapsUseCase = detectKeyMapsUseCase,
                fingerprintGesturesSupportedUseCase = fingerprintGesturesSupportedUseCase,
                rerouteKeyEventsUseCase = rerouteKeyEventsUseCase,
                pauseKeyMapsUseCase = pauseKeyMapsUseCase,
                devicesAdapter = devicesAdapter,
                suAdapter = suAdapter,
                inputMethodAdapter = inputMethodAdapter,
                settingsRepository = preferenceRepository,
                nodeRepository = nodeRepository,
            )
        }

        controller?.onServiceConnected()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Timber.i("Accessibility service: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        controller?.onDestroy()
        controller = null

        super.onDestroy()
    }
}
