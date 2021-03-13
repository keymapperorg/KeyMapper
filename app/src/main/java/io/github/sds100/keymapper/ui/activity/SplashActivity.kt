package io.github.sds100.keymapper.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.approvedFingerprintFeaturePrompt
import io.github.sds100.keymapper.data.shownAppIntro
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 20/01/21.
 */
class SplashActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val slidesToShow = when {
            !globalPreferences.shownAppIntro.firstBlocking() ->
                arrayOf(
                    AppIntroActivity.SLIDE_NOTE_FROM_DEVELOPER,
                    AppIntroActivity.SLIDE_ACCESSIBILITY_SERVICE,
                    AppIntroActivity.SLIDE_BATTERY_OPTIMISATION,
                    AppIntroActivity.SLIDE_FINGERPRINT_GESTURE_SUPPORT,
                    AppIntroActivity.SLIDE_DEX,
                    AppIntroActivity.SLIDE_DO_NOT_DISTURB,
                    AppIntroActivity.SLIDE_CONTRIBUTING
                )

            else -> sequence {
                if (!globalPreferences.approvedFingerprintFeaturePrompt.firstBlocking()
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ) {
                    yield(AppIntroActivity.SLIDE_FINGERPRINT_GESTURE_SUPPORT)
                }

                val previouslyChoseBluetoothDevices =
                    globalPreferences.getFlow(Keys.bluetoothDevicesThatShowImePicker)
                        .firstBlocking()?.isNotEmpty() == true
                        || globalPreferences.getFlow(Keys.bluetoothDevicesThatToggleKeyboard)
                        .firstBlocking()?.isNotEmpty() == true

                if (globalPreferences.getFlow(Keys.approvedSetupChosenDevicesAgain)
                        .firstBlocking() == false
                    && previouslyChoseBluetoothDevices
                ) {
                    yield(AppIntroActivity.SLIDE_SETUP_CHOSEN_DEVICES_IN_SETTINGS_AGAIN)
                }
            }.toList().toTypedArray()
        }

        if (slidesToShow.isEmpty()) {
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            Intent(this, AppIntroActivity::class.java).apply {
                putExtra(AppIntroActivity.EXTRA_SLIDES, slidesToShow)
                startActivity(this)
            }
        }

        finish()
    }
}