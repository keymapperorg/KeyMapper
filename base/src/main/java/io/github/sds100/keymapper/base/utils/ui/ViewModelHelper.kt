package io.github.sds100.keymapper.base.utils.ui

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.isFixable
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission

object ViewModelHelper {
    suspend fun showFixErrorDialog(
        resourceProvider: ResourceProvider,
        dialogProvider: DialogProvider,
        error: KMError,
        fixError: suspend () -> Unit,
    ) {
        if (error.isFixable) {
            val dialog = DialogModel.Alert(
                title = resourceProvider.getString(R.string.dialog_title_home_fix_error),
                message = error.getFullMessage(resourceProvider),
                positiveButtonText = resourceProvider.getString(R.string.dialog_button_fix),
                negativeButtonText = resourceProvider.getText(R.string.neg_cancel),
            )

            val response = dialogProvider.showDialog("fix_error", dialog)

            if (response == DialogResponse.POSITIVE) {
                fixError.invoke()
            }
        } else {
            val dialog = DialogModel.Alert(
                message = error.getFullMessage(resourceProvider),
                positiveButtonText = resourceProvider.getString(R.string.pos_ok),
            )

            dialogProvider.showDialog("fix_error", dialog)
        }
    }

    suspend fun showDialogExplainingDndAccessBeingUnavailable(
        resourceProvider: ResourceProvider,
        dialogProvider: DialogProvider,
        neverShowDndTriggerErrorAgain: () -> Unit,
        fixError: suspend () -> Unit,
    ) {
        val dialog = DialogModel.Alert(
            title = resourceProvider.getString(R.string.dialog_title_fix_dnd_trigger_error),
            message = resourceProvider.getText(R.string.dialog_message_fix_dnd_trigger_error),
            positiveButtonText = resourceProvider.getString(R.string.pos_ok),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
            neutralButtonText = resourceProvider.getString(R.string.neg_dont_show_again),
        )

        val dialogResponse = dialogProvider.showDialog("fix_dnd_trigger_error", dialog)

        if (dialogResponse == DialogResponse.POSITIVE) {
            SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)
            fixError.invoke()
        } else if (dialogResponse == DialogResponse.NEUTRAL) {
            neverShowDndTriggerErrorAgain.invoke()
        }
    }
}
