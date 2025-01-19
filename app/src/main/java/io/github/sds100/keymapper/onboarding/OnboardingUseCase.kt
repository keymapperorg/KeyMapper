package io.github.sds100.keymapper.onboarding

import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.canUseImeToPerform
import io.github.sds100.keymapper.actions.canUseShizukuToPerform
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.shizuku.ShizukuUtils
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.leanback.LeanbackAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.util.PrefDelegate
import io.github.sds100.keymapper.util.VersionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 14/02/21.
 */
class OnboardingUseCaseImpl(
    private val preferences: PreferenceRepository,
    private val fileAdapter: FileAdapter,
    private val leanbackAdapter: LeanbackAdapter,
    private val shizukuAdapter: ShizukuAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val packageManagerAdapter: PackageManagerAdapter,
) : PreferenceRepository by preferences,
    OnboardingUseCase {

    override var shownAppIntro by PrefDelegate(Keys.shownAppIntro, false)

    override suspend fun showInstallGuiKeyboardPrompt(action: ActionData): Boolean {
        val acknowledged = preferences.get(Keys.acknowledgedGuiKeyboard).first()
        val isGuiKeyboardInstalled =
            packageManagerAdapter.isAppInstalled(KeyMapperImeHelper.KEY_MAPPER_GUI_IME_PACKAGE)

        val isShizukuInstalled = shizukuAdapter.isInstalled.value

        return (acknowledged == null || !acknowledged) &&
            !isGuiKeyboardInstalled &&
            !isShizukuInstalled &&
            action.canUseImeToPerform()
    }

    override suspend fun showInstallShizukuPrompt(action: ActionData): Boolean = !shizukuAdapter.isInstalled.value &&
        ShizukuUtils.isRecommendedForSdkVersion() &&
        action.canUseShizukuToPerform()

    override fun neverShowGuiKeyboardPromptsAgain() {
        preferences.set(Keys.acknowledgedGuiKeyboard, true)
    }

    override var shownParallelTriggerOrderExplanation by PrefDelegate(
        Keys.shownParallelTriggerOrderExplanation,
        false,
    )
    override var shownSequenceTriggerExplanation by PrefDelegate(
        Keys.shownSequenceTriggerExplanation,
        false,
    )

    override val showWhatsNew = get(Keys.lastInstalledVersionCodeHomeScreen)
        .map { (it ?: -1) < Constants.VERSION_CODE }

    override fun showedWhatsNew() {
        set(Keys.lastInstalledVersionCodeHomeScreen, Constants.VERSION_CODE)
    }

    override fun getWhatsNewText(): String = with(fileAdapter.openAsset("whats-new.txt").bufferedReader()) {
        readText()
    }

    override var approvedAssistantTriggerFeaturePrompt by PrefDelegate(
        Keys.approvedAssistantTriggerFeaturePrompt,
        false,
    )

    /**
     * Show the assistant trigger only when they *upgrade* to the new version and after they've
     * completed the app intro, which asks them whether they want to receive notifications.
     */
    override val showAssistantTriggerFeatureNotification: Flow<Boolean> =
        combine(
            get(Keys.lastInstalledVersionCodeBackground).map { it ?: -1 },
            get(Keys.shownAppIntro).map { it ?: false },
            get(Keys.approvedAssistantTriggerFeaturePrompt).map { it ?: false },
        ) { oldVersionCode, shownAppIntro, approvedPrompt ->
            oldVersionCode < VersionHelper.ASSISTANT_TRIGGER_MIN_VERSION &&
                shownAppIntro &&
                !approvedPrompt
        }

    override fun showedAssistantTriggerFeatureNotification() {
        set(Keys.lastInstalledVersionCodeBackground, Constants.VERSION_CODE)
    }

    override val showQuickStartGuideHint: Flow<Boolean> = get(Keys.shownQuickStartGuideHint).map {
        if (it == null) {
            true
        } else {
            !it
        }
    }

    override fun shownQuickStartGuideHint() {
        preferences.set(Keys.shownQuickStartGuideHint, true)
    }

    override fun isTvDevice(): Boolean = leanbackAdapter.isTvDevice()

    override val promptForShizukuPermission: Flow<Boolean> = combine(
        preferences.get(Keys.shownShizukuPermissionPrompt),
        shizukuAdapter.isInstalled,
        permissionAdapter.isGrantedFlow(Permission.SHIZUKU),
    ) {
            shownPromptBefore,
            isShizkuInstalled,
            isShizukuPermissionGranted,
        ->
        shownPromptBefore != true && isShizkuInstalled && !isShizukuPermissionGranted
    }

    override val showShizukuAppIntroSlide: Boolean
        get() = shizukuAdapter.isInstalled.value

    override val showNoKeysDetectedBottomSheet: Flow<Boolean> =
        preferences.get(Keys.neverShowNoKeysRecordedError).map { neverShow ->
            if (neverShow == null) {
                true
            } else {
                !neverShow
            }
        }

    override fun neverShowNoKeysRecordedBottomSheet() {
        preferences.set(Keys.neverShowNoKeysRecordedError, true)
    }
}

interface OnboardingUseCase {
    var shownAppIntro: Boolean

    /**
     * @return whether to prompt the user to install the Key Mapper GUI Keyboard after adding
     * this action
     */
    suspend fun showInstallGuiKeyboardPrompt(action: ActionData): Boolean

    /**
     * @return whether to prompt the user to install Shizuku after adding
     * this action
     */
    suspend fun showInstallShizukuPrompt(action: ActionData): Boolean

    fun isTvDevice(): Boolean
    fun neverShowGuiKeyboardPromptsAgain()

    var shownParallelTriggerOrderExplanation: Boolean
    var shownSequenceTriggerExplanation: Boolean

    val showAssistantTriggerFeatureNotification: Flow<Boolean>
    fun showedAssistantTriggerFeatureNotification()
    var approvedAssistantTriggerFeaturePrompt: Boolean

    val showWhatsNew: Flow<Boolean>
    fun showedWhatsNew()
    fun getWhatsNewText(): String

    val showQuickStartGuideHint: Flow<Boolean>
    fun shownQuickStartGuideHint()

    val promptForShizukuPermission: Flow<Boolean>

    val showShizukuAppIntroSlide: Boolean

    val showNoKeysDetectedBottomSheet: Flow<Boolean>
    fun neverShowNoKeysRecordedBottomSheet()
}
