package io.github.sds100.keymapper.system.accessibility

import android.app.Service
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import io.github.sds100.keymapper.mappings.detection.PerformActionsUseCase
import io.github.sds100.keymapper.mappings.detection.DetectConstraintsUseCase
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.mappings.detection.DetectFingerprintMapsUseCase
import io.github.sds100.keymapper.mappings.detection.DetectKeyMapsUseCase
import io.github.sds100.keymapper.reroutekeyevents.RerouteKeyEventsUseCase
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(ServiceComponent::class)
object AccessibilityServiceModule {

    @Provides
    @ServiceScoped
    fun provideAccessibilityServiceInterface(service: Service): IAccessibilityService {
        return service as IAccessibilityService
    }

    @Provides
    @ServiceScoped
    fun provideAccessibilityServiceController(
        service: Service,
        adapter: AccessibilityServiceAdapterImpl,
        coroutineScope: CoroutineScope,
        detectConstraintsUseCase: DetectConstraintsUseCase,
        performActionsUseCase: PerformActionsUseCase,
        detectKeyMapsUseCase: DetectKeyMapsUseCase,
        detectFingerprintMapsUseCase: DetectFingerprintMapsUseCase,
        rerouteKeyEventsUseCase: RerouteKeyEventsUseCase,
        pauseMappingsUseCase: PauseMappingsUseCase,
        devicesAdapter: DevicesAdapter,
        suAdapter: SuAdapter,
        settingsRepository: PreferenceRepository
    ): AccessibilityServiceController {
        return AccessibilityServiceController(
            coroutineScope,
            service as IAccessibilityService,
            adapter.eventsToService,
            adapter.eventReceiver,
            detectConstraintsUseCase,
            performActionsUseCase,
            detectKeyMapsUseCase,
            detectFingerprintMapsUseCase,
            rerouteKeyEventsUseCase,
            pauseMappingsUseCase,
            devicesAdapter,
            suAdapter,
            settingsRepository
        )
    }
}