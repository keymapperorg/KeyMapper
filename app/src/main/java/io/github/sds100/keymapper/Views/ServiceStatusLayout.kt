package io.github.sds100.keymapper.Views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Utils.AttrUtils.getCustomStringAttrValue
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
            mEnabledText = getCustomStringAttrValue(
                    context,
                    attrs,
                    R.styleable.ServiceStatusLayout,
                    R.styleable.ServiceStatusLayout_enabledText
            )!!

            mDisabledText = getCustomStringAttrValue(
                    context,
                    attrs,
                    R.styleable.ServiceStatusLayout,
                    R.styleable.ServiceStatusLayout_disabledText
            )!!
        }

        //set to disabled state by default
        changeToServiceDisabledState()
    }

    fun changeToServiceEnabledState() {

        /* on pre-lollipop devices, vector drawables can't be used with drawableStart,
         * drawableEnd etc. otherwise the app crashes. */
        val drawable = AppCompatResources.getDrawable(context, R.drawable.check_circle_green)

        textViewStatus.text = mEnabledText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.visibility = View.GONE
    }

    fun changeToServiceDisabledState() {

        /* on pre-lollipop devices, vector drawables can't be used with drawableStart,
         * drawableEnd etc. otherwise the app crashes. */
        val drawable = AppCompatResources.getDrawable(context, R.drawable.close_circle_red)

        textViewStatus.text = mDisabledText
        textViewStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        buttonFix.visibility = View.VISIBLE
    }

    fun setOnFixClickListener(onClickListener: OnClickListener) {
        buttonFix.setOnClickListener(onClickListener)
    }
}