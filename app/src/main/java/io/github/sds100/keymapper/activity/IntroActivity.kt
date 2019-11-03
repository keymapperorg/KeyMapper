package io.github.sds100.keymapper.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.heinrichreimersoftware.materialintro.slide.Slide
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.DexUtils.isDexSupported
import io.github.sds100.keymapper.util.PermissionUtils
import io.github.sds100.keymapper.util.isPermissionGranted

/**
 * Created by sds100 on 07/07/2019.
 */

@RequiresApi(Build.VERSION_CODES.M)
class IntroActivity : IntroActivity() {

    companion object {
        const val REQUEST_CODE_INTRO = 123
    }

    private val mNoteFromDevSlide by lazy {
        SimpleSlide.Builder().apply {
            title(R.string.showcase_note_from_the_developer_title)
            description(R.string.showcase_note_from_the_developer_message)
            background(R.color.red)
            backgroundDark(R.color.redDark)
            image(R.mipmap.ic_launcher_round)
            canGoBackward(true)
            scrollable(true)
        }.build()
    }

    private val mBatteryOptimisationSlide by lazy {
        SimpleSlide.Builder().apply {
            title(R.string.showcase_disable_battery_optimisation_title)
            description(R.string.showcase_disable_battery_optimisation_message)
            background(R.color.blue)
            backgroundDark(R.color.blueDark)
            image(R.drawable.ic_battery_std_white_64dp)
            canGoBackward(true)
            scrollable(true)

            buttonCtaLabel(R.string.showcase_disable_battery_optimisation_button)
            buttonCtaClickListener {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }.build()
    }

    private val mDexSlide by lazy {
        SimpleSlide.Builder().apply {
            title(R.string.showcase_dex_mode_supported_title)
            description(R.string.showcase_dex_mode_supported_message)
            background(R.color.orange)
            backgroundDark(R.color.orangeDark)
            image(R.drawable.ic_dock_white_64dp)
            canGoBackward(true)
            scrollable(true)
        }.build()
    }

    private val mDndAccessSlide: Slide by lazy {
        SimpleSlide.Builder().apply {
            title(R.string.showcase_dnd_access_title)
            description(R.string.showcase_dnd_access_description)
            background(R.color.red)
            backgroundDark(R.color.redDark)
            image(R.drawable.ic_do_not_disturb_white_64dp)
            canGoBackward(true)
            scrollable(true)

            buttonCtaLabel(R.string.pos_grant)
            buttonCtaClickListener {
                PermissionUtils.requestPermission(this@IntroActivity, Manifest.permission.ACCESS_NOTIFICATION_POLICY)
            }
        }.build()
    }

    private val currentSlide: Slide
        get() = getSlide(currentSlidePosition)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSkipEnabled = false

        addSlide(mNoteFromDevSlide)

        val powerManager = (getSystemService(Context.POWER_SERVICE)) as PowerManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && !powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)) {
            addSlide(mBatteryOptimisationSlide)
        }

        if (isDexSupported()) {
            addSlide(mDexSlide)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addSlide(mDndAccessSlide)
        }
    }

    override fun onResume() {
        super.onResume()

        val powerManager = (getSystemService(Context.POWER_SERVICE)) as PowerManager

        /* when the user returns back from changing battery optimisation settings, go to the next page if they
            have turned it off */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            powerManager.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME) &&
            currentSlide == mBatteryOptimisationSlide) {
            nextSlide()
            removeSlide(mBatteryOptimisationSlide)
        }

        if (isPermissionGranted(Manifest.permission.ACCESS_NOTIFICATION_POLICY) && currentSlide == mDndAccessSlide) {
            nextSlide()
            removeSlide(mDndAccessSlide)
        }
    }
}