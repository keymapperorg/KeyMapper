package io.github.sds100.keymapper.mappings.keymaps

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.databinding.ActivityCreateKeymapShortcutBinding
import io.github.sds100.keymapper.system.permissions.RequestPermissionDelegate
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 08/09/20.
 */

class CreateKeyMapShortcutActivity : AppCompatActivity() {
    private lateinit var requestPermissionDelegate: RequestPermissionDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityCreateKeymapShortcutBinding>(
            this,
            R.layout.activity_create_keymap_shortcut
        )

        requestPermissionDelegate = RequestPermissionDelegate(this, showDialogs = true)

        launchRepeatOnLifecycle(Lifecycle.State.STARTED) {
            ServiceLocator.permissionAdapter(this@CreateKeyMapShortcutActivity).request
                .collectLatest { permission ->
                    requestPermissionDelegate.requestPermission(
                        permission,
                        findNavController(R.id.container)
                    )
                }
        }
    }
}