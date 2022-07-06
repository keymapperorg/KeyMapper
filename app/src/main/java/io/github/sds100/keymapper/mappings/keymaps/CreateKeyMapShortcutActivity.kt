package io.github.sds100.keymapper.mappings.keymaps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.ActivityCreateKeymapShortcutBinding
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapterImpl
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

/**
 * Created by sds100 on 08/09/20.
 */

@AndroidEntryPoint
class CreateKeyMapShortcutActivity : AppCompatActivity() {
    private lateinit var requestPermissionDelegate: RequestPermissionDelegate
    
    @Inject
    lateinit var permissionAdapter: AndroidPermissionAdapter

    @Inject
    lateinit var notificationReceiverAdapter: NotificationReceiverAdapterImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityCreateKeymapShortcutBinding>(
            this,
            R.layout.activity_create_keymap_shortcut
        )

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = true,permissionAdapter, notificationReceiverAdapter)

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            permissionAdapter.request
                .collectLatest { permission ->
                    requestPermissionDelegate.requestPermission(
                        permission,
                        findNavController(R.id.container)
                    )
                }
        }
    }
}