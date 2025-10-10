package io.github.sds100.keymapper.base.onboarding

import androidx.datastore.preferences.core.Preferences
import io.github.sds100.keymapper.base.utils.VersionHelper
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.utils.PrefDelegate
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.leanback.LeanbackAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingUseCaseImpl @Inject constructor(
    private val settingsRepository: PreferenceRepository,
    private val fileAdapter: FileAdapter,
    private val leanbackAdapter: LeanbackAdapter,
    private val shizukuAdapter: ShizukuAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val keyMapRepository: KeyMapRepository,
    private val buildConfigProvider: BuildConfigProvider,
) : PreferenceRepository by settingsRepository,
    OnboardingUseCase {

    override var shownAppIntro by PrefDelegate(Keys.shownAppIntro, false)

    override val showWhatsNew = get(Keys.lastInstalledVersionCodeHomeScreen)
        .map { (it ?: -1) < buildConfigProvider.versionCode }

    override fun showedWhatsNew() {
        set(Keys.lastInstalledVersionCodeHomeScreen, buildConfigProvider.versionCode)
    }

    override fun getWhatsNewText(): String =
        with(fileAdapter.openAsset("whats-new.txt").bufferedReader()) {
            readText()
        }

    override var approvedFloatingButtonFeaturePrompt by PrefDelegate(
        Keys.approvedFloatingButtonFeaturePrompt,
        false,
    )

    /**
     * Show only when they *upgrade* to the new version and after they've
     * completed the app intro, which asks them whether they want to receive notifications.
     */
    override val showFloatingButtonFeatureNotification: Flow<Boolean> = combine(
        get(Keys.lastInstalledVersionCodeBackground).map { it ?: -1 },
        get(Keys.approvedFloatingButtonFeaturePrompt).map { it ?: false },
    ) { oldVersionCode, approvedPrompt ->
        oldVersionCode < VersionHelper.FLOATING_BUTTON_MIN_VERSION && !approvedPrompt
    }

    override fun showedFloatingButtonFeatureNotification() {
        set(Keys.lastInstalledVersionCodeBackground, buildConfigProvider.versionCode)
    }

    override fun isTvDevice(): Boolean = leanbackAdapter.isTvDevice()

    override val promptForShizukuPermission: Flow<Boolean> = combine(
        settingsRepository.get(Keys.shownShizukuPermissionPrompt),
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

    override val hasViewedAdvancedTriggers: Flow<Boolean> =
        get(Keys.viewedAdvancedTriggers).map { it ?: false }

    override fun viewedAdvancedTriggers() {
        set(Keys.viewedAdvancedTriggers, true)
    }

    override fun showTapTarget(tapTarget: OnboardingTapTarget): Flow<Boolean> {
        val shownKey = getTapTargetKey(tapTarget)

        return combine(
            settingsRepository.get(shownKey).map { it ?: false },
            settingsRepository.get(Keys.skipTapTargetTutorial).map { it ?: false },
            keyMapRepository.keyMapList.filterIsInstance<State.Data<List<KeyMapEntity>>>(),
        ) { isShown, skipTapTarget, keyMapList ->
            showTutorialTapTarget(tapTarget, isShown, skipTapTarget, keyMapList.data)
        }
    }

    override fun completedTapTarget(tapTarget: OnboardingTapTarget) {
        val key = getTapTargetKey(tapTarget)
        settingsRepository.set(key, true)
    }

    private fun getTapTargetKey(tapTarget: OnboardingTapTarget): Preferences.Key<Boolean> {
        val key = when (tapTarget) {
            OnboardingTapTarget.CREATE_KEY_MAP -> Keys.shownTapTargetCreateKeyMap
            OnboardingTapTarget.CHOOSE_ACTION -> Keys.shownTapTargetChooseAction
        }
        return key
    }

    /**
     * Whether to show a tutorial tap target. This will try to determine whether the user
     * has interacted with each feature before by checking the key maps they've created (if any).
     * E.g if they have no key maps with actions then show a tap target highlighting the action tab
     * when they create a key map.
     */
    private fun showTutorialTapTarget(
        tapTarget: OnboardingTapTarget,
        isShown: Boolean,
        skipTutorial: Boolean,
        keyMapList: List<KeyMapEntity>,
    ): Boolean {
        if (isShown) {
            return false
        }

        if (skipTutorial) {
            return false
        }

        return when (tapTarget) {
            OnboardingTapTarget.CREATE_KEY_MAP -> keyMapList.isEmpty()
            OnboardingTapTarget.CHOOSE_ACTION -> keyMapList.all { it.actionList.isEmpty() }
        }
    }

    override fun skipTapTargetOnboarding() {
        settingsRepository.set(Keys.skipTapTargetTutorial, true)
    }
}

interface OnboardingUseCase {
    var shownAppIntro: Boolean

    fun isTvDevice(): Boolean

    val showFloatingButtonFeatureNotification: Flow<Boolean>
    fun showedFloatingButtonFeatureNotification()
    var approvedFloatingButtonFeaturePrompt: Boolean

    val showWhatsNew: Flow<Boolean>
    fun showedWhatsNew()
    fun getWhatsNewText(): String

    val promptForShizukuPermission: Flow<Boolean>

    val showShizukuAppIntroSlide: Boolean

    val hasViewedAdvancedTriggers: Flow<Boolean>
    fun viewedAdvancedTriggers()

    fun showTapTarget(tapTarget: OnboardingTapTarget): Flow<Boolean>
    fun completedTapTarget(tapTarget: OnboardingTapTarget)
    fun skipTapTargetOnboarding()
}
