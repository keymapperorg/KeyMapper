package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.github.sds100.keymapper.R
import kotlinx.android.synthetic.main.expanded_status_layout.view.*

/**
 * Created by sds100 on 15/05/2019.
 */

class StatusLayoutList(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        View.inflate(context, R.layout.expanded_status_layout, this)
    }

    fun addStatusLayout(statusLayout: StatusLayout) {
        linearLayout.addView(statusLayout)
    }
}