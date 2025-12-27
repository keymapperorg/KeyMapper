package io.github.sds100.keymapper.sysbridge.adb

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.common.utils.then
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.R)
@Singleton
class AdbManagerImpl @Inject constructor(@ApplicationContext private val ctx: Context) :
    AdbManager {
    companion object {
        private const val LOCALHOST = "127.0.0.1"
    }

    private val commandMutex: Mutex = Mutex()
    private val pairMutex: Mutex = Mutex()

    private val adbConnectMdns: AdbMdns by lazy { AdbMdns(ctx, AdbServiceType.TLS_CONNECT) }
    private val adbPairMdns: AdbMdns by lazy { AdbMdns(ctx, AdbServiceType.TLS_PAIR) }

    override suspend fun executeCommand(command: String): KMResult<String> {
        Timber.d("Execute ADB command: $command")

        val result = withContext(Dispatchers.IO) {
            return@withContext commandMutex.withLock {
                val port = adbConnectMdns.discoverPort()

                if (port == null || port == -1) {
                    return@withLock AdbError.ServerNotFound
                }

                val adbKey = getAdbKey()

                // Recreate a new client every time in case the port changes during the lifetime
                // of AdbManager
                val client: AdbClient = when (adbKey) {
                    is KMError -> return@withLock adbKey
                    is Success<AdbKey> -> AdbClient(LOCALHOST, port, adbKey.value)
                }

                return@withLock with(client) {
                    connect().then {
                        val output = buildString {
                            try {
                                client.shellCommand(command) { append(String(it)) }
                            } catch (e: Exception) {
                                Timber.e(e)
                                AdbError.Unknown(e)
                            }
                        }

                        Success(output)
                    }
                }
            }
        }

        Timber.i("Execute command result: $result")

        return result
    }

    override suspend fun pair(code: String): KMResult<Unit> {
        return pairMutex.withLock {
            val port = adbPairMdns.discoverPort() ?: return@withLock AdbError.ServerNotFound

            return@withLock getAdbKey().then { key ->
                val pairingClient = AdbPairingClient(LOCALHOST, port, code, key)

                with(pairingClient) {
                    try {
                        withContext(Dispatchers.IO) {
                            start()
                        }
                        Timber.i(
                            "Successfully paired with wireless ADB on port $port with code $code",
                        )
                        Success(Unit)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Timber.e(
                            "Failed to pair with wireless ADB on port $port with code $code: $e",
                        )
                        AdbError.PairingError
                    }
                }
            }
        }
    }

    private fun getAdbKey(): KMResult<AdbKey> {
        try {
            return AdbKey(
                PreferenceAdbKeyStore(PreferenceManager.getDefaultSharedPreferences(ctx)),
                "keymapper",
            ).success()
        } catch (e: Throwable) {
            Timber.e(e)
            return AdbError.KeyCreationError
        }
    }
}

interface AdbManager {
    /**
     * Execute an ADB command
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun executeCommand(command: String): KMResult<String>

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun pair(code: String): KMResult<Unit>
}
