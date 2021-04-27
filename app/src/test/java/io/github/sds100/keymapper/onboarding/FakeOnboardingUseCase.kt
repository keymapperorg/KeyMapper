package io.github.sds100.keymapper.onboarding

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by sds100 on 26/04/2021.
 */
class FakeOnboardingUseCase : OnboardingUseCase {
    var approvedSetupChosenDevicesAgainNotification = false

    override var shownAppIntro: Boolean = false

    override val showGuiKeyboardAdFlow = MutableStateFlow(false)

    override fun shownGuiKeyboardAd() {}

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

    override val showQuickStartGuideHint = MutableStateFlow(false)

    override fun shownQuickStartGuideHint() {}
}