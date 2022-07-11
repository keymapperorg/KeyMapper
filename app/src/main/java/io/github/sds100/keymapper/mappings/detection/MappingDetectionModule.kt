package io.github.sds100.keymapper.mappings.detection

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Created by sds100 on 07/07/2022.
 */
@Module
@InstallIn(ServiceComponent::class)
abstract class MappingDetectionModule {
    @Binds
    @ServiceScoped
    abstract fun bindPerformActionsUseCase(impl: PerformActionsUseCaseImpl): PerformActionsUseCase

    @Binds
    @ServiceScoped
    abstract fun detectKeyMapsUseCase(impl: DetectKeyMapsUseCaseImpl): DetectKeyMapsUseCase

    @Binds
    @ServiceScoped
    abstract fun detectFingerprintMapsUseCase(impl: DetectFingerprintMapsUseCaseImpl): DetectFingerprintMapsUseCase

    @Binds
    @ServiceScoped
    abstract fun detectMappingUseCase(impl: DetectMappingUseCaseImpl):DetectMappingUseCase

    @Binds
    @ServiceScoped
    abstract fun detectConstraintsUseCase(impl: DetectConstraintsUseCaseImpl): DetectConstraintsUseCase
}