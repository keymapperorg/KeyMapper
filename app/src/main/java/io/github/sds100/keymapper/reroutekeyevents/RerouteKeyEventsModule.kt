package io.github.sds100.keymapper.reroutekeyevents

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Created by sds100 on 06/07/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RerouteKeyEventsModule {
    @Binds
    abstract fun bindRerouteKeyEventsUseCase(impl: RerouteKeyEventsUseCaseImpl): RerouteKeyEventsUseCase
}