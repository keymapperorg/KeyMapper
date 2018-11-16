package io.github.sds100.keymapper.Views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.layout_service_status.view.*

/**
 * Created by sds100 on 15/11/2018.
 */
class ServiceStatusLayout(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    private var mEnabledText: String? = null
    private var mDisabledText: String? = null

    init {
        View.inflate(context, R.layout.layout_service_status, this)

        if (attrs != null) {
            mEnabledText = getCustomStringAttrValue(attrs, R.styleable.ServiceStatusLayout_enabledText)!!
            mDisabledText = getCustomStringAttrValue(attrs, R.styleable.ServiceStatusLayout_disabledText)!!
        }

        //set to disabled state by default
        changeToServiceDisabledState()
    }

    fun changeToServiceEnabledState() {
        textViewStatus.text = mEnabledText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.check_circle_green, 0, 0, 0)

        buttonFix.visibility = View.GONE
    }

    fun changeToServiceDisabledState() {
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.close_circle_red, 0, 0, 0)
        textViewStatus.text = mDisabledText

        buttonFix.visibility = View.VISIBLE
    }

    private fun getCustomStringAttrValue(attrs: AttributeSet, attrId: Int): String? {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ServiceStatusLayout, 0, 0)

        val attrValue: String?

        try {
            attrValue = typedArray.getString(attrId)
        } finally {
            typedArray.recycle()
        }

        return attrValue
    }
}