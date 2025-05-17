package io.github.sds100.keymapper.base.api

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.system.accessibility.ServiceState
import splitties.toast.toast

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
/**
 * Use basic Activity, NOT AppCompatActivity so the NoDisplay theme works. Otherwise an
 * exception may be thrown because the theme doesn't extend AppCompat.
 */
class LaunchKeyMapShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accessibilityServiceState = ServiceLocator.accessibilityServiceAdapter(this).state.value

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
