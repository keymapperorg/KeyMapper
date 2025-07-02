package io.github.sds100.keymapper.base.utils.ui

import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import io.github.sds100.keymapper.base.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object SnackBarUtils {

    suspend fun show(
        view: CoordinatorLayout,
        text: String,
        actionText: String? = null,
        long: Boolean = false,
    ) = suspendCancellableCoroutine { continuation ->

        val duration = if (long) {
            Snackbar.LENGTH_LONG
        } else {
            Snackbar.LENGTH_SHORT
        }

        Snackbar.make(view, text, duration)
            .setAnchorView(R.id.fab)
            .setAction(actionText, {
                if (!continuation.isCompleted) {
                    continuation.resume(Unit)
                }
            })
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (!continuation.isCompleted) {
                        continuation.resume(null)
                    }
                }
            })
            .show()

        // if there is no action then there is no point waiting for a user response
        if (actionText == null) {
            continuation.resume(null)
        }
    }
}
