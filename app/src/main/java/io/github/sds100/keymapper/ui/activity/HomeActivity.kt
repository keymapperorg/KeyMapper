package io.github.sds100.keymapper.ui.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.gson.Gson
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.KeyActionTypeViewModel
import io.github.sds100.keymapper.databinding.ActivityHomeBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.FileAccessDenied
import io.github.sds100.keymapper.util.result.GenericFailure
import io.github.sds100.keymapper.util.result.onFailure
import io.github.sds100.keymapper.util.result.onSuccess
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.titleResource
import splitties.snackbar.snack
import timber.log.Timber

/**
 * Created by sds100 on 19/02/2020.
 */

class HomeActivity : AppCompatActivity() {

    companion object {
        const val KEY_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG =
            "$PACKAGE_NAME.show_accessibility_settings_not_found_dialog"
    }

    private val keyActionTypeViewModel: KeyActionTypeViewModel by viewModels {
        InjectorUtils.provideKeyActionTypeViewModel()
    }

    private val backupRestoreViewModel: BackupRestoreViewModel by viewModels {
        InjectorUtils.provideBackupRestoreViewModel(this)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private val requestBackupObserver = Observer<RequestBackup> { event ->
        if (!BackupUtils.backupAutomatically) return@Observer

        BackupUtils.createAutomaticBackupOutputStream(this@HomeActivity)
            .onSuccess {
                lifecycleScope.launch {
                    backupRestoreViewModel.backupKeymaps(it, event.keymapList)
                }
            }.onFailure {
                if (it is FileAccessDenied) {
                    coordinatorLayout.snack(R.string.error_file_access_denied_automatic_backup).apply {
                        setAction(R.string.reset) {
                            container.findNavController().navigate(R.id.action_global_settingsFragment)
                        }

                        show()
                    }
                }

                if (it is GenericFailure) {
                    Timber.e(it.exception)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)

        if (intent.getBooleanExtra(KEY_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG, false)) {
            alertDialog {
                titleResource = R.string.dialog_title_cant_find_accessibility_settings_page
                messageResource = R.string.dialog_message_cant_find_accessibility_settings_page

                okButton {
                    PermissionUtils.requestWriteSecureSettingsPermission(
                        this@HomeActivity, findNavController(R.id.container))
                }

                show()
            }
        }

        ServiceLocator.keymapRepository(this).apply {
            keymapList.observe(this@HomeActivity, {

                sendPackageBroadcast(
                    MyAccessibilityService.ACTION_UPDATE_KEYMAP_LIST_CACHE,
                    bundleOf(
                        MyAccessibilityService.EXTRA_KEYMAP_LIST to Gson().toJson(it)
                    ))
            })

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                requestBackup.observe(this@HomeActivity, requestBackupObserver)
            }
        }

        if (BuildConfig.DEBUG && PermissionUtils.isPermissionGranted(Manifest.permission.WRITE_SECURE_SETTINGS)) {
            AccessibilityUtils.enableService(this)
        }

        if (!AppPreferences.shownAppIntro) {
            startActivity(Intent(this, AppIntroActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()

        NotificationController.invalidateNotifications(this)
    }

    override fun onDestroy() {
        ServiceLocator.release()

        super.onDestroy()
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        keyActionTypeViewModel.keyEvent.value = event

        return super.onKeyUp(keyCode, event)
    }
}