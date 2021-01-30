package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.sds100.keymapper.R
import splitties.toast.toast

/**
 * Created by sds100 on 23/03/2019.
 */

object FeedbackUtils {
    fun emailDeveloper(ctx: Context) {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")

            putExtra(Intent.EXTRA_EMAIL, arrayOf(ctx.str(R.string.developer_email)))
            putExtra(Intent.EXTRA_SUBJECT, ctx.str(R.string.email_subject))
            putExtra(Intent.EXTRA_TEXT, ctx.str(R.string.email_default_message))

            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                ctx.startActivity(this)
            } catch (e: ActivityNotFoundException) {
                ctx.toast(R.string.error_no_app_found_to_send_feedback)
            }
        }
    }
}