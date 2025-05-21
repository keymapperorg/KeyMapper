package io.github.sds100.keymapper.system.accessibility

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.actions.PerformActionsUseCaseFactory
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCaseFactory
import io.github.sds100.keymapper.base.keymaps.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.detection.DetectKeyMapsUseCaseFactory
import io.github.sds100.keymapper.base.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityService
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityServiceController
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjectorImpl
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
    lateinit var detectConstraintsUseCaseFactory: DetectConstraintsUseCaseFactory

    @Inject
    lateinit var performActionsUseCaseFactory: PerformActionsUseCaseFactory

    @Inject
    lateinit var detectKeyMapsUseCaseFactory: DetectKeyMapsUseCaseFactory

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
            val imeInputEventInjector = ImeInputEventInjectorImpl(
                this,
                keyEventRelayService = keyEventRelayServiceWrapper,
                inputMethodAdapter = inputMethodAdapter,
            )

            controller = AccessibilityServiceController(
                coroutineScope = coroutineScope,
                accessibilityService = this,
                inputEvents = accessibilityServiceAdapter.eventReceiver,
                outputEvents = accessibilityServiceAdapter.eventsToService,
                detectConstraintsUseCase = detectConstraintsUseCaseFactory.create(this),
                performActionsUseCase = performActionsUseCaseFactory.create(
                    accessibilityService = this,
                    imeInputEventInjector = imeInputEventInjector,
                ),
                detectKeyMapsUseCase = detectKeyMapsUseCaseFactory.create(
                    accessibilityService = this,
                    coroutineScope = lifecycleScope,
                    imeInputEventInjector = imeInputEventInjector,
                ),
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
