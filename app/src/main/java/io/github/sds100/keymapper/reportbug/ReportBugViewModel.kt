package io.github.sds100.keymapper.reportbug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.home.FixAppKillingSlide
import io.github.sds100.keymapper.onboarding.AppIntroSlideUi
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.system.share.EmailModel
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
    private val controlAccessibilityService: ControlAccessibilityServiceUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    companion object {
        private const val ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE = "restart_accessibility_service"
        private const val ID_BUTTON_CREATE_GITHUB_ISSUE = "create_github_issue"
        private const val ID_BUTTON_DISCORD_SERVER = "discord_server"
        private const val ID_BUTTON_EMAIL = "email"
        private const val ID_BUTTON_CREATE_BUG_REPORT = "create_bug_report"
    }

    private var bugReportUri: String? = null

    private val slideModels: List<AppIntroSlideUi> = sequence {
        yield(createBugReportSlide())
        yield(shareBugReportSlide())

        if (controlAccessibilityService.state.firstBlocking() == ServiceState.CRASHED) {
            yield(restartServiceSlide())
        }
    }.toList()

    val slides: List<String> = slideModels.map { it.id }

    private val _chooseBugReportLocation = MutableSharedFlow<Unit>()
    val chooseBugReportLocation = _chooseBugReportLocation.asSharedFlow()

    private val _goToNextSlide = MutableSharedFlow<Unit>()
    val goToNextSlide = _goToNextSlide.asSharedFlow()

    private val _openUrl = MutableSharedFlow<String>()
    val openUrl = _openUrl.asSharedFlow()

    private val _emailDeveloper = MutableSharedFlow<EmailModel>()

    /**
     * The uri of the bug report.
     */
    val emailDeveloper = _emailDeveloper.asSharedFlow()

    fun onButtonClick(id: String) {
        viewModelScope.launch {
            when (id) {
                ID_BUTTON_CREATE_BUG_REPORT -> _chooseBugReportLocation.emit(Unit)
                ID_BUTTON_CREATE_GITHUB_ISSUE -> _openUrl.emit(getString(R.string.url_github_create_issue_bug))
                ID_BUTTON_DISCORD_SERVER -> _openUrl.emit(getString(R.string.url_discord_server_invite))
                ID_BUTTON_EMAIL -> {
                    val dialog = PopupUi.Text(
                        hint = getString(R.string.hint_bug_report_description_email),
                        allowEmpty = false,
                        message = getString(R.string.dialog_message_bug_report_description_email)
                    )

                    val bugDescription = showPopup("get_bug_description", dialog) ?: return@launch

                    _emailDeveloper.emit(EmailModel(message = bugDescription, attachmentUri = bugReportUri))
                }
                ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE -> {
                    controlAccessibilityService.restart()

                    controlAccessibilityService.state.first { it == ServiceState.ENABLED } //wait for it to be started
                    _goToNextSlide.emit(Unit)
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

    fun getSlide(slide: String): Flow<AppIntroSlideUi> = flow { emit(slideModels.single { it.id == slide }) }

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
        buttonId2 = ID_BUTTON_CREATE_GITHUB_ISSUE,
        buttonText3 = getString(R.string.slide_button_share_email),
        buttonId3 = ID_BUTTON_EMAIL
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

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val useCase: ReportBugUseCase,
        private val controlAccessibilityService: ControlAccessibilityServiceUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ReportBugViewModel(useCase, controlAccessibilityService, resourceProvider) as T
        }
    }
}