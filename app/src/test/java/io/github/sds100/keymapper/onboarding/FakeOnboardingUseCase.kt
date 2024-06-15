package io.github.sds100.keymapper.onboarding

import io.github.sds100.keymapper.actions.ActionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by sds100 on 26/04/2021.
 */
class FakeOnboardingUseCase : OnboardingUseCase {
    var approvedSetupChosenDevicesAgainNotification = false

    override var shownAppIntro: Boolean = false

    override suspend fun showInstallGuiKeyboardPrompt(action: ActionData): Boolean {
        throw NotImplementedError()
    }

    override suspend fun showInstallShizukuPrompt(action: ActionData): Boolean {
        throw NotImplementedError()
    }

    override fun isTvDevice(): Boolean {
        throw NotImplementedError()
    }

    override fun neverShowGuiKeyboardPromptsAgain() {}

    override var approvedFingerprintFeaturePrompt: Boolean = false
    override var shownParallelTriggerOrderExplanation: Boolean = false
    override var shownSequenceTriggerExplanation: Boolean = false
    override val showFingerprintFeatureNotificationIfAvailable = MutableStateFlow(false)

    override fun showedFingerprintFeatureNotificationIfAvailable() {}

    override val showSetupChosenDevicesAgainNotification = MutableStateFlow(false)

    override fun approvedSetupChosenDevicesAgainNotification() {
        approvedSetupChosenDevicesAgainNotification = true
    }

    override val showSetupChosenDevicesAgainAppIntro = MutableStateFlow(false)

    override fun approvedSetupChosenDevicesAgainAppIntro() {}

    override val showWhatsNew = MutableStateFlow(false)

    override fun showedWhatsNew() {}

    override fun getWhatsNewText(): String {
        throw NotImplementedError()
    }

    override val showQuickStartGuideHint = MutableStateFlow(false)

    override fun shownQuickStartGuideHint() {}

    override val promptForShizukuPermission: Flow<Boolean> = MutableStateFlow(false)

    override val showShizukuAppIntroSlide: Boolean = false
}
