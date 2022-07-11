package io.github.sds100.keymapper.onboarding

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(ViewModelComponent::class, SingletonComponent::class)
abstract class OnboardingModule {
    @Binds
    abstract fun bindOnboardingUseCase(impl: OnboardingUseCaseImpl): OnboardingUseCase

    @Binds
    abstract fun bindAppIntroUseCase(impl: AppIntroUseCaseImpl): AppIntroUseCase
}