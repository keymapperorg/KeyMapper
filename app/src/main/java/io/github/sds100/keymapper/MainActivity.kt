package io.github.sds100.keymapper

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.databinding.ActivityMainBinding
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import splitties.alertdialog.appcompat.*
import timber.log.Timber

/**
 * Created by sds100 on 19/02/2020.
 */

class MainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG =
                "$PACKAGE_NAME.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG"
    }

    private val viewModel by viewModels<ActivityViewModel> {
        ActivityViewModel.Factory(ServiceLocator.resourceProvider(this))
    }

    private val currentNightMode: Int
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.previousNightMode != currentNightMode) {
            ServiceLocator.resourceProvider(this).onThemeChange()
        }

        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        viewModel.showPopups(this, coordinatorLayout)

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = true)

        ServiceLocator.permissionAdapter(this@MainActivity).request
                .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
                .onEach { permission ->
                    requestPermissionDelegate.requestPermission(
                            permission,
                            findNavController(R.id.container)
                    )
                }
                .launchIn(lifecycleScope)

        if (intent.action == ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG) {
            viewModel.onCantFindAccessibilitySettings()
        }
    }

    override fun onResume() {
        super.onResume()

        Timber.i("MainActivity: onResume. Version: ${Constants.VERSION}")
    }

    override fun onDestroy() {
        viewModel.previousNightMode = currentNightMode
        super.onDestroy()
    }
}