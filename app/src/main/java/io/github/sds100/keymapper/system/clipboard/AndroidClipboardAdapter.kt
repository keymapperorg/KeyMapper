package io.github.sds100.keymapper.system.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService

/**
 * Created by sds100 on 14/05/2021.
 */
class AndroidClipboardAdapter(context: Context) : ClipboardAdapter {
    private val ctx = context.applicationContext

    private val clipboardManager: ClipboardManager? = ctx.getSystemService()

    override fun copy(label: String, text: String) {
        val clipData = ClipData.newPlainText(label, text)
        clipboardManager?.setPrimaryClip(clipData)
    }
}
