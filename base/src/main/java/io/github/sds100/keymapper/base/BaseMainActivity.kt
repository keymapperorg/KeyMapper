package io.github.sds100.keymapper.base

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStateAtLeast
import androidx.navigation.findNavController
import com.anggrayudi.storage.extension.openInputStream
import com.anggrayudi.storage.extension.openOutputStream
import com.anggrayudi.storage.extension.toDocumentFile
import io.github.sds100.keymapper.base.compose.ComposeColors
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.base.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.base.trigger.RecordTriggerControllerImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProviderImpl
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.sysbridge.service.SystemBridgeSetupController
import io.github.sds100.keymapper.system.devices.AndroidDevicesAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.inputevents.KMGamePadEvent
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapterImpl
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapterImpl
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

abstract class BaseMainActivity : AppCompatActivity() {

    companion object {
        const val ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG =
            "${BuildConfig.LIBRARY_PACKAGE_NAME}.ACTION_SHOW_ACCESSIBILITY_SETTINGS_NOT_FOUND_DIALOG"

        const val ACTION_USE_FLOATING_BUTTONS =
            "${BuildConfig.LIBRARY_PACKAGE_NAME}.ACTION_USE_FLOATING_BUTTONS"

        const val ACTION_SAVE_FILE = "${BuildConfig.LIBRARY_PACKAGE_NAME}.ACTION_SAVE_FILE"
        const val EXTRA_FILE_URI = "${BuildConfig.LIBRARY_PACKAGE_NAME}.EXTRA_FILE_URI"
    }

    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    lateinit var serviceAdapter: AccessibilityServiceAdapterImpl

    @Inject
    lateinit var resourceProvider: ResourceProviderImpl

    @Inject
    lateinit var onboardingUseCase: OnboardingUseCase

    @Inject
    lateinit var recordTriggerController: RecordTriggerControllerImpl

    @Inject
    lateinit var notificationReceiverAdapter: NotificationReceiverAdapterImpl

    @Inject
    lateinit var shizukuAdapter: ShizukuAdapter

    @Inject
    lateinit var buildConfigProvider: BuildConfigProvider

    @Inject
    lateinit var privServiceSetup: SystemBridgeSetupController

    @Inject
    lateinit var suAdapter: SuAdapterImpl

    @Inject
    lateinit var devicesAdapter: AndroidDevicesAdapter

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    private val currentNightMode: Int
        get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    val viewModel by viewModels<ActivityViewModel>()

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
            resourceProvider.onThemeChange()
        }

        requestPermissionDelegate = RequestPermissionDelegate(
            this,
            showDialogs = true,
            permissionAdapter,
            notificationReceiverAdapter = notificationReceiverAdapter,
            buildConfigProvider = buildConfigProvider,
            shizukuAdapter = shizukuAdapter,
        )

        permissionAdapter.request
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

        Timber.i("MainActivity: onResume. Version: ${buildConfigProvider.version} ${buildConfigProvider.versionCode}")

        // This must be after onResume to ensure all the fragment lifecycles' have also
        // resumed which are observing these events.
        // This is checked here and not in KeyMapperApp's lifecycle observer because
        // the activities have not necessarily resumed at that point.
        permissionAdapter.onPermissionsChanged()
        serviceAdapter.invalidateState()
        suAdapter.invalidateIsRooted()
    }

    override fun onDestroy() {
        onboardingUseCase.shownAppIntro = true

        viewModel.previousNightMode = currentNightMode
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        event ?: return super.onGenericMotionEvent(event)

        // TODO send this to inputeventhub
        val gamepadEvent = KMGamePadEvent.fromMotionEvent(event) ?: return false
        val consume = recordTriggerController.onActivityMotionEvent(gamepadEvent)

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
