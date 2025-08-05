package io.github.sds100.keymapper.sysbridge.manager

import io.github.sds100.keymapper.sysbridge.ISystemBridge

interface SystemBridgeConnection {
    fun onServiceConnected(service: ISystemBridge)
    fun onServiceDisconnected(service: ISystemBridge)
    fun onBindingDied()
}