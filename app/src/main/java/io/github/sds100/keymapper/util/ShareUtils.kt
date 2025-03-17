package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.sds100.keymapper.system.files.FileUtils

object ShareUtils {
    fun sendMail(ctx: Context, email: String, subject: String, body: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "vnd.android.cursor.item/email"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
            intent.putExtra(Intent.EXTRA_TEXT, body)
            ctx.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    fun sendZipFile(ctx: Context, file: Uri) {
        try {
            Intent(Intent.ACTION_SEND).apply {
                type = FileUtils.MIME_TYPE_ZIP
                putExtra(Intent.EXTRA_STREAM, file)
                ctx.startActivity(this)
            }
        } catch (_: ActivityNotFoundException) {
        }
    }
}
