package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import io.github.sds100.keymapper.R
import splitties.init.appCtx
import splitties.resources.appStr
import splitties.toast.toast

/**
 * Created by sds100 on 23/03/2019.
 */

object FeedbackUtils {
    fun sendFeedback() {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")

            putExtra(Intent.EXTRA_EMAIL, arrayOf(appStr(R.string.developer_email)))
            putExtra(Intent.EXTRA_SUBJECT, appStr(R.string.email_subject))
            putExtra(Intent.EXTRA_TEXT, appStr(R.string.email_default_message))

            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK)

            try {
                appCtx.startActivity(this)
            } catch (e: ActivityNotFoundException) {
                appCtx.toast(R.string.error_no_app_found_to_send_feedback)
            }
        }
    }
}