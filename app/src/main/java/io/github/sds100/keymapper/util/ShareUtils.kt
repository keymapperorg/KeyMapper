package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

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
}
