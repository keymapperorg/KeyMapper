package io.github.sds100.keymapper.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 07/07/2019.
 */

class IntroActivity : IntroActivity() {

    companion object {
        private const val BATTERY_OPTIMISATION_SLIDE_POSITION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addSlide(SimpleSlide.Builder().apply {
            title(R.string.showcase_note_from_the_developer)
            description(R.string.showcase_note_from_the_developer_message)
            background(R.color.red)
            backgroundDark(R.color.redDark)
            image(R.mipmap.ic_launcher_round)
            scrollable(false)
            canGoBackward(true)
            isSkipEnabled = false
        }.build())

        val powerManager = (getSystemService(Context.POWER_SERVICE)) as PowerManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)) {

            addSlide(SimpleSlide.Builder().apply {
                title(R.string.showcase_disable_battery_optimisation)
                description(R.string.showcase_disable_battery_optimisation_message)
                background(R.color.blue)
                backgroundDark(R.color.blueDark)
                image(R.drawable.ic_battery_std_white_64dp)
                scrollable(false)
                canGoBackward(true)
                isSkipEnabled = false

                buttonCtaLabel(R.string.showcase_disable_battery_optimisation_button)
                buttonCtaClickListener {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            }.build())
        }
    }

    override fun onResume() {
        super.onResume()

        val powerManager = (getSystemService(Context.POWER_SERVICE)) as PowerManager

        /* when the user returns back from changing battery optimisation settings, go to the next page if they
            have turned it off */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME) &&
                currentSlidePosition == BATTERY_OPTIMISATION_SLIDE_POSITION) {
            nextSlide()
        }
    }
}