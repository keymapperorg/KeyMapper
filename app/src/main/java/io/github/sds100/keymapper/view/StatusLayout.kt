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
import kotlinx.android.synthetic.main.layout_service_status.view.*

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

    private var mFixedText: String? = null
    private var mErrorText: String? = null
    private var mYellowOnError: Boolean = false

    init {
        View.inflate(context, R.layout.layout_service_status, this)

        if (attrs != null) {
            mFixedText = str(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_fixedText
            )

            mErrorText = str(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_errorText
            )

            mYellowOnError = bool(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_yellowOnError
            )
        }

        if (mYellowOnError) {
            buttonFix.setBackgroundColor(color(R.color.warn))
        } else {
            buttonFix.setBackgroundColor(color(R.color.error))
        }

        //set to disabled state by default
        changeToErrorState()
    }

    fun changeToFixedState() {

        val drawable = drawable(R.drawable.ic_check_green_outline_24dp)

        textViewStatus.text = mFixedText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.visibility = View.GONE
    }

    fun changeToErrorState() {

        val drawable = if (mYellowOnError) {
            drawable(R.drawable.ic_warn_outline_yellow_24dp)
        } else {
            drawable(R.drawable.ic_error_outline_red_24dp)
        }

        textViewStatus.text = mErrorText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.visibility = View.VISIBLE
    }

    fun setOnFixClickListener(onClickListener: OnClickListener) {
        buttonFix.setOnClickListener(onClickListener)
    }
}