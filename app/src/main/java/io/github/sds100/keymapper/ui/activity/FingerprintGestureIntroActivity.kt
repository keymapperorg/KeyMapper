package io.github.sds100.keymapper.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.github.appintro.AppIntro2
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.PreferenceKeys
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 07/07/2019.
 */

@RequiresApi(Build.VERSION_CODES.O)
class FingerprintGestureIntroActivity : AppIntro2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isSkipButtonEnabled = false

        addSlide(FingerprintGestureSupportSlide())
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)

        runBlocking {
            ServiceLocator.globalPreferences(this@FingerprintGestureIntroActivity)
                .set(PreferenceKeys.approvedFingerprintFeaturePrompt, true)
        }

        startActivity(Intent(this, HomeActivity::class.java))

        finish()
    }
}