package io.github.sds100.keymapper.util

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.globalPreferences
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.PromptFocal
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.CirclePromptFocal

/**
 * Created by sds100 on 17/01/21.
 */

sealed class TapTarget(
    private val key: Preferences.Key<Boolean>,
    @StringRes val primaryText: Int,
    @StringRes val secondaryText: Int
) {

    /**
     * Only works on app bar items if called during or after
     * the resumed state in the view lifecycle.
     */
    suspend fun show(
        fragment: Fragment,
        @IdRes viewId: Int,
        promptFocal: PromptFocal = CirclePromptFocal()
    ) {
        val prefs = fragment.requireContext().globalPreferences

        if (prefs.get(key) == true) return

        MaterialTapTargetPrompt.Builder(fragment).apply {
            setTarget(viewId)

            focalColour = fragment.color(android.R.color.transparent)
            setPrimaryText(this@TapTarget.primaryText)
            setSecondaryText(this@TapTarget.secondaryText)
            backgroundColour = fragment.color(R.color.colorAccent)
            this.promptFocal = promptFocal

            setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED
                    || state == MaterialTapTargetPrompt.STATE_FINISHED) {

                    prefs.set(key, true)
                }
            }

            show()
        }
    }
}

class QuickStartGuideTapTarget : TapTarget(
    preferencesKey("tap_target_quick_start_guide"),
    R.string.tap_target_quick_start_guide_primary,
    R.string.tap_target_quick_start_guide_secondary
)
