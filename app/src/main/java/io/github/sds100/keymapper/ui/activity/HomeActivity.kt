package io.github.sds100.keymapper.ui.activity

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.WidgetsManager
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.viewmodel.KeyActionTypeViewModel
import io.github.sds100.keymapper.databinding.ActivityHomeBinding
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.NotificationUtils
import io.github.sds100.keymapper.util.PermissionUtils
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource

/**
 * Created by sds100 on 19/02/2020.
 */

class HomeActivity : AppCompatActivity() {

    companion object {
        const val KEY_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG =
            "$PACKAGE_NAME.show_accessibility_settings_not_found_dialog"
    }

    private val mKeyActionTypeViewModel: KeyActionTypeViewModel by viewModels {
        InjectorUtils.provideKeyActionTypeViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkThemeMode = AppPreferences.darkThemeMode
        AppCompatDelegate.setDefaultNightMode(darkThemeMode)

        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)

        if (intent.getBooleanExtra(KEY_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG, false)) {
            alertDialog {
                titleResource = R.string.dialog_title_cant_find_accessibility_settings_page
                messageResource = R.string.dialog_message_cant_find_accessibility_settings_page

                okButton {
                    PermissionUtils.requestWriteSecureSettingsPermission(this@HomeActivity)
                }

                show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        WidgetsManager.invalidateNotifications(this)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        mKeyActionTypeViewModel.keyEvent.value = event

        return super.onKeyUp(keyCode, event)
    }
}