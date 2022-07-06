package io.github.sds100.keymapper

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.onboarding.AppIntroActivity
import io.github.sds100.keymapper.onboarding.AppIntroSlide
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.util.firstBlocking
import javax.inject.Inject

/**
 * Created by sds100 on 20/01/21.
 */
@AndroidEntryPoint
class SplashActivity : FragmentActivity() {

    @Inject
    lateinit var systemFeatureAdapter: SystemFeatureAdapter
    
    @Inject
    lateinit var onboarding: OnboardingUseCase
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { true }

        super.onCreate(savedInstanceState)
        
        val appIntroSlides: List<String>

        if (!onboarding.shownAppIntro) {
            appIntroSlides = sequence {
                yield(AppIntroSlide.NOTE_FROM_DEV)

                yield(AppIntroSlide.ACCESSIBILITY_SERVICE)
                yield(AppIntroSlide.BATTERY_OPTIMISATION)

                if (systemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                    yield(AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT)
                }

                if (onboarding.showShizukuAppIntroSlide) {
                    yield(AppIntroSlide.GRANT_SHIZUKU_PERMISSION)
                }

                yield(AppIntroSlide.CONTRIBUTING)
            }.toList()
        } else {
            appIntroSlides = sequence {
                if (!onboarding.approvedFingerprintFeaturePrompt
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    && systemFeatureAdapter.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                ) {
                    yield(AppIntroSlide.FINGERPRINT_GESTURE_SUPPORT)
                }

                if (onboarding.showSetupChosenDevicesAgainAppIntro.firstBlocking()) {
                    yield(AppIntroSlide.SETUP_CHOSEN_DEVICES_AGAIN)
                }

                if (onboarding.promptForShizukuPermission.firstBlocking()) {
                    yield(AppIntroSlide.GRANT_SHIZUKU_PERMISSION)
                }
            }.toList()
        }

        if (appIntroSlides.isEmpty()) {
            val intentAction = this.intent.action

            Intent(this, MainActivity::class.java).apply {
                action = intentAction
                startActivity(this)
            }
        } else {
            Intent(this, AppIntroActivity::class.java).apply {
                val slidesToStringArray = appIntroSlides.map { it }.toTypedArray()

                putExtra(AppIntroActivity.EXTRA_SLIDES, slidesToStringArray)
                startActivity(this)
            }
        }

        finish()
    }
}