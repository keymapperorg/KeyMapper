package io.github.sds100.keymapper.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
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

        when {
            !globalPreferences.shownAppIntro.firstBlocking() ->
                startActivity(Intent(this, AppIntroActivity::class.java))

            !globalPreferences.approvedFingerprintFeaturePrompt.firstBlocking()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                startActivity(Intent(this, FingerprintGestureIntroActivity::class.java))

            else -> startActivity(Intent(this, HomeActivity::class.java))
        }

        finish()
    }
}