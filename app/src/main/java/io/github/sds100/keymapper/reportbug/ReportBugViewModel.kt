package io.github.sds100.keymapper.reportbug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.home.FixAppKillingSlide
import io.github.sds100.keymapper.onboarding.AppIntroSlideUi
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 14/02/2021.
 */

class ReportBugViewModel(
    private val useCase: ReportBugUseCase,
    private val controlService: ControlAccessibilityServiceUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    companion object {
        private const val ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE = "restart_accessibility_service"
        private const val ID_BUTTON_CREATE_GITHUB_ISSUE = "create_github_issue"
        private const val ID_BUTTON_DISCORD_SERVER = "discord_server"
        private const val ID_BUTTON_CREATE_BUG_REPORT = "create_bug_report"
    }

    private var bugReportUri: String? = null

    val slides: StateFlow<List<AppIntroSlideUi>> = MutableStateFlow(createSlides())

    private val _chooseBugReportLocation = MutableSharedFlow<Unit>()
    val chooseBugReportLocation = _chooseBugReportLocation.asSharedFlow()

    private val _goToNextSlide = MutableSharedFlow<Unit>()
    val goToNextSlide = _goToNextSlide.asSharedFlow()

    fun onButtonClick(id: String) {
        viewModelScope.launch {
            when (id) {
                ID_BUTTON_CREATE_BUG_REPORT -> _chooseBugReportLocation.emit(Unit)

                ID_BUTTON_CREATE_GITHUB_ISSUE -> showPopup(
                    "url_create_github_issue",
                    PopupUi.OpenUrl(getString(R.string.url_github_create_issue_bug))
                )

                ID_BUTTON_DISCORD_SERVER -> showPopup(
                    "url_discord_server",
                    PopupUi.OpenUrl(getString(R.string.url_discord_server_invite))
                )

                ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE -> {
                    if (!controlService.restartService()) {
                        ViewModelHelper.handleCantFindAccessibilitySettings(
                            resourceProvider = this@ReportBugViewModel,
                            popupViewModel = this@ReportBugViewModel
                        )
                    } else {
                        controlService.serviceState.first { it == ServiceState.ENABLED } //wait for it to be started
                        _goToNextSlide.emit(Unit)
                    }
                }
            }
        }
    }

    fun onChooseBugReportLocation(uri: String) {
        viewModelScope.launch {
            useCase.createBugReport(uri)
                .onSuccess {
                    bugReportUri = uri

                    _goToNextSlide.emit(Unit)
                }.onFailure { error ->
                    val dialog = PopupUi.Dialog(
                        title = getString(R.string.dialog_title_failed_to_create_bug_report),
                        message = error.getFullMessage(this@ReportBugViewModel),
                        positiveButtonText = getString(R.string.pos_ok)
                    )

                    showPopup("failed_to_create_report", dialog)
                }
        }
    }

    fun canGoToNextSlide(currentSlide: String): Boolean {
        return when (currentSlide) {
            ReportBugSlide.CREATE_BUG_REPORT -> bugReportUri != null
            else -> true
        }
    }

    private fun createBugReportSlide() = AppIntroSlideUi(
        id = ReportBugSlide.CREATE_BUG_REPORT,
        image = getDrawable(R.drawable.ic_outline_bug_report_64),
        backgroundColor = getColor(R.color.slideRed),
        title = getString(R.string.slide_title_create_bug_report),
        description = getString(R.string.slide_description_create_bug_report),
        buttonText1 = getString(R.string.slide_button_create_bug_report),
        buttonId1 = ID_BUTTON_CREATE_BUG_REPORT
    )

    private fun shareBugReportSlide() = AppIntroSlideUi(
        id = ReportBugSlide.SHARE_BUG_REPORT,
        image = getDrawable(R.drawable.ic_outline_share_64),
        backgroundColor = getColor(R.color.slideOrange),
        title = getString(R.string.slide_title_share_bug_report),
        description = getString(R.string.slide_description_share_bug_report),
        buttonText1 = getString(R.string.slide_button_share_discord),
        buttonId1 = ID_BUTTON_DISCORD_SERVER,
        buttonText2 = getString(R.string.slide_button_share_github),
        buttonId2 = ID_BUTTON_CREATE_GITHUB_ISSUE
    )

    private fun restartServiceSlide() = AppIntroSlideUi(
        id = FixAppKillingSlide.RESTART_ACCESSIBILITY_SERVICE,
        image = getDrawable(R.drawable.ic_outline_error_outline_64),
        backgroundColor = getColor(R.color.slideGreen),
        title = getString(R.string.slide_title_restart_accessibility_service),
        description = getString(R.string.slide_description_restart_accessibility_service),
        buttonText1 = getString(R.string.button_restart_accessibility_service),
        buttonId1 = ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE
    )

    private fun createSlides(): List<AppIntroSlideUi> = sequence {
        yield(createBugReportSlide())
        yield(shareBugReportSlide())

        if (controlService.serviceState.firstBlocking() == ServiceState.CRASHED) {
            yield(restartServiceSlide())
        }
    }.toList()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: ReportBugUseCase,
        private val controlAccessibilityService: ControlAccessibilityServiceUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReportBugViewModel(useCase, controlAccessibilityService, resourceProvider) as T
        }
    }
}