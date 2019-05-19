package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.sds100.keymapper.R
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 23/03/2019.
 */

object FeedbackUtils {
    fun sendFeedback(ctx: Context, message: String = "") {

        val emailIntent = Intent(
                Intent.ACTION_SENDTO,
                Uri.fromParts("mailto", ctx.str(R.string.developer_email), null)
        ).apply {
            putExtra(Intent.EXTRA_SUBJECT, ctx.str(R.string.email_subject))
            putExtra(Intent.EXTRA_TEXT, message)

            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }

        try {
            ctx.startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
            ctx.toast(R.string.error_no_app_found_to_send_feedback).show()
        }
    }
}