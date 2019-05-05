package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.github.sds100.keymapper.ActionDescription
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.drawable
import kotlinx.android.synthetic.main.layout_action_description.view.*

/**
 * Created by sds100 on 15/10/2018.
 */

class ActionDescriptionLayout(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        View.inflate(context, R.layout.layout_action_description, this)

        /* on pre-lollipop devices, vector drawables can't be used with drawableStart,
         * drawableEnd etc. otherwise the app crashes. */
        val errorDrawable = drawable(R.drawable.ic_error_outline_red_24dp)

        textViewError.setCompoundDrawablesWithIntrinsicBounds(
                errorDrawable,
                null,
                null,
                null
        )
    }

    fun setDescription(description: ActionDescription) {
        description.apply {
            textViewTitle.text = title
            imageViewAction.setImageDrawable(iconDrawable)
            textViewError.text = errorDescription

            textViewError.setVisible(errorResult != null)
            imageViewAction.setVisible(iconDrawable != null)
            textViewTitle.setVisible(title != null)
        }
    }

    private fun View.setVisible(visible: Boolean) {
        if (visible) {
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }
}