package io.github.sds100.keymapper.sysbridge.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket

/**
 * This uses mDNS to scan for the ADB pairing and connection ports.
 */
@RequiresApi(Build.VERSION_CODES.R)
internal class AdbMdns(
    ctx: Context,
    private val serviceType: AdbServiceType,
) {

    private var registered = false
    private var running = false
    private var serviceName: String? = null
    private val nsdManager: NsdManager = ctx.getSystemService(NsdManager::class.java)

    private val _port: MutableStateFlow<Int?> = MutableStateFlow<Int?>(null)
    val port: StateFlow<Int?> = _port.asStateFlow()

    private val discoveryListener: NsdManager.DiscoveryListener =
        object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Timber.d("onDiscoveryStarted: $serviceType")

                registered = true
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.d("onStartDiscoveryFailed: $serviceType, $errorCode")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("onDiscoveryStopped: $serviceType")

                registered = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.d("onStopDiscoveryFailed: $serviceType, $errorCode")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.d("onServiceFound: ${serviceInfo.serviceName}")

                nsdManager.resolveService(serviceInfo, ResolveListener(this@AdbMdns))
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.d("onServiceLost: ${serviceInfo.serviceName}")

                if (serviceInfo.serviceName == serviceName) {
                    _port.update { null }
                }
            }
        }

    fun start() {
        if (running) {
            return
        }

        running = true

        if (!registered) {
            nsdManager.discoverServices(
                serviceType.id,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        }
    }

    fun stop() {
        if (!running) {
            return
        }

        running = false

        if (registered) {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }

    private fun onServiceResolved(resolvedService: NsdServiceInfo) {
        if (running && NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .any { networkInterface ->
                    networkInterface.inetAddresses
                        .asSequence()
                        .any { resolvedService.host.hostAddress == it.hostAddress }
                }
            && isPortAvailable(resolvedService.port)
        ) {
            serviceName = resolvedService.serviceName
            _port.update { resolvedService.port }
        }
    }

    private fun isPortAvailable(port: Int) = try {
        ServerSocket().use {
            it.bind(InetSocketAddress("127.0.0.1", port), 1)
            false
        }
    } catch (e: IOException) {
        true
    }

    internal class ResolveListener(private val adbMdns: AdbMdns) : NsdManager.ResolveListener {
        override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, i: Int) {}

        override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
            adbMdns.onServiceResolved(nsdServiceInfo)
        }
    }
}
