package io.github.sds100.keymapper.util.ui

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.snackbar.onDismiss
import splitties.snackbar.snack
import kotlin.coroutines.resume

/**
 * Created by sds100 on 06/04/2021.
 */
object SnackBarUtils {

    suspend fun show(view: CoordinatorLayout, text: String, actionText: String? = null, long: Boolean = false) =
        suspendCancellableCoroutine<PopupUi.SnackBarActionResponse?> { continuation ->

            val snackBar = if (long) {
                view.longSnack(text) {
                    if (actionText != null) {
                        action(actionText) {
                            continuation.resume(PopupUi.SnackBarActionResponse)
                        }
                    }

                    anchorView = view.findViewById(R.id.fab)
                }
            } else {
                view.snack(text) {
                    if (actionText != null) {
                        action(actionText) {
                            continuation.resume(PopupUi.SnackBarActionResponse)
                        }
                    }

                    anchorView = view.findViewById(R.id.fab)
                }
            }

            //if there is no action then there is no point waiting for a user response
            if (actionText == null){
                continuation.resume(null)
            }

            snackBar.onDismiss {
                if (!continuation.isCompleted) {
                    continuation.resume(null)
                }
            }
        }

}