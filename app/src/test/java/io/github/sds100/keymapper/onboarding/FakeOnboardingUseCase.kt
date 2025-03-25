package io.github.sds100.keymapper.onboarding

import io.github.sds100.keymapper.actions.ActionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Created by sds100 on 26/04/2021.
 */
class FakeOnboardingUseCase : OnboardingUseCase {
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

    override var shownParallelTriggerOrderExplanation: Boolean = false
    override var shownSequenceTriggerExplanation: Boolean = false
    override val showAssistantTriggerFeatureNotification: Flow<Boolean> = MutableStateFlow(false)

    override fun showedAssistantTriggerFeatureNotification() {}

    override var approvedAssistantTriggerFeaturePrompt: Boolean = false

    override val showWhatsNew = MutableStateFlow(false)

    override fun showedWhatsNew() {}

    override fun getWhatsNewText(): String {
        throw NotImplementedError()
    }

    override val showQuickStartGuideHint = MutableStateFlow(false)

    override fun shownQuickStartGuideHint() {}

    override val promptForShizukuPermission: Flow<Boolean> = MutableStateFlow(false)

    override val showShizukuAppIntroSlide: Boolean = false

    override val showNoKeysDetectedBottomSheet: Flow<Boolean> = MutableStateFlow(false)

    override fun neverShowNoKeysRecordedBottomSheet() {}
    override val hasViewedAdvancedTriggers: Flow<Boolean> = MutableStateFlow(false)

    override fun viewedAdvancedTriggers() {}
}
