package io.github.sds100.keymapper.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Created by sds100 on 27/10/2018.
 */

object PackageUtils {
    fun viewAppOnline(ctx: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=$packageName")
            ctx.startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data =
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ctx.startActivity(intent)
        }
    }
}