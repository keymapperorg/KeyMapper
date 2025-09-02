package io.github.sds100.keymapper.base.utils.ui

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.isFixable
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission

object ViewModelHelper {
    suspend fun handleKeyMapperCrashedDialog(
        resourceProvider: ResourceProvider,
        dialogProvider: DialogProvider,
        restartService: () -> Boolean,
        ignoreCrashed: () -> Unit,
    ) {
        val dialog = DialogModel.Alert(
            title = resourceProvider.getString(R.string.dialog_title_key_mapper_crashed),
            message = resourceProvider.getText(R.string.dialog_message_key_mapper_crashed),
            positiveButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_yes),
            negativeButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_no),
            neutralButtonText = resourceProvider.getString(R.string.pos_restart),
        )

        val response = dialogProvider.showDialog("app_crashed_prompt", dialog) ?: return

        when (response) {
            DialogResponse.POSITIVE -> {
                val popup =
                    DialogModel.OpenUrl(resourceProvider.getString(R.string.url_dont_kill_my_app))
                dialogProvider.showDialog("dont_kill_my_app", popup)
            }

            DialogResponse.NEGATIVE -> ignoreCrashed()

            DialogResponse.NEUTRAL -> restartService()
        }
    }

    suspend fun showAccessibilityServiceExplanationDialog(
        resourceProvider: ResourceProvider,
        dialogProvider: DialogProvider,
    ): DialogResponse {
        val dialog = DialogModel.Alert(
            title = resourceProvider.getString(R.string.dialog_title_accessibility_service_explanation),
            message = resourceProvider.getString(R.string.dialog_message_accessibility_service_explanation),
            positiveButtonText = resourceProvider.getString(R.string.enable),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
        )

        val response =
            dialogProvider.showDialog("accessibility_service_explanation", dialog)
                ?: return DialogResponse.NEGATIVE

        return response
    }

    suspend fun handleCantFindAccessibilitySettings(
        resourceProvider: ResourceProvider,
        dialogProvider: DialogProvider,
    ) {
        val dialog = DialogModel.Alert(
            title = resourceProvider.getString(R.string.dialog_title_cant_find_accessibility_settings_page),
            message = resourceProvider.getText(R.string.dialog_message_cant_find_accessibility_settings_page),
            positiveButtonText = resourceProvider.getString(R.string.pos_start_service_with_adb_guide),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
        )

        val response =
            dialogProvider.showDialog("cant_find_accessibility_settings", dialog) ?: return

        if (response == DialogResponse.POSITIVE) {
            val url =
                resourceProvider.getString(R.string.url_cant_find_accessibility_settings_issue)
            val openUrlPopup = DialogModel.OpenUrl(url)

            dialogProvider.showDialog("url_cant_find_accessibility_settings_issue", openUrlPopup)
        }
    }

    suspend fun handleAccessibilityServiceStoppedDialog(
        resourceProvider: ResourceProvider,
        dialogProvider: DialogProvider,
        startService: () -> Boolean,
    ) {
        val explanationResponse =
            showAccessibilityServiceExplanationDialog(resourceProvider, dialogProvider)

        if (explanationResponse != DialogResponse.POSITIVE) {
            return
        }

        if (!startService.invoke()) {
            handleCantFindAccessibilitySettings(resourceProvider, dialogProvider)
        }
    }

    suspend fun handleAccessibilityServiceCrashedDialog(
        resourceProvider: ResourceProvider,
        dialogProvider: DialogProvider,
        restartService: () -> Boolean,
    ) {
        val dialog = DialogModel.Alert(
            title = resourceProvider.getString(R.string.dialog_title_accessibility_service_explanation),
            message = resourceProvider.getString(R.string.dialog_message_restart_accessibility_service),
            positiveButtonText = resourceProvider.getString(R.string.pos_restart),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
        )

        val response = dialogProvider.showDialog("accessibility_service_explanation", dialog)

        if (response != DialogResponse.POSITIVE) {
            return
        }

        if (!restartService.invoke()) {
            handleCantFindAccessibilitySettings(resourceProvider, dialogProvider)
        }
    }

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
