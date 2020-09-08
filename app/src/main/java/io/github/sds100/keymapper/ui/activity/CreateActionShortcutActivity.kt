package io.github.sds100.keymapper.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.databinding.ActivityActionShortcutBinding

/**
 * Created by sds100 on 08/09/20.
 */
class CreateActionShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkThemeMode = AppPreferences.darkThemeMode
        AppCompatDelegate.setDefaultNightMode(darkThemeMode)

        DataBindingUtil.setContentView<ActivityActionShortcutBinding>(this, R.layout.activity_action_shortcut)
    }
}