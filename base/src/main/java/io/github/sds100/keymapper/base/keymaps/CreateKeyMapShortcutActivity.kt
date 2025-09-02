package io.github.sds100.keymapper.base.keymaps

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.ComposeColors
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.system.accessibility.AccessibilityServiceAdapterImpl
import io.github.sds100.keymapper.base.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.base.trigger.RecordTriggerController
import io.github.sds100.keymapper.base.utils.ui.ResourceProviderImpl
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapterImpl
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class CreateKeyMapShortcutActivity : AppCompatActivity() {

    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    lateinit var serviceAdapter: AccessibilityServiceAdapterImpl

    @Inject
    lateinit var resourceProvider: ResourceProviderImpl

    @Inject
    lateinit var onboardingUseCase: OnboardingUseCase

    @Inject
    lateinit var recordTriggerController: RecordTriggerController

    @Inject
    lateinit var notificationReceiverAdapter: NotificationReceiverAdapterImpl

    @Inject
    lateinit var shizukuAdapter: ShizukuAdapter

    @Inject
    lateinit var buildConfigProvider: BuildConfigProvider

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    private val viewModel by viewModels<CreateKeyMapShortcutViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
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

        setContent {
            KeyMapperTheme {
                CreateKeyMapShortcutScreen(
                    viewModel = viewModel,
                    finishActivity = { finish() },
                )
            }
        }

        requestPermissionDelegate = RequestPermissionDelegate(
            this,
            showDialogs = true,
            permissionAdapter,
            notificationReceiverAdapter = notificationReceiverAdapter,
            buildConfigProvider = buildConfigProvider,
            shizukuAdapter = shizukuAdapter,
        )

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            permissionAdapter.request
                .collectLatest { permission ->
                    requestPermissionDelegate.requestPermission(
                        permission,
                        findNavController(R.id.container),
                    )
                }
        }

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.returnIntentResult.collectLatest { intent ->
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }
}
