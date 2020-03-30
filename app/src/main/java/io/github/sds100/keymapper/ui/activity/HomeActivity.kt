package io.github.sds100.keymapper.ui.activity

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.viewmodel.KeyActionTypeViewModel
import io.github.sds100.keymapper.databinding.ActivityHomeBinding
import io.github.sds100.keymapper.util.InjectorUtils
import splitties.experimental.ExperimentalSplittiesApi

/**
 * Created by sds100 on 19/02/2020.
 */

@ExperimentalSplittiesApi
class HomeActivity : AppCompatActivity() {

    private val mKeyActionTypeViewModel: KeyActionTypeViewModel by viewModels {
        InjectorUtils.provideKeyActionTypeViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            val darkThemeMode = AppPreferences().darkThemeMode

            runOnUiThread {
                AppCompatDelegate.setDefaultNightMode(darkThemeMode)
            }
        }

        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        mKeyActionTypeViewModel.keyEvent.value = event

        return super.onKeyUp(keyCode, event)
    }
}