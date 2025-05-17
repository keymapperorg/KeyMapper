package io.github.sds100.keymapper.base.utils.ui

import androidx.coordinatorlayout.widget.CoordinatorLayout
import io.github.sds100.keymapper.R
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.snackbar.onDismiss
import splitties.snackbar.snack
import kotlin.coroutines.resume


object SnackBarUtils {

    suspend fun show(
        view: CoordinatorLayout,
        text: String,
        actionText: String? = null,
        long: Boolean = false,
    ) =
        suspendCancellableCoroutine<Unit?> { continuation ->

            val snackBar = if (long) {
                view.longSnack(text) {
                    if (actionText != null) {
                        action(actionText) {
                            if (!continuation.isCompleted) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    anchorView = view.findViewById(R.id.fab)
                }
            } else {
                view.snack(text) {
                    if (actionText != null) {
                        action(actionText) {
                            if (!continuation.isCompleted) {
                                continuation.resume(Unit)
                            }
                        }
                    }

                    anchorView = view.findViewById(R.id.fab)
                }
            }

            // if there is no action then there is no point waiting for a user response
            if (actionText == null) {
                continuation.resume(null)
            }

            snackBar.onDismiss {
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
            }
        }
}
