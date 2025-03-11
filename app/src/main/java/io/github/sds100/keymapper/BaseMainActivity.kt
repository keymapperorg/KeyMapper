package io.github.sds100.keymapper

import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.compose.ComposeColors
import io.github.sds100.keymapper.databinding.ActivityMainBinding
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerController
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Created by sds100 on 19/02/2020.
 */

abstract class BaseMainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG =
            "$PACKAGE_NAME.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG"

        const val ACTION_USE_ASSISTANT_TRIGGER =
            "$PACKAGE_NAME.ACTION_USE_ASSISTANT_TRIGGER"
    }

    private val viewModel by viewModels<ActivityViewModel> {
        ActivityViewModel.Factory(ServiceLocator.resourceProvider(this))
    }

    private val currentNightMode: Int
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate
    private val recordTriggerController: RecordTriggerController by lazy {
        (applicationContext as KeyMapperApp).recordTriggerController
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                ComposeColors.surfaceContainerLight.toArgb(),
                ComposeColors.surfaceContainerDark.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                ComposeColors.surfaceContainerLight.toArgb(),
                ComposeColors.surfaceContainerDark.toArgb(),
            )
        )
        super.onCreate(savedInstanceState)

        if (viewModel.previousNightMode != currentNightMode) {
            ServiceLocator.resourceProvider(this).onThemeChange()
        }

        val binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        viewModel.showPopups(this, binding.coordinatorLayout)

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = true)

        ServiceLocator.permissionAdapter(this@BaseMainActivity).request
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { permission ->
                requestPermissionDelegate.requestPermission(
                    permission,
                    findNavController(R.id.container),
                )
            }
            .launchIn(lifecycleScope)

        // Must launch when the activity is resumed
        // so the nav controller can be found
        launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (viewModel.handledActivityLaunchIntent) {
                return@launchRepeatOnLifecycle
            }

            when (intent.action) {
                ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG -> {
                    viewModel.onCantFindAccessibilitySettings()
                }

                ACTION_USE_ASSISTANT_TRIGGER -> {
                    findNavController(R.id.container).navigate(
                        NavAppDirections.actionToConfigKeymap(
                            keymapUid = null,
                            showAdvancedTriggers = true,
                        ),
                    )
                }
            }

            viewModel.handledActivityLaunchIntent = true
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

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event ?: return super.onGenericMotionEvent(event)

        val consume =
            recordTriggerController.onActivityMotionEvent(MyMotionEvent.fromMotionEvent(event))

        return if (consume) {
            true
        } else {
            // IMPORTANT! return super so that the back navigation button still works.
            super.onGenericMotionEvent(event)
        }
    }
}
