package io.github.sds100.keymapper.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 14/02/2021.
 */

class AppIntroViewModel(
    private val useCase: AppIntroUseCase,
    val slides: List<String>,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    companion object {
        private const val ID_BUTTON_ENABLE_ACCESSIBILITY_SERVICE = "enable_accessibility_service"
        private const val ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE = "restart_accessibility_service"
        private const val ID_BUTTON_DISABLE_BATTERY_OPTIMISATION = "disable_battery_optimisation"
        private const val ID_BUTTON_DONT_KILL_MY_APP = "go_to_dont_kill_my_app"
        private const val ID_BUTTON_MORE_SHIZUKU_INFO = "shizuku_info"
        private const val ID_BUTTON_REQUEST_SHIZUKU_PERMISSION = "request_shizuku_permission"
        private const val ID_BUTTON_REQUEST_NOTIFICATION_PERMISSION =
            "request_notification_permission"
    }

    private val slideModels: StateFlow<List<AppIntroSlideUi>> = combine(
        useCase.serviceState,
        useCase.isBatteryOptimised,
        useCase.fingerprintGesturesSupported,
        useCase.isShizukuPermissionGranted,
        useCase.isNotificationPermissionGranted,
    ) {
            serviceState,
            isBatteryOptimised,
            fingerprintGesturesSupported,
            isShizukuPermissionGranted,
            isNotificationPermissionGranted,
        ->

        slides.map { slide ->
            when (slide) {
                AppIntroSlide.NOTE_FROM_DEV -> noteFromDeveloperSlide()
                AppIntroSlide.ACCESSIBILITY_SERVICE -> accessibilityServiceSlide(serviceState)
                AppIntroSlide.BATTERY_OPTIMISATION -> batteryOptimisationSlide(isBatteryOptimised)
                AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT ->
                    fingerprintGestureSupportSlide(fingerprintGesturesSupported)

                AppIntroSlide.CONTRIBUTING -> contributingSlide()
                AppIntroSlide.SETUP_CHOSEN_DEVICES_AGAIN -> setupChosenDevicesAgainSlide()
                AppIntroSlide.GRANT_SHIZUKU_PERMISSION ->
                    requestShizukuPermissionSlide(isShizukuPermissionGranted)

                AppIntroSlide.NOTIFICATION_PERMISSION ->
                    requestNotificationSlide(isNotificationPermissionGranted)

                else -> throw Exception("Unknown slide $slide")
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onButtonClick(id: String) {
        when (id) {
            ID_BUTTON_ENABLE_ACCESSIBILITY_SERVICE -> useCase.enableAccessibilityService()
            ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE -> useCase.restartAccessibilityService()
            ID_BUTTON_DONT_KILL_MY_APP -> viewModelScope.launch {
                showPopup(
                    "url_dont_kill_my_app",
                    PopupUi.OpenUrl(getString(R.string.url_dont_kill_my_app)),
                )
            }

            ID_BUTTON_DISABLE_BATTERY_OPTIMISATION -> useCase.ignoreBatteryOptimisation()
            ID_BUTTON_MORE_SHIZUKU_INFO -> runBlocking {
                showPopup(
                    "url_shizuku_setting_benefits",
                    PopupUi.OpenUrl(getString(R.string.url_shizuku_setting_benefits)),
                )
            }

            ID_BUTTON_REQUEST_SHIZUKU_PERMISSION -> viewModelScope.launch {
                if (useCase.isShizukuStarted) {
                    useCase.requestShizukuPermission()
                } else {
                    val dialog = PopupUi.Dialog(
                        title = getString(R.string.showcase_shizuku_not_started_title),
                        message = getString(R.string.showcase_shizuku_not_started_message),
                        positiveButtonText = getString(R.string.showcase_shizuku_launch_shizuku_app),
                        negativeButtonText = getString(R.string.neg_cancel),
                    )

                    val response = showPopup("start_shizuku", dialog) ?: return@launch

                    if (response == DialogResponse.POSITIVE) {
                        useCase.openShizuku()
                    }
                }
            }

            ID_BUTTON_REQUEST_NOTIFICATION_PERMISSION -> useCase.requestNotificationPermission()
        }
    }

    fun getSlide(slide: String): Flow<AppIntroSlideUi> =
        slideModels.mapNotNull { allSlides -> allSlides.find { it.id == slide } }

    fun onDoneClick() {
        if (slideModels.value.any { it.id == AppIntroSlide.GRANT_SHIZUKU_PERMISSION }) {
            useCase.shownShizukuPermissionPrompt()
        }

        useCase.shownAppIntro()
    }

    private fun noteFromDeveloperSlide() = AppIntroSlideUi(
        id = AppIntroSlide.NOTE_FROM_DEV,
        image = getDrawable(R.mipmap.ic_launcher_round),
        title = getString(R.string.showcase_note_from_the_developer_title),
        description = getString(R.string.showcase_note_from_the_developer_description),
        backgroundColor = getColor(R.color.slideRed),
    )

    private fun accessibilityServiceSlide(serviceState: ServiceState): AppIntroSlideUi =
        when (serviceState) {
            ServiceState.ENABLED ->
                AppIntroSlideUi(
                    id = AppIntroSlide.ACCESSIBILITY_SERVICE,
                    image = getDrawable(R.drawable.ic_baseline_check_64),
                    title = getString(R.string.showcase_accessibility_service_title_enabled),
                    description = getString(R.string.showcase_accessibility_service_description_enabled),
                    backgroundColor = getColor(R.color.slidePurple),
                )

            ServiceState.CRASHED ->
                AppIntroSlideUi(
                    id = AppIntroSlide.ACCESSIBILITY_SERVICE,
                    image = getDrawable(R.drawable.ic_outline_error_outline_64),
                    title = getString(R.string.showcase_accessibility_service_title_crashed),
                    description = getString(R.string.showcase_accessibility_service_description_crashed),
                    backgroundColor = getColor(R.color.slidePurple),

                    buttonId1 = ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE,
                    buttonText1 = getString(R.string.showcase_accessibility_service_button_restart),
                )

            ServiceState.DISABLED ->
                AppIntroSlideUi(
                    id = AppIntroSlide.ACCESSIBILITY_SERVICE,
                    image = getDrawable(R.drawable.ic_outline_error_outline_64),
                    title = getString(R.string.showcase_accessibility_service_title_disabled),
                    description = getString(R.string.showcase_accessibility_service_description_disabled),
                    backgroundColor = getColor(R.color.slidePurple),

                    buttonId1 = ID_BUTTON_ENABLE_ACCESSIBILITY_SERVICE,
                    buttonText1 = getString(R.string.enable),
                )
        }

    private fun batteryOptimisationSlide(isBatteryOptimised: Boolean): AppIntroSlideUi {
        if (isBatteryOptimised) {
            return AppIntroSlideUi(
                id = AppIntroSlide.BATTERY_OPTIMISATION,
                image = getDrawable(R.drawable.ic_battery_std_white_64dp),
                title = getString(R.string.showcase_disable_battery_optimisation_title),
                description = getString(R.string.showcase_disable_battery_optimisation_message_bad),
                backgroundColor = getColor(R.color.slideBlue),

                buttonId1 = ID_BUTTON_DONT_KILL_MY_APP,
                buttonText1 = getString(R.string.showcase_disable_battery_optimisation_button_dont_kill_my_app),

                buttonId2 = ID_BUTTON_DISABLE_BATTERY_OPTIMISATION,
                buttonText2 = getString(R.string.showcase_disable_battery_optimisation_button_turn_off),
            )
        } else {
            return AppIntroSlideUi(
                id = AppIntroSlide.BATTERY_OPTIMISATION,
                image = getDrawable(R.drawable.ic_battery_std_white_64dp),
                title = getString(R.string.showcase_disable_battery_optimisation_title),
                description = getString(R.string.showcase_disable_battery_optimisation_message_good),
                backgroundColor = getColor(R.color.slideBlue),

                buttonId1 = ID_BUTTON_DONT_KILL_MY_APP,
                buttonText1 = getString(R.string.showcase_disable_battery_optimisation_button_dont_kill_my_app),
            )
        }
    }

    private fun fingerprintGestureSupportSlide(areGesturesAvailable: Boolean?): AppIntroSlideUi {
        when (areGesturesAvailable) {
            true -> return AppIntroSlideUi(
                id = AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT,
                image = getDrawable(R.drawable.ic_baseline_check_64),
                title = getString(R.string.showcase_fingerprint_gesture_support_title_supported),
                description = getString(R.string.showcase_fingerprint_gesture_support_message_supported),
                backgroundColor = getColor(R.color.slideOrange),
            )

            false -> return AppIntroSlideUi(
                id = AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT,
                image = getDrawable(R.drawable.ic_baseline_cross_64),
                title = getString(R.string.showcase_fingerprint_gesture_support_title_not_supported),
                description = getString(R.string.showcase_fingerprint_gesture_support_message_not_supported),
                backgroundColor = getColor(R.color.slideOrange),
            )

            null -> return AppIntroSlideUi(
                id = AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT,
                image = getDrawable(R.drawable.ic_baseline_fingerprint_64),
                title = getString(R.string.showcase_fingerprint_gesture_support_title_supported_unknown),
                description = getString(R.string.showcase_fingerprint_gesture_support_message_supported_unknown),
                backgroundColor = getColor(R.color.slideOrange),

                buttonId1 = ID_BUTTON_ENABLE_ACCESSIBILITY_SERVICE,
                buttonText1 = getString(R.string.enable),
            )
        }
    }

    private fun requestShizukuPermissionSlide(isPermissionGranted: Boolean): AppIntroSlideUi {
        if (isPermissionGranted) {
            return AppIntroSlideUi(
                id = AppIntroSlide.GRANT_SHIZUKU_PERMISSION,
                image = getDrawable(R.drawable.ic_baseline_check_64),
                title = getString(R.string.showcase_grant_shizuku_permission_granted_title),
                description = getString(R.string.showcase_grant_shizuku_permission_granted_message),
                backgroundColor = getColor(R.color.slideBlue),
            )
        } else {
            return AppIntroSlideUi(
                id = AppIntroSlide.GRANT_SHIZUKU_PERMISSION,
                image = getDrawable(R.drawable.ic_outline_error_outline_64),
                title = getString(R.string.showcase_grant_shizuku_permission_denied_title),
                description = getString(R.string.showcase_grant_shizuku_permission_denied_message),
                backgroundColor = getColor(R.color.slideBlue),
                buttonId1 = ID_BUTTON_MORE_SHIZUKU_INFO,
                buttonText1 = getString(R.string.showcase_more_shizuku_info),
                buttonId2 = ID_BUTTON_REQUEST_SHIZUKU_PERMISSION,
                buttonText2 = getString(R.string.showcase_request_shizuku_permission),
            )
        }
    }

    private fun requestNotificationSlide(isPermissionGranted: Boolean): AppIntroSlideUi {
        if (isPermissionGranted) {
            return AppIntroSlideUi(
                id = AppIntroSlide.NOTIFICATION_PERMISSION,
                image = getDrawable(R.drawable.ic_baseline_check_64),
                title = getString(R.string.showcase_notification_permission_granted_title),
                description = getString(R.string.showcase_notification_permission_granted_message),
                backgroundColor = getColor(R.color.slidePurple),
            )
        } else {
            return AppIntroSlideUi(
                id = AppIntroSlide.NOTIFICATION_PERMISSION,
                image = getDrawable(R.drawable.ic_outline_error_outline_64),
                title = getString(R.string.showcase_notification_permission_denied_title),
                description = getString(R.string.showcase_notification_permission_denied_message),
                backgroundColor = getColor(R.color.slidePurple),
                buttonId1 = ID_BUTTON_REQUEST_NOTIFICATION_PERMISSION,
                buttonText1 = getString(R.string.showcase_notification_permission_button),
            )
        }
    }

    private fun contributingSlide() = AppIntroSlideUi(
        id = AppIntroSlide.CONTRIBUTING,
        image = getDrawable(R.drawable.ic_outline_feedback_64),
        title = getString(R.string.showcase_contributing_title),
        description = getString(R.string.showcase_contributing_description),
        backgroundColor = getColor(R.color.slideGreen),
    )

    private fun setupChosenDevicesAgainSlide() = AppIntroSlideUi(
        id = AppIntroSlide.SETUP_CHOSEN_DEVICES_AGAIN,
        image = getDrawable(R.drawable.ic_outline_devices_other_64),
        title = getString(R.string.showcase_setup_chosen_devices_again_title),
        description = getString(R.string.showcase_setup_chosen_devices_again_message),
        backgroundColor = getColor(R.color.slideBlue),
    )

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: AppIntroUseCase,
        private val slides: List<String>,
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppIntroViewModel(
            useCase,
            slides,
            resourceProvider,
        ) as T
    }
}
