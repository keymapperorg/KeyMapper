package io.github.sds100.keymapper.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import io.github.sds100.keymapper.data.AppPreferences

/**
 * Created by sds100 on 19/02/2020.
 */

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkThemeMode = AppPreferences.darkThemeMode
        AppCompatDelegate.setDefaultNightMode(darkThemeMode)

        if (AppPreferences.shownAppIntro) {
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            startActivity(Intent(this, AppIntroActivity::class.java))
        }

        finish()
    }
}