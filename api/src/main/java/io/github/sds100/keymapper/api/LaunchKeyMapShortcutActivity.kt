package io.github.sds100.keymapper.api

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.system.service.ServiceAdapter
import io.github.sds100.keymapper.system.accessibility.ServiceState
import javax.inject.Inject

// DON'T MOVE THIS CLASS TO A DIFFERENT PACKAGE BECAUSE IT BREAKS THE API
/**
 * Use basic Activity, NOT AppCompatActivity so the NoDisplay theme works. Otherwise an
 * exception may be thrown because the theme doesn't extend AppCompat.
 */
@AndroidEntryPoint
class LaunchKeyMapShortcutActivity : Activity() {

    @Inject
    private lateinit var buildConfigProvider: BuildConfigProvider

    @Inject
    private lateinit var accessibilityServiceAdapter: ServiceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accessibilityServiceState = accessibilityServiceAdapter.state.value

        when (accessibilityServiceState) {
            ServiceState.ENABLED ->
                if (intent.action == Api.ACTION_TRIGGER_KEYMAP_BY_UID) {
                    Intent(Api.ACTION_TRIGGER_KEYMAP_BY_UID).apply {
                        setPackage(buildConfigProvider.packageName)

                        val uuid = intent.getStringExtra(Api.EXTRA_KEYMAP_UID)
                        putExtra(Api.EXTRA_KEYMAP_UID, uuid)

                        sendBroadcast(this)
                    }
                }

            ServiceState.CRASHED -> Toast.makeText(
                this,
                R.string.error_accessibility_service_crashed,
                Toast.LENGTH_SHORT,
            )

            ServiceState.DISABLED -> Toast.makeText(
                this,
                R.string.error_accessibility_service_disabled,
                Toast.LENGTH_SHORT,
            )
        }

        finish()
    }
}
