package io.github.sds100.keymapper.Selection

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.AttrUtils

/**
 * A [StateListDrawable] which changes color when selected.
 */
class SelectableBackground(ctx: Context) : StateListDrawable() {
    init {
        addState(intArrayOf(android.R.attr.state_selected),
                ColorDrawable(ContextCompat.getColor(ctx, R.color.background_selected)))

        addState(intArrayOf(), AttrUtils.getSelectableItemBackground(ctx))
    }
}