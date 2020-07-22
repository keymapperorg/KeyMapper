package io.github.sds100.keymapper.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.github.sds100.keymapper.R

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

    init {
        View.inflate(context, R.layout.list_item_status, this)
    }

    enum class State {
        POSITIVE, WARN, ERROR
    }
}