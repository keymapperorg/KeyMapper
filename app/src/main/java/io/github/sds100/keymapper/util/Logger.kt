package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.interfaces.OnLogChangedListener
import org.jetbrains.anko.defaultSharedPreferences
import org.jetbrains.anko.toast
import java.io.File
import java.text.DateFormat
import java.util.*

/**
 * Created by sds100 on 11/05/2019.
 */

/**
 * Controls how data is read and written to the app's custom logging feature.
 */
object Logger {
    private const val LOG_FILE_NAME = "log.txt"

    private var mOnChangeListener: OnLogChangedListener? = null

    fun write(ctx: Context, title: String = "", message: String, isError: Boolean = false) {
        //only log if the user has enabled logging
        if (!isLoggingEnabled(ctx)) {
            if (isError && shouldShowErrorToast(ctx)) {
                ctx.toast(R.string.error_enable_log)
            }

            return
        }

        if (isError && shouldShowErrorToast(ctx)) {
            ctx.toast(R.string.error_look_in_log)
        }

        val path = getPath(ctx)
        val time = DateFormat.getDateTimeInstance().format(Date().time)

        if (title.isNotEmpty()) {
            FileUtils.appendTextToFile(
                    path,
                    "\n#### $title\n"
            )
        }

        FileUtils.appendTextToFile(
                path,
                "\n[$time]: $message\n"
        )

        mOnChangeListener?.onLogChange()
    }

    fun read(ctx: Context): String {
        //create the file if it doesn't exist
        val logPath = getPath(ctx)
        File(logPath).createNewFile()

        return FileUtils.getTextFromAppFiles(ctx, LOG_FILE_NAME)
    }

    fun isLoggingEnabled(ctx: Context): Boolean {
        return ctx.defaultSharedPreferences.getBoolean(
                ctx.str(R.string.key_pref_debug),
                ctx.bool(R.bool.default_value_debug))
    }

    fun delete(ctx: Context) {
        ctx.deleteFile(LOG_FILE_NAME)

        mOnChangeListener?.onLogChange()
    }

    fun registerOnLogChangedListener(onLogChangedListener: OnLogChangedListener) {
        mOnChangeListener = onLogChangedListener
    }

    fun unregisterOnLogChangedListener() {
        mOnChangeListener = null
    }

    fun getPath(ctx: Context) = "${ctx.filesDir}/$LOG_FILE_NAME"

    fun shouldShowErrorToast(ctx: Context) =
            ctx.defaultSharedPreferences.getBoolean(ctx.str(R.string.key_pref_show_toast_when_error_encountered),
                    ctx.bool(R.bool.default_value_show_toast_when_error_encountered))
}