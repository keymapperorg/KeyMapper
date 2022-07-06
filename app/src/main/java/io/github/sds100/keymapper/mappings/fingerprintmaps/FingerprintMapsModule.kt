package io.github.sds100.keymapper.mappings.fingerprintmaps

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class FingerprintMapsModule {
    @Binds
    abstract fun bindAreFingerprintGesturesSupportedUseCase(impl: AreFingerprintGesturesSupportedUseCaseImpl): AreFingerprintGesturesSupportedUseCase
}