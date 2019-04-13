package io.github.sds100.keymapper.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import io.github.sds100.keymapper.StateChange
import org.jetbrains.anko.toast
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Created by sds100 on 12/12/2018.
 */

object NetworkUtils {

    /**
     * Download a file from a specified [url] to a specified path.
     * @return whether the file was downloaded successfully
     */
    fun downloadFile(ctx: Context, url: String, downloadPath: String): Boolean {
        if (!isNetworkAvailable(ctx)) return false

        val inputStream = URL(url).openStream()

        inputStream.use {
            //only available in Java 7 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.copy(inputStream, Paths.get(downloadPath), StandardCopyOption.REPLACE_EXISTING)
            } else {

                try {
                    val bufferedInputStream = BufferedInputStream(inputStream)
                    val fileOutputStream = FileOutputStream(downloadPath)

                    bufferedInputStream.use {
                        fileOutputStream.use {
                            val dataBuffer = ByteArray(1024)
                            var bytesRead: Int

                            while (true) {
                                bytesRead = bufferedInputStream.read(dataBuffer, 0, 1024)

                                //if at the end of the file, break out of the loop
                                if (bytesRead == -1) break

                                fileOutputStream.write(dataBuffer, 0, bytesRead)
                            }
                        }
                    }

                } catch (e: IOException) {
                    Log.e(this::class.java.simpleName, e.toString())
                    ctx.toast("IO Exception when downloading file")
                    return false
                }
            }
        }

        return true
    }

    //WiFi stuff
    fun changeWifiState(ctx: Context, stateChange: StateChange) {
        val wifiManager = ctx.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

        when (stateChange) {
            StateChange.ENABLE -> wifiManager.isWifiEnabled = true
            StateChange.DISABLE -> wifiManager.isWifiEnabled = false
            StateChange.TOGGLE -> wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
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

    fun toggleMobileData(ctx: Context) {
        if (isMobileDataEnabled(ctx)) {
            disableMobileData()
        } else {
            enableMobileData()
        }
    }

    private fun isNetworkAvailable(ctx: Context): Boolean {
        val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo ?: return false

        return activeNetworkInfo.isConnected
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
}