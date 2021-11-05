package io.github.sds100.keymapper.util.ui

import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 04/11/2021.
 */
object ViewModelHelper {
    suspend fun handleKeyMapperCrashedDialog(
        viewModel: BaseViewModel,
        restartService: () -> Boolean
    ) {
        val dialog = PopupUi.Dialog(
            title = viewModel.getString(R.string.dialog_title_key_mapper_crashed),
            message = viewModel.getText(R.string.dialog_message_key_mapper_crashed),
            positiveButtonText = viewModel.getString(R.string.dialog_button_read_dont_kill_my_app_no),
            negativeButtonText = viewModel.getString(R.string.neg_cancel),
            neutralButtonText = viewModel.getString(R.string.dialog_button_read_dont_kill_my_app_yes)
        )

        val response = viewModel.showPopup("app_crashed_prompt", dialog) ?: return

        when (response) {
            DialogResponse.POSITIVE -> viewModel.navigate(
                "fix_app_killing",
                NavDestination.FixAppKilling
            )

            DialogResponse.NEUTRAL -> {
                val restartServiceDialog = PopupUi.Ok(
                    message = viewModel.getString(R.string.dialog_message_restart_accessibility_service)
                )

                viewModel.showPopup("restart_accessibility_service", restartServiceDialog) ?: return

                if (!restartService.invoke()) {
                    handleCantFindAccessibilitySettings(viewModel)
                }
            }
        }
    }

    suspend fun handleCantFindAccessibilitySettings(viewModel: BaseViewModel) {
        val dialog = PopupUi.Dialog(
            title = viewModel.getString(R.string.dialog_title_cant_find_accessibility_settings_page),
            message = viewModel.getText(R.string.dialog_message_cant_find_accessibility_settings_page),
            positiveButtonText = viewModel.getString(R.string.pos_start_service_with_adb_guide),
            negativeButtonText = viewModel.getString(R.string.neg_cancel)
        )

        val response = viewModel.showPopup("cant_find_accessibility_settings", dialog) ?: return

        if (response == DialogResponse.POSITIVE) {
            val openUrlPopup =
                PopupUi.OpenUrl(viewModel.getString(R.string.url_cant_find_accessibility_settings_issue))

            viewModel.showPopup("url_cant_find_accessibility_settings_issue", openUrlPopup)
        }
    }

    suspend fun handleAccessibilityServiceStoppedSnackBar(
        viewModel: BaseViewModel,
        startService: () -> Boolean
    ) {

        val snackBar = PopupUi.SnackBar(
            message = viewModel.getString(R.string.dialog_message_enable_accessibility_service_to_test_action),
            actionText = viewModel.getString(R.string.pos_turn_on)
        )

        viewModel.showPopup("snackbar_enable_service", snackBar) ?: return

        if (!startService.invoke()) {
            handleCantFindAccessibilitySettings(viewModel)
        }
    }

    suspend fun handleAccessibilityServiceCrashedSnackBar(
        viewModel: BaseViewModel,
        restartService: () -> Boolean
    ) {
        val snackBar = PopupUi.SnackBar(
            message = viewModel.getString(R.string.dialog_message_restart_accessibility_service_to_test_action),
            actionText = viewModel.getString(R.string.pos_restart)
        )

        viewModel.showPopup("snackbar_restart_service", snackBar) ?: return

        if (!restartService.invoke()) {
            handleCantFindAccessibilitySettings(viewModel)
        }
    }
}