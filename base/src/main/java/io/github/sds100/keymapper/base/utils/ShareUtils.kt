package io.github.sds100.keymapper.base.utils

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.chooser.ChooserAction
import androidx.core.app.ShareCompat
import androidx.core.net.toUri
import io.github.sds100.keymapper.base.BaseMainActivity
import io.github.sds100.keymapper.base.R

object ShareUtils {
    fun sendMail(
        ctx: Context,
        email: String,
        subject: String,
        body: String,
    ) {
        try {
            val intent =
                Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:$email".toUri()
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }

            ctx.startActivity(Intent.createChooser(intent, null))
        } catch (_: ActivityNotFoundException) {
        }
    }

    fun shareFile(
        ctx: Context,
        file: Uri,
        packageName: String,
    ) {
        try {
            val type = ctx.contentResolver.getType(file)

            ShareCompat
                .IntentBuilder(ctx)
                .setType(type)
                .setStream(file)
                .createChooserIntent()
                .also { intent ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val broadcast =
                            Intent(BaseMainActivity.ACTION_SAVE_FILE).apply {
                                setPackage(packageName)
                                putExtra(BaseMainActivity.EXTRA_FILE_URI, file)
                            }

                        val customActions =
                            arrayOf(
                                ChooserAction
                                    .Builder(
                                        Icon.createWithResource(ctx, R.drawable.ic_outline_save_24),
                                        ctx.getString(R.string.home_export_share_sheet_save_to_files),
                                        PendingIntent.getBroadcast(
                                            ctx,
                                            1,
                                            broadcast,
                                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT,
                                        ),
                                    ).build(),
                            )

                        intent.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, customActions)
                    }

                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    ctx.startActivity(intent)
                }
        } catch (_: ActivityNotFoundException) {
        }
    }
}
