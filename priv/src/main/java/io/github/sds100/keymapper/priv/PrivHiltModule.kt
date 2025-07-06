package io.github.sds100.keymapper.priv

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.priv.service.PrivServiceSetupController
import io.github.sds100.keymapper.priv.service.PrivServiceSetupControllerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrivHiltModule {

    @Singleton
    @Binds
    abstract fun bindPrivServiceSetupController(impl: PrivServiceSetupControllerImpl): PrivServiceSetupController
}