package io.github.sds100.keymapper.sysbridge

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.sysbridge.adb.AdbManager
import io.github.sds100.keymapper.sysbridge.adb.AdbManagerImpl
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManagerImpl
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupControllerImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SystemBridgeHiltModule {

    @Singleton
    @Binds
    abstract fun bindSystemBridgeSetupController(impl: SystemBridgeSetupControllerImpl): SystemBridgeSetupController

    @Singleton
    @Binds
    abstract fun bindSystemBridgeManager(impl: SystemBridgeConnectionManagerImpl): SystemBridgeConnectionManager

    @Singleton
    @Binds
    abstract fun bindAdbManager(impl: AdbManagerImpl): AdbManager
}