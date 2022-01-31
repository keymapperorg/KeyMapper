package io.github.sds100.keymapper.util.ui

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.isFixable

/**
 * Created by sds100 on 04/11/2021.
 */
object ViewModelHelper {
    suspend fun handleKeyMapperCrashedDialog(
            resourceProvider: ResourceProvider,
            navigationViewModel: NavigationViewModel,
            popupViewModel: PopupViewModel,
            restartService: () -> Boolean
    ) {
        val dialog = PopupUi.Dialog(
                title = resourceProvider.getString(R.string.dialog_title_key_mapper_crashed),
                message = resourceProvider.getText(R.string.dialog_message_key_mapper_crashed),
                positiveButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_no),
                negativeButtonText = resourceProvider.getString(R.string.neg_cancel),
                neutralButtonText = resourceProvider.getString(R.string.dialog_button_read_dont_kill_my_app_yes)
        )

        val response = popupViewModel.showPopup("app_crashed_prompt", dialog) ?: return

        when (response) {
            DialogResponse.POSITIVE -> navigationViewModel.navigate(
                    "fix_app_killing",
                    NavDestination.FixAppKilling
            )

            DialogResponse.NEUTRAL -> {
                val restartServiceDialog = PopupUi.Ok(
                        message = resourceProvider.getString(R.string.dialog_message_restart_accessibility_service)
                )

                popupViewModel.showPopup("restart_accessibility_service", restartServiceDialog)
                        ?: return

                if (!restartService.invoke()) {
                    handleCantFindAccessibilitySettings(resourceProvider, popupViewModel)
                }
            }
        }
    }

    suspend fun handleCantFindAccessibilitySettings(
            resourceProvider: ResourceProvider,
            popupViewModel: PopupViewModel,
    ) {
        val dialog = PopupUi.Dialog(
                title = resourceProvider.getString(R.string.dialog_title_cant_find_accessibility_settings_page),
                message = resourceProvider.getText(R.string.dialog_message_cant_find_accessibility_settings_page),
                positiveButtonText = resourceProvider.getString(R.string.pos_start_service_with_adb_guide),
                negativeButtonText = resourceProvider.getString(R.string.neg_cancel)
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

    suspend fun handleAccessibilityServiceStoppedSnackBar(
            resourceProvider: ResourceProvider,
            popupViewModel: PopupViewModel,
            startService: () -> Boolean
    ) {

        val snackBar = PopupUi.SnackBar(
                message = resourceProvider.getString(R.string.dialog_message_enable_accessibility_service_to_test_action),
                actionText = resourceProvider.getString(R.string.pos_turn_on)
        )

        popupViewModel.showPopup("snackbar_enable_service", snackBar) ?: return

        if (!startService.invoke()) {
            handleCantFindAccessibilitySettings(resourceProvider, popupViewModel)
        }
    }

    suspend fun handleAccessibilityServiceCrashedSnackBar(
            resourceProvider: ResourceProvider,
            popupViewModel: PopupViewModel,
            restartService: () -> Boolean
    ) {
        val snackBar = PopupUi.SnackBar(
                message = resourceProvider.getString(R.string.dialog_message_restart_accessibility_service_to_test_action),
                actionText = resourceProvider.getString(R.string.pos_restart)
        )

        popupViewModel.showPopup("snackbar_restart_service", snackBar) ?: return

        if (!restartService.invoke()) {
            handleCantFindAccessibilitySettings(resourceProvider, popupViewModel)
        }
    }

    suspend fun showFixErrorDialog(resourceProvider: ResourceProvider,
                                   popupViewModel: PopupViewModel,
                                   error: Error,
                                   fixError: suspend () -> Unit) {

        if (error.isFixable) {
            val dialog = PopupUi.Dialog(
                    title = resourceProvider.getString(R.string.dialog_title_home_fix_error),
                    message = error.getFullMessage(resourceProvider),
                    positiveButtonText = resourceProvider.getString(R.string.dialog_button_fix),
                    negativeButtonText = resourceProvider.getText(R.string.neg_cancel)
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
}