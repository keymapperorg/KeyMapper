package io.github.sds100.keymapper.system.apps

import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.os.BundleCompat

/**
 * Use an activity trampoline so that the keyguard is dismissed when launching activities from
 * the lock screen.
 */
class TrampolineActivity : ComponentActivity() {
    companion object {
        const val EXTRA_INTENT = "io.github.sds100.keymapper.EXTRA_INTENT"
    }

    private val keyguardManager: KeyguardManager by lazy {
        getSystemService(KeyguardManager::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        keyguardManager.requestDismissKeyguard(this, null)

        val intentExtras = intent?.extras ?: return finish()

        val activityIntent: Intent? =
            BundleCompat.getParcelable(intentExtras, EXTRA_INTENT, Intent::class.java)

        if (activityIntent != null) {
            val pendingIntent =
                PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val bundle = ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                    )
                    .toBundle()

                pendingIntent.send(bundle)
            } else {
                pendingIntent.send()
            }
        }

        finish()
    }
}