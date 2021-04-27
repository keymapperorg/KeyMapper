package io.github.sds100.keymapper

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.github.sds100.keymapper.onboarding.AppIntroActivity
import io.github.sds100.keymapper.onboarding.AppIntroSlide
import io.github.sds100.keymapper.util.firstBlocking

/**
 * Created by sds100 on 20/01/21.
 */
class SplashActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onboarding = UseCases.onboarding(this)

        val appIntroSlides: List<AppIntroSlide>

        if (!onboarding.shownAppIntro) {
            appIntroSlides = listOf(
                AppIntroSlide.NOTE_FROM_DEV,
                AppIntroSlide.ACCESSIBILITY_SERVICE,
                AppIntroSlide.BATTERY_OPTIMISATION,
                AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT,
                AppIntroSlide.DO_NOT_DISTURB,
                AppIntroSlide.CONTRIBUTING,
            )
        } else {
            appIntroSlides = sequence {
                if (!onboarding.approvedFingerprintFeaturePrompt
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ) {
                    yield(AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT)
                }

                if (onboarding.showSetupChosenDevicesAgainAppIntro.firstBlocking()) {
                    yield(AppIntroSlide.SETUP_CHOSEN_DEVICES_AGAIN)
                }
            }.toList()
        }

        if (appIntroSlides.isEmpty()) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            Intent(this, AppIntroActivity::class.java).apply {
                val slidesToStringArray = appIntroSlides.map { it.toString() }.toTypedArray()

                putExtra(AppIntroActivity.EXTRA_SLIDES, slidesToStringArray)
                startActivity(this)
            }
        }

        finish()
    }
}