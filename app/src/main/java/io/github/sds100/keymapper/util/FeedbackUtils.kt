package io.github.sds100.keymapper.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 23/03/2019.
 */

object FeedbackUtils {
    fun sendFeedback(ctx: Context) {
        val emailIntent = Intent(Intent.ACTION_SENDTO)

        emailIntent.data = Uri.parse("mailto:${ctx.str(R.string.developer_email)}" +
                "?subject=${ctx.str(R.string.email_subject)}")

        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)

        ctx.startActivity(emailIntent)
    }
}