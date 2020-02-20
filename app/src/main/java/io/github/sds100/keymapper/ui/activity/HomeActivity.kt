package io.github.sds100.keymapper.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.databinding.ActivityHomeBinding
import splitties.experimental.ExperimentalSplittiesApi

/**
 * Created by sds100 on 19/02/2020.
 */

@ExperimentalSplittiesApi
class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)

        lifecycleScope.launchWhenCreated {
            val darkThemeMode = AppPreferences().darkThemeMode

            runOnUiThread {
                AppCompatDelegate.setDefaultNightMode(darkThemeMode)
            }
        }
    }
}