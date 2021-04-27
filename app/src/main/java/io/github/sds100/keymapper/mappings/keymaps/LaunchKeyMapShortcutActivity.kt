package io.github.sds100.keymapper.mappings.keymaps

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.system.accessibility.MyAccessibilityService
import splitties.toast.toast

/**
 * Created by sds100 on 08/09/20.
 */
class LaunchKeyMapShortcutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ServiceLocator.serviceAdapter(this).isEnabled.value) {
            if (intent.action == MyAccessibilityService.ACTION_TRIGGER_KEYMAP_BY_UID) {
                Intent(MyAccessibilityService.ACTION_TRIGGER_KEYMAP_BY_UID).apply {
                    setPackage(Constants.PACKAGE_NAME)

                    val uuid = intent.getStringExtra(MyAccessibilityService.EXTRA_KEYMAP_UID)
                    putExtra(MyAccessibilityService.EXTRA_KEYMAP_UID, uuid)

                    sendBroadcast(this)
                }
            }
        } else {
            toast(R.string.error_accessibility_service_disabled)
        }

        finish()
    }
}