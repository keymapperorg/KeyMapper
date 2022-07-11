package io.github.sds100.keymapper.api

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.ServiceState
import splitties.toast.toast
import javax.inject.Inject

/**
 * Created by sds100 on 08/09/20.
 */

//DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
@AndroidEntryPoint
class LaunchKeyMapShortcutActivity : AppCompatActivity() {
    
    @Inject
    lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accessibilityServiceState = accessibilityServiceAdapter.state.value

        when (accessibilityServiceState) {
            ServiceState.ENABLED ->
                if (intent.action == Api.ACTION_TRIGGER_KEYMAP_BY_UID) {
                    Intent(Api.ACTION_TRIGGER_KEYMAP_BY_UID).apply {
                        setPackage(Constants.PACKAGE_NAME)

                        val uuid = intent.getStringExtra(Api.EXTRA_KEYMAP_UID)
                        putExtra(Api.EXTRA_KEYMAP_UID, uuid)

                        sendBroadcast(this)
                    }
                }

            ServiceState.CRASHED -> toast(R.string.error_accessibility_service_crashed)
            ServiceState.DISABLED -> toast(R.string.error_accessibility_service_disabled)
        }

        finish()
    }
}