package io.github.sds100.keymapper.ui.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.darkThemeMode
import io.github.sds100.keymapper.databinding.ActivityCreateKeymapShortcutBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 08/09/20.
 */
class CreateKeymapShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        runBlocking {
            ServiceLocator.globalPreferences(this@CreateKeymapShortcutActivity)
                .darkThemeMode()
                .first()
                .let {
                    AppCompatDelegate.setDefaultNightMode(it)
                }
        }

        DataBindingUtil.setContentView<ActivityCreateKeymapShortcutBinding>(this, R.layout.activity_create_keymap_shortcut)
    }
}