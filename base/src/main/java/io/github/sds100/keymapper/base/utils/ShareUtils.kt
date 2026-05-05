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
import io.github.sds100.keymapper.system.files.FileUtils

object ShareUtils {
    fun sendBugReportEmail(ctx: Context, subject: String, attachmentUri: Uri? = null) {
        val body = ctx.getString(
            R.string.customer_email_body,
            Build.DEVICE,
            if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {
                Build.VERSION.RELEASE_OR_CODENAME
            } else {
                Build.VERSION.RELEASE
            },
            ctx.applicationContext.packageManager.getPackageInfo(ctx.packageName, 0).versionName,
        )

        if (attachmentUri == null) {
            sendMail(
                ctx,
                email = ctx.getString(R.string.purchasing_contact_email),
                subject = subject,
                body = body,
            )
        } else {
            sendMailWithAttachment(
                ctx,
                email = ctx.getString(R.string.purchasing_contact_email),
                subject = subject,
                body = body,
                attachmentUri = attachmentUri,
            )
        }
    }

    fun sendMail(ctx: Context, email: String, subject: String, body: String) {
        try {
            // Specify the extra parameters so it works with the gmail app.
            val uri = "mailto:$email?subject=$subject&body=$body".toUri()

            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = uri
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }

            ctx.startActivity(Intent.createChooser(intent, null))
        } catch (_: ActivityNotFoundException) {
        }
    }

    /**
     * Build an email intent that targets [email] with the given [subject] and [body], plus an
     * attached file at [attachmentUri]. ACTION_SENDTO doesn't support attachments so we use
     * ACTION_SEND with a content URI and grant temporary read permission to the chosen app.
     */
    fun sendMailWithAttachment(
        ctx: Context,
        email: String,
        subject: String,
        body: String,
        attachmentUri: Uri,
    ) {
        try {
            ShareCompat.IntentBuilder(ctx)
                .setType(FileUtils.MIME_TYPE_ZIP)
                .setEmailTo(arrayOf(email))
                .setSubject(subject)
                .setText(body)
                .setStream(attachmentUri)
                .createChooserIntent()
                .also { intent ->
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                }
        } catch (_: ActivityNotFoundException) {
        }
    }

    fun shareFile(ctx: Context, file: Uri, packageName: String) {
        try {
            val type = ctx.contentResolver.getType(file)

            ShareCompat.IntentBuilder(ctx)
                .setType(type)
                .setStream(file)
                .createChooserIntent()
                .also { intent ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        val broadcast = Intent(BaseMainActivity.ACTION_SAVE_FILE).apply {
                            setPackage(packageName)
                            putExtra(BaseMainActivity.EXTRA_FILE_URI, file)
                        }

                        val customActions = arrayOf(
                            ChooserAction.Builder(
                                Icon.createWithResource(ctx, R.drawable.ic_outline_save_24),
                                ctx.getString(R.string.home_export_share_sheet_save_to_files),
                                PendingIntent.getBroadcast(
                                    ctx,
                                    1,
                                    broadcast,
                                    PendingIntent.FLAG_IMMUTABLE or
                                        PendingIntent.FLAG_CANCEL_CURRENT,
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
