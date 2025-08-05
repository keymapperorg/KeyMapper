package io.github.sds100.keymapper.sysbridge

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeManagerImpl
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupControllerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemBridgeHiltModule {

    @Singleton
    @Binds
    abstract fun bindPrivServiceSetupController(impl: SystemBridgeSetupControllerImpl): SystemBridgeSetupController

    @Singleton
    @Binds
    abstract fun bindSystemBridgeManager(impl: SystemBridgeManagerImpl): SystemBridgeManager
}