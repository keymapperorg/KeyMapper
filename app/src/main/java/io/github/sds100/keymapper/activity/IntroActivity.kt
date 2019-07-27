package io.github.sds100.keymapper.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.edit
import com.heinrichreimersoftware.materialintro.app.IntroActivity
import com.heinrichreimersoftware.materialintro.slide.SimpleSlide
import com.heinrichreimersoftware.materialintro.slide.Slide
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.DexUtils.isDexSupported
import io.github.sds100.keymapper.util.FirebaseUtils
import io.github.sds100.keymapper.util.str
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 07/07/2019.
 */

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
        }.build()
    }

    private val mDataCollectionSlide by lazy {
        SimpleSlide.Builder().apply {
            title(R.string.showcase_data_collection_title)
            description(R.string.showcase_data_collection_message)
            background(R.color.green)
            backgroundDark(R.color.greenDark)
            image(R.drawable.ic_bug_white_64dp)
            canGoBackward(true)

            buttonCtaLabel(R.string.pos_opt_in)
            buttonCtaClickListener {
                defaultSharedPreferences.edit {
                    putBoolean(str(R.string.key_pref_data_collection), true)
                }

                FirebaseUtils.setFirebaseDataCollection(this@IntroActivity)

                nextSlide()
                removeSlide(this)
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

        addSlide(mDataCollectionSlide)
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
    }
}