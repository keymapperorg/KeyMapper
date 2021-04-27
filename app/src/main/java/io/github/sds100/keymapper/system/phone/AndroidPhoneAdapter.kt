package io.github.sds100.keymapper.system.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success

/**
 * Created by sds100 on 21/04/2021.
 */
class AndroidPhoneAdapter(context: Context) : PhoneAdapter {
    private val ctx = context.applicationContext

    override fun startCall(number: String): Result<*> {
        try {
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return Error.NoAppToPhoneCall
        }
    }
}