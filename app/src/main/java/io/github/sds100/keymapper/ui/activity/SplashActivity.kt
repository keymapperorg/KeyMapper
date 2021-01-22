package io.github.sds100.keymapper.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.PreferenceKeys
import io.github.sds100.keymapper.data.appIntro
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 20/01/21.
 */
class SplashActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val globalPreferences = ServiceLocator.globalPreferences(this)

        when {
            runBlocking { !globalPreferences.appIntro().first() } ->
                startActivity(Intent(this, AppIntroActivity::class.java))

            runBlocking { globalPreferences.get(PreferenceKeys.approvedFingerprintFeaturePrompt) == false }
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ->
                startActivity(Intent(this, FingerprintGestureIntroActivity::class.java))

            else -> startActivity(Intent(this, HomeActivity::class.java))
        }

        finish()
    }
}