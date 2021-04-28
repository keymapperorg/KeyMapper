package io.github.sds100.keymapper.onboarding

import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.FlowPrefDelegate
import io.github.sds100.keymapper.util.PrefDelegate
import io.github.sds100.keymapper.util.VersionUtils
import kotlinx.coroutines.flow.*

/**
 * Created by sds100 on 14/02/21.
 */
class OnboardingUseCaseImpl(
    private val preferenceRepository: PreferenceRepository,
    private val fileAdapter: FileAdapter
) : PreferenceRepository by preferenceRepository, OnboardingUseCase {

    override var shownAppIntro by PrefDelegate(Keys.shownAppIntro, false)

    override val showGuiKeyboardAdFlow by FlowPrefDelegate(Keys.showGuiKeyboardAd, true)
    override fun shownGuiKeyboardAd() {
        preferenceRepository.set(Keys.showGuiKeyboardAd, false)
    }

    override var approvedFingerprintFeaturePrompt by PrefDelegate(
        Keys.approvedFingerprintFeaturePrompt,
        false
    )

    override var shownParallelTriggerOrderExplanation by PrefDelegate(
        Keys.shownParallelTriggerOrderExplanation,
        false
    )
    override var shownSequenceTriggerExplanation by PrefDelegate(
        Keys.shownSequenceTriggerExplanation,
        false
    )

    override val showWhatsNew = get(Keys.lastInstalledVersionCodeHomeScreen)
        .map { it ?: -1 < Constants.VERSION_CODE }

    override fun showedWhatsNew() {
        set(Keys.lastInstalledVersionCodeHomeScreen, Constants.VERSION_CODE)
    }

    override fun getWhatsNewText(): String {
        return with(fileAdapter.openAsset("whats-new.txt").bufferedReader()) {
            readText()
        }
    }

    override val showFingerprintFeatureNotificationIfAvailable: Flow<Boolean> by lazy {
        combine(
            get(Keys.lastInstalledVersionCodeBackground).map { it ?: -1 },
            showWhatsNew
        ) { oldVersionCode, showWhatsNew ->
            //has the user opened the app and will have already seen that they can remap fingerprint gestures
            val handledUpdateInHomeScreen = !showWhatsNew

            oldVersionCode < VersionUtils.FINGERPRINT_GESTURES_MIN_VERSION && !handledUpdateInHomeScreen
        }
    }

    override fun showedFingerprintFeatureNotificationIfAvailable() {
        set(Keys.lastInstalledVersionCodeBackground, Constants.VERSION_CODE)
    }

    override val showSetupChosenDevicesAgainNotification: Flow<Boolean> =
        get(Keys.approvedSetupChosenDevicesAgain).map { it ?: false }.map { approvedPreviously ->
            val bluetoothDevicesThatShowImePicker =
                get(stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker")).first()
                    ?: emptySet()

            val bluetoothDevicesThatChangeIme =
                get(stringSetPreferencesKey("pref_bluetooth_devices")).first() ?: emptySet()

            val previouslyChoseBluetoothDevices =
                bluetoothDevicesThatShowImePicker.isNotEmpty() || bluetoothDevicesThatChangeIme.isNotEmpty()

            return@map !approvedPreviously && previouslyChoseBluetoothDevices
        }

    override fun approvedSetupChosenDevicesAgainNotification() {
        set(Keys.approvedSetupChosenDevicesAgain, true)
    }

    override val showSetupChosenDevicesAgainAppIntro: Flow<Boolean> =
        get(Keys.approvedSetupChosenDevicesAgain).map { it ?: false }.map { approvedPreviously ->

            val bluetoothDevicesThatShowImePicker =
                get(stringSetPreferencesKey("pref_bluetooth_devices_show_ime_picker")).first()
                    ?: emptySet()

            val bluetoothDevicesThatChangeIme =
                get(stringSetPreferencesKey("pref_bluetooth_devices")).first()
                    ?: emptySet()

            val previouslyChoseBluetoothDevices =
                bluetoothDevicesThatShowImePicker.isNotEmpty() || bluetoothDevicesThatChangeIme.isNotEmpty()

            return@map !approvedPreviously && previouslyChoseBluetoothDevices
        }

    override fun approvedSetupChosenDevicesAgainAppIntro() {
        set(Keys.approvedSetupChosenDevicesAgain, true)
    }

    override val showQuickStartGuideHint: Flow<Boolean> = get(Keys.shownQuickStartGuideHint).map {
        if (it == null) {
            true
        } else {
            !it
        }
    }

    override fun shownQuickStartGuideHint() {
        preferenceRepository.set(Keys.shownQuickStartGuideHint, true)
    }
}

interface OnboardingUseCase {
    var shownAppIntro: Boolean

    val showGuiKeyboardAdFlow: Flow<Boolean>
    fun shownGuiKeyboardAd()

    var approvedFingerprintFeaturePrompt: Boolean
    var shownParallelTriggerOrderExplanation: Boolean
    var shownSequenceTriggerExplanation: Boolean

    val showFingerprintFeatureNotificationIfAvailable: Flow<Boolean>
    fun showedFingerprintFeatureNotificationIfAvailable()

    val showSetupChosenDevicesAgainNotification: Flow<Boolean>
    fun approvedSetupChosenDevicesAgainNotification()

    val showSetupChosenDevicesAgainAppIntro: Flow<Boolean>
    fun approvedSetupChosenDevicesAgainAppIntro()

    val showWhatsNew: Flow<Boolean>
    fun showedWhatsNew()
    fun getWhatsNewText(): String

    val showQuickStartGuideHint: Flow<Boolean>
    fun shownQuickStartGuideHint()
}