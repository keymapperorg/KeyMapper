package io.github.sds100.keymapper.util

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentActivity
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.util.result.getFullMessage
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.snackbar.snack

/**
 * Created by sds100 on 04/12/20.
 */

fun CoordinatorLayout.showEnableAccessibilityServiceSnackBar() {
    snack(R.string.error_accessibility_service_disabled_record_trigger) {
        setAction(str(R.string.snackbar_fix)) {
            AccessibilityUtils.enableService(context)
        }
    }
}

fun CoordinatorLayout.showFixActionSnackBar(
    failure: Failure,
    activity: FragmentActivity,
    recoverFailureDelegate: RecoverFailureDelegate
) {
    longSnack(failure.getFullMessage(context)) {

        //only add an action to fix the error if the error can be recovered from
        if (failure is RecoverableFailure) {
            action(R.string.snackbar_fix) {
                recoverFailureDelegate.recover(activity, failure)
            }
        }

        show()
    }
}