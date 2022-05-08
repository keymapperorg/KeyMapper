package io.github.sds100.keymapper.util

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import io.github.sds100.keymapper.R
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt
import uk.co.samuelwall.materialtaptargetprompt.extras.PromptFocal
import uk.co.samuelwall.materialtaptargetprompt.extras.focals.CirclePromptFocal

/**
 * Created by sds100 on 17/01/21.
 */

sealed class TapTarget(
    @StringRes val primaryText: Int,
    @StringRes val secondaryText: Int
) {

    /**
     * Only works on app bar items if called during or after
     * the resumed state in the view lifecycle.
     */
    fun show(
        fragment: Fragment,
        @IdRes viewId: Int,
        promptFocal: PromptFocal = CirclePromptFocal(),
        onSuccessfulFinish: () -> Unit
    ): MaterialTapTargetPrompt? {
        return MaterialTapTargetPrompt.Builder(fragment).apply {
            setTarget(viewId)

            focalColour = fragment.color(android.R.color.transparent)
            setPrimaryText(this@TapTarget.primaryText)
            setSecondaryText(this@TapTarget.secondaryText)
            backgroundColour = fragment.styledColor(R.attr.colorPrimary)
            this.promptFocal = promptFocal

            setPromptStateChangeListener { _, state ->
                if (state == MaterialTapTargetPrompt.STATE_DISMISSED
                    || state == MaterialTapTargetPrompt.STATE_FINISHED
                ) {
                    onSuccessfulFinish.invoke()
                }
            }
        }.show()
    }
}

class QuickStartGuideTapTarget : TapTarget(
    R.string.tap_target_quick_start_guide_primary,
    R.string.tap_target_quick_start_guide_secondary
)
