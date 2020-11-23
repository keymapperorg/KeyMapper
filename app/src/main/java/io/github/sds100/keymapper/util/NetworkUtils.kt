package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.result.DownloadFailed
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.SSLHandshakeError
import io.github.sds100.keymapper.util.result.Success
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.init.appCtx
import splitties.systemservices.wifiManager
import splitties.toast.toast
import java.io.File
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.resume

/**
 * Created by sds100 on 04/04/2020.
 */
object NetworkUtils {

    @ExperimentalCoroutinesApi
    suspend fun downloadFile(
        url: String,
        filePath: String
    ): Result<File> = suspendCancellableCoroutine {

        val queue = Volley.newRequestQueue(appCtx)

        val request = StringRequest(Request.Method.GET, url,
            { response ->
                val file = File(filePath)
                file.writeText(response)

                it.resume(Success(file))
            },

            { error ->
                if (error.cause is SSLHandshakeException) {
                    it.resume(SSLHandshakeError())
                } else {
                    it.resume(DownloadFailed())
                }
            })

        it.invokeOnCancellation {
            request.cancel()
        }

        queue.add(request)
    }

    //WiFi stuff
    @Suppress("DEPRECATION")
    fun changeWifiStatePreQ(ctx: Context, stateChange: StateChange) {
        val wifiManager = ctx.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

        when (stateChange) {
            StateChange.ENABLE -> wifiManager.isWifiEnabled = true
            StateChange.DISABLE -> wifiManager.isWifiEnabled = false
            StateChange.TOGGLE -> wifiManager.isWifiEnabled = !isWifiEnabled()
        }
    }

    /**
     * REQUIRES ROOT!!
     */
    fun enableWifiRoot() {
        RootUtils.executeRootCommand("svc wifi enable")
    }

    /**
     * REQUIRES ROOT!!!
     */
    fun disableWifiRoot() {
        RootUtils.executeRootCommand("svc wifi disable")
    }

    /**
     * REQUIRES ROOT!!!
     */
    fun toggleWifiRoot() {
        if (isWifiEnabled()) {
            disableWifiRoot()
        } else {
            enableWifiRoot()
        }
    }
    //Mobile data stuff

    /**
     * REQUIRES ROOT!!
     */
    fun enableMobileData() {
        RootUtils.executeRootCommand("svc data enable")
    }

    /**
     * REQUIRES ROOT!!!
     */
    fun disableMobileData() {
        RootUtils.executeRootCommand("svc data disable")
    }

    /**
     * REQUIRES ROOT!!!
     */
    fun toggleMobileData(ctx: Context) {
        if (isMobileDataEnabled(ctx)) {
            disableMobileData()
        } else {
            enableMobileData()
        }
    }

    private fun isMobileDataEnabled(ctx: Context): Boolean {
        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.isDataEnabled
        } else if (telephonyManager.dataState == TelephonyManager.DATA_CONNECTED) {
            return true
        }

        return false
    }

    private fun isWifiEnabled(): Boolean = wifiManager?.isWifiEnabled ?: false
}

fun Context.openUrl(url: String) {
    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        try {
            startActivity(this)
        } catch (e: ActivityNotFoundException) {
            toast(R.string.error_no_app_found_to_open_url)
        }
    }
}
