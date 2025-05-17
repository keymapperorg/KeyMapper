package io.github.sds100.keymapper.base.utils.ui.compose

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.UriHandler
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.base.utils.str

fun UriHandler.openUriSafe(ctx: Context, uri: String) {
    try {
        openUri(uri)
    } catch (e: IllegalArgumentException) {
        Toast.makeText(ctx, ctx.str(R.string.error_no_app_to_open_url), Toast.LENGTH_SHORT).show()
    }
}
