package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.bool
import io.github.sds100.keymapper.util.drawable

/**
 * Created by sethsch1 on 28/12/17.
 */

class ExpandButton(
        context: Context,
        attrs: AttributeSet
) : AppCompatImageButton(context, attrs) {

    companion object {
        private val STATE_SET_DOWN = intArrayOf(-R.attr.state_up)
        private val STATE_SET_UP = intArrayOf(R.attr.state_up)
    }

    /**
     * Called when the button is pressed and is going to the expanded state.
     * @return whether the button should continue with expanding by changing the arrow direction.
     */
    var onExpandClick: () -> Boolean = { true }

    /**
     * Called when the button is pressed and is going to the collapsed state.
     * @return whether the button should continue with collapsing by changing the arrow direction.
     */
    var onCollapseClick: () -> Boolean = { true }

    var expanded = false
        set(value) {
            if (!value) {
                if (!onCollapseClick()) return
            } else {
                if (!onExpandClick()) return
            }

            field = value

            if (expanded) {
                setImageState(STATE_SET_UP, true)
            } else {
                setImageState(STATE_SET_DOWN, true)
            }
        }

    init {
        setImageDrawable(context.drawable(R.drawable.asl_expand))
        scaleType = ScaleType.FIT_CENTER

        expanded = context.bool(attrs, R.styleable.ExpandButton, R.styleable.ExpandButton_expanded, true)
    }

    override fun performClick(): Boolean {
        expanded = !expanded

        return super.performClick()
    }
}