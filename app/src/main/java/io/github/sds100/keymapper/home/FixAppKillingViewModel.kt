package io.github.sds100.keymapper.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.onboarding.AppIntroSlideUi
import io.github.sds100.keymapper.system.accessibility.ControlAccessibilityServiceUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceState
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 14/02/2021.
 */

class FixAppKillingViewModel(
    resourceProvider: ResourceProvider,
    private val controlAccessibilityService: ControlAccessibilityServiceUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    companion object {
        private const val ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE = "restart_accessibility_service"
        private const val ID_BUTTON_GO_TO_DONT_KILL_MY_APP = "go_to_dont_kill_my_app"
    }

    val allSlides = listOf(
        goToDontKillMyAppSlide(),
        restartServiceSlide(),
    )

    private val _goToNextSlide = MutableSharedFlow<Unit>()
    val goToNextSlide = _goToNextSlide.asSharedFlow()

    fun onButtonClick(id: String) {
        viewModelScope.launch {
            when (id) {
                ID_BUTTON_GO_TO_DONT_KILL_MY_APP -> {
                    showPopup(
                        "url_dont_kill_my_app",
                        PopupUi.OpenUrl(getString(R.string.url_dont_kill_my_app)),
                    )
                }

                ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE -> {
                    controlAccessibilityService.restartService()

                    controlAccessibilityService.serviceState.first { it == ServiceState.ENABLED } // wait for it to be started
                    _goToNextSlide.emit(Unit)
                }
            }
        }
    }

    fun getSlide(slide: String): AppIntroSlideUi = allSlides.find { it.id == slide }!!

    private fun goToDontKillMyAppSlide() = AppIntroSlideUi(
        id = FixAppKillingSlide.GO_TO_DONT_KILL_MY_APP,
        image = getDrawable(R.drawable.ic_baseline_cross_64),
        backgroundColor = getColor(R.color.slideRed),
        title = getString(R.string.slide_title_read_dont_kill_my_app),
        description = getString(R.string.slide_description_read_dont_kill_my_app),
        buttonText1 = getString(R.string.slide_button_read_dont_kill_my_app),
        buttonId1 = ID_BUTTON_GO_TO_DONT_KILL_MY_APP,

    )

    private fun restartServiceSlide() = AppIntroSlideUi(
        id = FixAppKillingSlide.RESTART_ACCESSIBILITY_SERVICE,
        image = getDrawable(R.drawable.ic_outline_error_outline_64),
        backgroundColor = getColor(R.color.slideOrange),
        title = getString(R.string.slide_title_restart_accessibility_service),
        description = getString(R.string.slide_description_restart_accessibility_service),
        buttonText1 = getString(R.string.button_restart_accessibility_service),
        buttonId1 = ID_BUTTON_RESTART_ACCESSIBILITY_SERVICE,
    )

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
        private val controlAccessibilityServiceUseCase: ControlAccessibilityServiceUseCase,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FixAppKillingViewModel(resourceProvider, controlAccessibilityServiceUseCase) as T
    }
}
