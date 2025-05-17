package io.github.sds100.keymapper.base.util.ui

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.base.util.getFullMessage
import io.github.sds100.keymapper.base.util.isFixable


object ViewModelHelper {
    suspend fun handleKeyMapperCrashedDialog(
        resourceProvider: ResourceProvider,
        popupViewModel: PopupViewModel,
        restartService: () -> Boolean,
        ignoreCrashed: () -> Unit,
    ) {
        val dialog = PopupUi.Dialog(
            title = resourceProvider.getString(R.string.dialog_title_key_mapper_crashed),
            message = resourceProvider.getText(R.string.dialog_message_key_mapper_crashed),
            positiveButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_yes),
            negativeButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_no),
            neutralButtonText = resourceProvider.getString(R.string.pos_restart),
        )

        val response = popupViewModel.showPopup("app_crashed_prompt", dialog) ?: return

        when (response) {
            DialogResponse.POSITIVE -> {
                val popup =
                    PopupUi.OpenUrl(resourceProvider.getString(R.string.url_dont_kill_my_app))
                popupViewModel.showPopup("dont_kill_my_app", popup)
            }

            DialogResponse.NEGATIVE -> ignoreCrashed()

            DialogResponse.NEUTRAL -> restartService()
        }
    }

    suspend fun showAccessibilityServiceExplanationDialog(
        resourceProvider: ResourceProvider,
        popupViewModel: PopupViewModel,
    ): DialogResponse {
        val dialog = PopupUi.Dialog(
            title = resourceProvider.getString(R.string.dialog_title_accessibility_service_explanation),
            message = resourceProvider.getString(R.string.dialog_message_accessibility_service_explanation),
            positiveButtonText = resourceProvider.getString(R.string.enable),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
        )

        val response =
            popupViewModel.showPopup("accessibility_service_explanation", dialog)
                ?: return DialogResponse.NEGATIVE

        return response
    }

    suspend fun handleCantFindAccessibilitySettings(
        resourceProvider: ResourceProvider,
        popupViewModel: PopupViewModel,
    ) {
        val dialog = PopupUi.Dialog(
            title = resourceProvider.getString(R.string.dialog_title_cant_find_accessibility_settings_page),
            message = resourceProvider.getText(R.string.dialog_message_cant_find_accessibility_settings_page),
            positiveButtonText = resourceProvider.getString(R.string.pos_start_service_with_adb_guide),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
        )

        val response =
            popupViewModel.showPopup("cant_find_accessibility_settings", dialog) ?: return

        if (response == DialogResponse.POSITIVE) {
            val url =
                resourceProvider.getString(R.string.url_cant_find_accessibility_settings_issue)
            val openUrlPopup = PopupUi.OpenUrl(url)

            popupViewModel.showPopup("url_cant_find_accessibility_settings_issue", openUrlPopup)
        }
    }

    suspend fun handleAccessibilityServiceStoppedDialog(
        resourceProvider: ResourceProvider,
        popupViewModel: PopupViewModel,
        startService: () -> Boolean,
    ) {
        val explanationResponse =
            showAccessibilityServiceExplanationDialog(resourceProvider, popupViewModel)

        if (explanationResponse != DialogResponse.POSITIVE) {
            return
        }

        if (!startService.invoke()) {
            handleCantFindAccessibilitySettings(resourceProvider, popupViewModel)
        }
    }

    suspend fun handleAccessibilityServiceCrashedDialog(
        resourceProvider: ResourceProvider,
        popupViewModel: PopupViewModel,
        restartService: () -> Boolean,
    ) {
        val dialog = PopupUi.Dialog(
            title = resourceProvider.getString(R.string.dialog_title_accessibility_service_explanation),
            message = resourceProvider.getString(R.string.dialog_message_restart_accessibility_service),
            positiveButtonText = resourceProvider.getString(R.string.pos_restart),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
        )

        val response = popupViewModel.showPopup("accessibility_service_explanation", dialog)

        if (response != DialogResponse.POSITIVE) {
            return
        }

        if (!restartService.invoke()) {
            handleCantFindAccessibilitySettings(resourceProvider, popupViewModel)
        }
    }

    suspend fun showFixErrorDialog(
        resourceProvider: ResourceProvider,
        popupViewModel: PopupViewModel,
        error: Error,
        fixError: suspend () -> Unit,
    ) {
        if (error.isFixable) {
            val dialog = PopupUi.Dialog(
                title = resourceProvider.getString(R.string.dialog_title_home_fix_error),
                message = error.getFullMessage(resourceProvider),
                positiveButtonText = resourceProvider.getString(R.string.dialog_button_fix),
                negativeButtonText = resourceProvider.getText(R.string.neg_cancel),
            )

            val response = popupViewModel.showPopup("fix_error", dialog)

            if (response == DialogResponse.POSITIVE) {
                fixError.invoke()
            }
        } else {
            val dialog = PopupUi.Dialog(
                message = error.getFullMessage(resourceProvider),
                positiveButtonText = resourceProvider.getString(R.string.pos_ok),
            )

            popupViewModel.showPopup("fix_error", dialog)
        }
    }

    suspend fun showDialogExplainingDndAccessBeingUnavailable(
        resourceProvider: ResourceProvider,
        popupViewModel: PopupViewModel,
        neverShowDndTriggerErrorAgain: () -> Unit,
        fixError: suspend () -> Unit,
    ) {
        val dialog = PopupUi.Dialog(
            title = resourceProvider.getString(R.string.dialog_title_fix_dnd_trigger_error),
            message = resourceProvider.getText(R.string.dialog_message_fix_dnd_trigger_error),
            positiveButtonText = resourceProvider.getString(R.string.pos_ok),
            negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
            neutralButtonText = resourceProvider.getString(R.string.neg_dont_show_again),
        )

        val dialogResponse = popupViewModel.showPopup("fix_dnd_trigger_error", dialog)

        if (dialogResponse == DialogResponse.POSITIVE) {
            val error = SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY)
            fixError.invoke()
        } else if (dialogResponse == DialogResponse.NEUTRAL) {
            neverShowDndTriggerErrorAgain.invoke()
        }
    }
}
