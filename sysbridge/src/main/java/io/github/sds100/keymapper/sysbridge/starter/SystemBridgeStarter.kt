package io.github.sds100.keymapper.sysbridge.starter

import android.content.Context
import android.os.Build
import android.os.UserManager
import android.system.ErrnoException
import android.system.Os
import io.github.sds100.keymapper.sysbridge.R
import io.github.sds100.keymapper.sysbridge.ktx.createDeviceProtectedStorageContextCompat
import io.github.sds100.keymapper.sysbridge.ktx.logd
import io.github.sds100.keymapper.sysbridge.ktx.loge
import rikka.core.os.FileUtils
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.zip.ZipFile

// TODO clean up this code and move it to SystemBridgeConnectionManager, and if a lot of starter code in there then move it to a StarterDelegate
internal object SystemBridgeStarter {

    private var commandInternal = arrayOfNulls<String>(2)

    val dataCommand get() = commandInternal[0]!!

    val sdcardCommand get() = commandInternal[1]!!

    /**
     * @return the path to the script file.
     */
    fun writeSdcardFiles(context: Context): String {

        val um = context.getSystemService(UserManager::class.java)!!
        val unlocked = Build.VERSION.SDK_INT < 24 || um.isUserUnlocked
        if (!unlocked) {
            throw IllegalStateException("User is locked")
        }

        val filesDir = context.getExternalFilesDir(null)
            ?: throw IOException("getExternalFilesDir() returns null")
        val dir = filesDir.parentFile ?: throw IOException("$filesDir parentFile returns null")
        val starter = copyStarter(context, File(dir, "starter"))
        val sh = writeScript(context, File(dir, "start.sh"), starter)
        val apkPath = context.applicationInfo.sourceDir
        val libPath = context.applicationInfo.nativeLibraryDir
        val packageName = context.applicationInfo.packageName

        commandInternal[1] = buildStartCommand(sh, apkPath, libPath, packageName)
        logd(commandInternal[1]!!)

        return sh
    }

    fun buildStartCommand(
        sh: String,
        apkPath: String,
        libPath: String,
        packageName: String
    ): String = "sh $sh --apk=$apkPath --lib=$libPath --package=$packageName"

    fun writeDataFiles(context: Context, permission: Boolean = false) {
        if (commandInternal[0] != null && !permission) {
            logd("already written")
            return
        }

        val dir = context.createDeviceProtectedStorageContextCompat().filesDir?.parentFile ?: return

        if (permission) {
            try {
                Os.chmod(dir.absolutePath, 457 /* 0711 */)
            } catch (e: ErrnoException) {
                e.printStackTrace()
            }
        }

        try {
            val starter = copyStarter(context, File(dir, "starter"))
            val sh = writeScript(context, File(dir, "start.sh"), starter)

            val apkPath = context.applicationInfo.sourceDir
            val libPath = context.applicationInfo.nativeLibraryDir

            commandInternal[0] = "sh $sh --apk=$apkPath --lib=$libPath"
            logd(commandInternal[0]!!)

            if (permission) {
                try {
                    Os.chmod(starter, 420 /* 0644 */)
                } catch (e: ErrnoException) {
                    e.printStackTrace()
                }
                try {
                    Os.chmod(sh, 420 /* 0644 */)
                } catch (e: ErrnoException) {
                    e.printStackTrace()
                }
            }
        } catch (e: IOException) {
            loge("write files", e)
        }
    }

    private fun copyStarter(context: Context, out: File): String {
        val so = "lib/${Build.SUPPORTED_ABIS[0]}/libsysbridge.so"
        val ai = context.applicationInfo

        val fos = FileOutputStream(out)
        val apk = ZipFile(ai.sourceDir)
        val entries = apk.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement() ?: break
            if (entry.name != so) continue

            val buf = ByteArray(entry.size.toInt())
            val dis = DataInputStream(apk.getInputStream(entry))
            dis.readFully(buf)
            FileUtils.copy(ByteArrayInputStream(buf), fos)
            break
        }
        return out.absolutePath
    }

    private fun writeScript(context: Context, out: File, starter: String): String {
        if (!out.exists()) {
            out.createNewFile()
        }
        val `is` = BufferedReader(InputStreamReader(context.resources.openRawResource(R.raw.start)))
        val os = PrintWriter(FileWriter(out))
        var line: String?
        while (`is`.readLine().also { line = it } != null) {
            os.println(line!!.replace("%%%STARTER_PATH%%%", starter))
        }
        os.flush()
        os.close()
        return out.absolutePath
    }
}
