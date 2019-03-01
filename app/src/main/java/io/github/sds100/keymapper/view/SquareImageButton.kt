package io.github.sds100.keymapper.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageButton

/**
 * Created by sds100 on 16/07/2018.
 */
class SquareImageButton(context: Context?, attrs: AttributeSet?) : AppCompatImageButton(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //have equal sides.
        super.onMeasure(heightMeasureSpec, heightMeasureSpec)
    }
}