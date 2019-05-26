package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
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

    lateinit var state: State

    private var mFixedText: String? = null
    private var mWarningText: String? = null
    private var mErrorText: String? = null
    private var mShowFixButton: Boolean = true

    init {
        View.inflate(context, R.layout.layout_status, this)

        if (attrs != null) {
            mFixedText = str(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_fixedText
            )

            mWarningText = str(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_warningText
            )

            mErrorText = str(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_errorText
            )

            mShowFixButton = bool(
                    attrs,
                    R.styleable.StatusLayout,
                    R.styleable.StatusLayout_showFixButton,
                    defaultValue = true
            )
        }

        if (mErrorText.isNullOrEmpty()) {
            changeToWarningState()
        } else {
            changeToErrorState()
        }
    }

    fun changeToFixedState() {

        val drawable = drawable(R.drawable.ic_check_green_outline_24dp)

        textViewStatus.text = mFixedText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.visibility = View.GONE
        state = State.FIXED
    }

    fun changeToWarningState() {

        val drawable = drawable(R.drawable.ic_warn_outline_yellow_24dp)

        textViewStatus.text = mWarningText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.isVisible = mShowFixButton
        buttonFix.setBackgroundColor(color(R.color.warn))
        state = State.WARN
    }

    fun changeToErrorState() {

        val drawable = drawable(R.drawable.ic_error_outline_red_24dp)

        textViewStatus.text = mErrorText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.isVisible = mShowFixButton
        buttonFix.setBackgroundColor(color(R.color.error))
        state = State.ERROR
    }

    fun setOnFixClickListener(onClickListener: OnClickListener) {
        buttonFix.setOnClickListener(onClickListener)
    }

    enum class State {
        FIXED, WARN, ERROR
    }
}