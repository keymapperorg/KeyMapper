package io.github.sds100.keymapper.common.ui

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton


class SquareImageButton(context: Context, attrs: AttributeSet?) : AppCompatImageButton(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // have equal sides.
        super.onMeasure(heightMeasureSpec, heightMeasureSpec)
    }
}
