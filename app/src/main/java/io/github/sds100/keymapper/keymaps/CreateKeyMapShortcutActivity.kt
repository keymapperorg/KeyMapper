package io.github.sds100.keymapper.keymaps

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.compose.ComposeColors
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 08/09/20.
 */

class CreateKeyMapShortcutActivity : AppCompatActivity() {

    private val viewModel by viewModels<CreateKeyMapShortcutViewModel> {
        Inject.createActionShortcutViewModel(this)
    }

    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

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

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = true)

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            ServiceLocator.permissionAdapter(this@CreateKeyMapShortcutActivity).request
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
