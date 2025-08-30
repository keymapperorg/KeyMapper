package io.github.sds100.keymapper.sysbridge.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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

    private val nsdManager: NsdManager = ctx.getSystemService(NsdManager::class.java)

    private val serviceDiscoveredChannel: Channel<NsdServiceInfo> = Channel(capacity = 10)

    /**
     * Only one service can be resolved at a time.
     * A null value is sent if the service failed to resolve.
     */
    private val serviceResolvedChannel: Channel<NsdServiceInfo?> = Channel(capacity = 1)

    private val isDiscovering: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val discoveredPort: MutableStateFlow<Int?> = MutableStateFlow(null)
    private val discoverMutex: Mutex = Mutex()

    private val resolveListener: NsdManager.ResolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, i: Int) {
            serviceResolvedChannel.trySend(null)
        }

        override fun onServiceResolved(nsdServiceInfo: NsdServiceInfo) {
            Timber.d("onServiceResolved: ${nsdServiceInfo.serviceName} ${nsdServiceInfo.host} ${nsdServiceInfo.port} ${nsdServiceInfo.serviceType}")
            serviceResolvedChannel.trySend(nsdServiceInfo)
        }
    }

    private val discoveryListener: NsdManager.DiscoveryListener =
        object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Timber.d("onDiscoveryStarted: $serviceType")
                isDiscovering.update { true }
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.d("onStartDiscoveryFailed: $serviceType, $errorCode")
                isDiscovering.update { false }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Timber.d("onDiscoveryStopped: $serviceType")
                isDiscovering.update { false }
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Timber.d("onStopDiscoveryFailed: $serviceType, $errorCode")
                isDiscovering.update { false }
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Timber.d("onServiceFound: ${serviceInfo.serviceName} ${serviceInfo.host} ${serviceInfo.port} ${serviceInfo.serviceType}")

                // You can only resolve one service at a time and they can take some time to resolve.
                serviceDiscoveredChannel.trySend(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Timber.d("onServiceLost: ${serviceInfo.serviceName}")
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun discoverPort(): Int? {
        discoverMutex.withLock {
            val currentPort = discoveredPort.value

            if (currentPort == null || !isPortAvailable(currentPort)) {
                val port = withContext(Dispatchers.IO) {
                    discoverPortInternal()
                }
                discoveredPort.value = port
                return port
            } else {
                return currentPort
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun discoverPortInternal(): Int? {
        var port: Int? = null

        if (isDiscovering.value) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (_: Exception) {

            }
        }

        // Wait for it to stop discovering
        isDiscovering.first { !it }

        nsdManager.discoverServices(
            serviceType.id,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )

        try {
            withTimeout(10000L) {
                while (port == null) {
                    val service = serviceDiscoveredChannel.receive()
                    nsdManager.resolveService(service, resolveListener)

                    val resolvedService = serviceResolvedChannel.receive()

                    if (resolvedService == null) {
                        continue
                    }

                    val isLocalNetwork = NetworkInterface.getNetworkInterfaces()
                        .asSequence()
                        .any { networkInterface ->
                            networkInterface.inetAddresses
                                .asSequence()
                                .any { resolvedService.host.hostAddress == it.hostAddress }
                        }

                    if (isLocalNetwork && isPortAvailable(resolvedService.port)) {
                        Timber.d("Discovered ADB port: ${resolvedService.port}")
                        port = resolvedService.port
                    }
                }
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to discover ADB port")
        } finally {
            try {
                if (SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU) >= 7) {
                    nsdManager.stopServiceResolution(resolveListener)
                }
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (_: Exception) {

            }

            // Clear the resolve channel if there is anything left.
            while (!serviceResolvedChannel.isEmpty) {
                serviceResolvedChannel.tryReceive()
            }

            // Clear the discovered channel if there is anything left.
            while (!serviceDiscoveredChannel.isEmpty) {
                serviceDiscoveredChannel.tryReceive()
            }
        }

        return port
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket().use {
                it.bind(InetSocketAddress("127.0.0.1", port), 1)
                false
            }
        } catch (e: IOException) {
            true
        }
    }
}
