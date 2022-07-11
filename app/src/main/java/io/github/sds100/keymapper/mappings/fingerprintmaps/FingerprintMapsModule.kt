package io.github.sds100.keymapper.mappings.fingerprintmaps

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FingerprintMapsModule {
    @Binds
    abstract fun bindAreFingerprintGesturesSupportedUseCase(impl: AreFingerprintGesturesSupportedUseCaseImpl): AreFingerprintGesturesSupportedUseCase

    @Binds
    abstract fun bindListFingerprintMapsUseCase(impl: ListFingerprintMapsUseCaseImpl): ListFingerprintMapsUseCase

    @Binds
    abstract fun bindConfigFingerprintMapUseCase(impl: ConfigFingerprintMapUseCaseImpl): ConfigFingerprintMapUseCase
}