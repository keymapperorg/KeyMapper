package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.bool
import io.github.sds100.keymapper.util.color
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.str
import kotlinx.android.synthetic.main.layout_status.view.*

/**
 * Created by sds100 on 15/11/2018.
 */
class StatusLayout(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    var fixedText: String? = null
    var errorText: String? = null
    var yellowOnError: Boolean = false

    init {
        View.inflate(context, R.layout.layout_status, this)

        if (attrs != null) {
            fixedText = str(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_fixedText
            )

            errorText = str(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_errorText
            )

            yellowOnError = bool(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_yellowOnError
            )
        }

        if (yellowOnError) {
            buttonFix.setBackgroundColor(color(R.color.warn))
        } else {
            buttonFix.setBackgroundColor(color(R.color.error))
        }

        //set to disabled state by default
        changeToErrorState()
    }

    fun changeToFixedState() {

        val drawable = drawable(R.drawable.ic_check_green_outline_24dp)

        textViewStatus.text = fixedText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.visibility = View.GONE
    }

    fun changeToErrorState() {

        val drawable = if (yellowOnError) {
            drawable(R.drawable.ic_warn_outline_yellow_24dp)
        } else {
            drawable(R.drawable.ic_error_outline_red_24dp)
        }

        textViewStatus.text = errorText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.visibility = View.VISIBLE
    }

    fun setOnFixClickListener(onClickListener: OnClickListener) {
        buttonFix.setOnClickListener(onClickListener)
    }
}