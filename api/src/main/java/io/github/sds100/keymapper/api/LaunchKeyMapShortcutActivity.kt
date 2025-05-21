package io.github.sds100.keymapper.api

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import javax.inject.Inject

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
/**
 * Use basic Activity, NOT AppCompatActivity so the NoDisplay theme works. Otherwise an
 * exception may be thrown because the theme doesn't extend AppCompat.
 */

// TODO test this still works.
class LaunchKeyMapShortcutActivity : ComponentActivity() {

    @Inject
    lateinit var accessibilityServiceAdapter: AccessibilityServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val accessibilityServiceState = accessibilityServiceAdapter.state.value

        when (accessibilityServiceState) {
            AccessibilityServiceState.ENABLED ->
                if (intent.action == Api.ACTION_TRIGGER_KEYMAP_BY_UID) {
                    Intent(Api.ACTION_TRIGGER_KEYMAP_BY_UID).apply {
                        setPackage(packageName)

                        val uuid = intent.getStringExtra(Api.EXTRA_KEYMAP_UID)
                        putExtra(Api.EXTRA_KEYMAP_UID, uuid)

                        sendBroadcast(this)
                    }
                }

            AccessibilityServiceState.CRASHED -> Toast.makeText(
                this,
                R.string.error_accessibility_service_crashed,
                Toast.LENGTH_SHORT,
            ).show()

            AccessibilityServiceState.DISABLED -> Toast.makeText(
                this,
                R.string.error_accessibility_service_disabled,
                Toast.LENGTH_SHORT,
            ).show()
        }

        finish()
    }
}
