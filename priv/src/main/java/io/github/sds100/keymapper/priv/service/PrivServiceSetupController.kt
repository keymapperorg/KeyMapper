package io.github.sds100.keymapper.priv.service

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.priv.adb.AdbClient
import io.github.sds100.keymapper.priv.adb.AdbKey
import io.github.sds100.keymapper.priv.adb.AdbKeyException
import io.github.sds100.keymapper.priv.adb.AdbPairingClient
import io.github.sds100.keymapper.priv.adb.PreferenceAdbKeyStore
import io.github.sds100.keymapper.priv.starter.Starter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This starter code is taken from the Shizuku project.
 */
@Singleton
class PrivServiceSetupControllerImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope
) : PrivServiceSetupController {

    private val sb = StringBuilder()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun startWithAdb(host: String, port: Int) {
        writeStarterFiles()

        sb.append("Starting with wireless adb...").append('\n').append('\n')
        postResult()

        coroutineScope.launch(Dispatchers.IO) {
            val key = try {
                AdbKey(
                    PreferenceAdbKeyStore(PreferenceManager.getDefaultSharedPreferences(ctx)),
                    "shizuku",
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                sb.append('\n').append(Log.getStackTraceString(e))

                postResult(AdbKeyException(e))
                return@launch
            }

            AdbClient(host, port, key).runCatching {
                connect()
                shellCommand(Starter.sdcardCommand) {
                    sb.append(String(it))
                    postResult()
                }
                close()
            }.onFailure {
                it.printStackTrace()

                sb.append('\n').append(Log.getStackTraceString(it))
                postResult(it)
            }

            /* Adb on MIUI Android 11 has no permission to access Android/data.
               Before MIUI Android 12, we can temporarily use /data/user_de.
               After that, is better to implement "adb push" and push files directly to /data/local/tmp.
             */
            if (sb.contains("/Android/data/${ctx.packageName}/start.sh: Permission denied")) {
                sb.append('\n')
                    .appendLine("adb have no permission to access Android/data, how could this possible ?!")
                    .appendLine("try /data/user_de instead...")
                    .appendLine()
                postResult()

                Starter.writeDataFiles(ctx, true)

                AdbClient(host, port, key).runCatching {
                    connect()
                    shellCommand(Starter.dataCommand) {
                        sb.append(String(it))
                        postResult()
                    }
                    close()
                }.onFailure {
                    it.printStackTrace()

                    sb.append('\n').append(Log.getStackTraceString(it))
                    postResult(it)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun writeStarterFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                Starter.writeSdcardFiles(ctx)
            } catch (e: Throwable) {
                // TODO show error message if fails to start
            }
        }
    }

    fun postResult(throwable: Throwable? = null) {
        if (throwable == null) {
            Timber.e(sb.toString())
        } else {
            Timber.e(throwable)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun pairWirelessAdb(port: Int, code: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            val host = "127.0.0.1"

            val key = try {
                AdbKey(
                    PreferenceAdbKeyStore(PreferenceManager.getDefaultSharedPreferences(ctx)),
                    "shizuku",
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                return@launch
            }

            AdbPairingClient(host, port, code.toString(), key).runCatching {
                start()
            }.onFailure {
                Timber.d("Pairing failed: $it")
//                handleResult(false, it)
            }.onSuccess {
                Timber.d("Pairing success")
//                handleResult(it, null)
            }
        }

//        val intent = AdbPairingService.startIntent(ctx)
//        try {
//            ctx.startForegroundService(intent)
//        } catch (e: Throwable) {
//            Timber.e("start ADB pairing service failed: $e")
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
//                e is ForegroundServiceStartNotAllowedException
//            ) {
//                val mode = ctx.getSystemService(AppOpsManager::class.java)
//                    .noteOpNoThrow(
//                        "android:start_foreground",
//                        android.os.Process.myUid(),
//                        ctx.packageName,
//                        null,
//                        null,
//                    )
//                if (mode == AppOpsManager.MODE_ERRORED) {
//                    Toast.makeText(
//                        ctx,
//                        "OP_START_FOREGROUND is denied. What are you doing?",
//                        Toast.LENGTH_LONG,
//                    ).show()
//                }
//                ctx.startService(intent)
//            }
//        }
    }
}

interface PrivServiceSetupController {
    @RequiresApi(Build.VERSION_CODES.R)
    fun pairWirelessAdb(port: Int, code: Int)

    @RequiresApi(Build.VERSION_CODES.R)
    fun startWithAdb(host: String, port: Int)
}