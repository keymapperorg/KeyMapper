package io.github.sds100.keymapper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.navigation.findNavController
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.extension.openOutputStream
import com.anggrayudi.storage.extension.toDocumentFile
import io.github.sds100.keymapper.Constants.PACKAGE_NAME
import io.github.sds100.keymapper.compose.ComposeColors
import io.github.sds100.keymapper.databinding.ActivityMainBinding
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerController
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.inputevents.MyMotionEvent
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Created by sds100 on 19/02/2020.
 */

abstract class BaseMainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG =
            "$PACKAGE_NAME.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG"

        const val ACTION_USE_FLOATING_BUTTONS =
            "$PACKAGE_NAME.ACTION_USE_FLOATING_BUTTONS"

        const val ACTION_SAVE_FILE = "$PACKAGE_NAME.ACTION_SAVE_FILE"
        const val EXTRA_FILE_URI = "$PACKAGE_NAME.EXTRA_FILE_URI"
    }

    private val permissionAdapter: AndroidPermissionAdapter by lazy {
        ServiceLocator.permissionAdapter(this)
    }

    val serviceAdapter: AccessibilityServiceAdapter by lazy {
        ServiceLocator.accessibilityServiceAdapter(this)
    }

    val viewModel by viewModels<ActivityViewModel> {
        ActivityViewModel.Factory(ServiceLocator.resourceProvider(this))
    }

    private val currentNightMode: Int
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate
    private val recordTriggerController: RecordTriggerController by lazy {
        (applicationContext as KeyMapperApp).recordTriggerController
    }

    private var originalFileUri: Uri? = null

    private val saveFileLauncher =
        registerForActivityResult(CreateDocument(FileUtils.MIME_TYPE_ALL)) { uri ->
            uri ?: return@registerForActivityResult

            originalFileUri?.let { original -> saveFile(originalFile = original, targetFile = uri) }
        }

    /**
     * This is used when saving a file with the Android share sheet and want to copy
     * the private to the public location.
     */
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                ACTION_SAVE_FILE -> {
                    lifecycleScope.launch {
                        withStateAtLeast(Lifecycle.State.RESUMED) {
                            selectFileLocationAndSave(intent)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                ComposeColors.surfaceContainerLight.toArgb(),
                ComposeColors.surfaceContainerDark.toArgb(),
            ),
            navigationBarStyle = SystemBarStyle.auto(
                ComposeColors.surfaceContainerLight.toArgb(),
                ComposeColors.surfaceContainerDark.toArgb(),
            ),
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

            when (intent?.action) {
                ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG -> {
                    viewModel.onCantFindAccessibilitySettings()
                    viewModel.handledActivityLaunchIntent = true
                }
            }
        }

        IntentFilter().apply {
            addAction(ACTION_SAVE_FILE)

            ContextCompat.registerReceiver(
                this@BaseMainActivity,
                broadcastReceiver,
                this,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    override fun onResume() {
        super.onResume()

        Timber.i("MainActivity: onResume. Version: ${Constants.VERSION}")

        // This must be after onResume to ensure all the fragment lifecycles' have also
        // resumed which are observing these events.
        // This is checked here and not in KeyMapperApp's lifecycle observer because
        // the activities have not necessarily resumed at that point.
        permissionAdapter.onPermissionsChanged()
        serviceAdapter.updateWhetherServiceIsEnabled()
    }

    override fun onDestroy() {
        UseCases.onboarding(this).shownAppIntro = true

        viewModel.previousNightMode = currentNightMode
        unregisterReceiver(broadcastReceiver)
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

    private fun saveFile(originalFile: Uri, targetFile: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            targetFile.openOutputStream(this@BaseMainActivity)?.use { output ->
                originalFile.openInputStream(this@BaseMainActivity)?.use { input ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun selectFileLocationAndSave(intent: Intent) {
        val fileUri =
            IntentCompat.getParcelableExtra(intent, EXTRA_FILE_URI, Uri::class.java) ?: return

        val fileName = fileUri.toDocumentFile(this@BaseMainActivity)?.name ?: return

        originalFileUri = fileUri
        saveFileLauncher.launch(fileName)
    }
}
