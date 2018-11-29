package io.github.sds100.keymapper

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

/**
 * Created by sds100 on 26/11/2018.
 */

class CustomViewPager(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {
    var isPagingEnabled: Boolean = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent) =
            if (isPagingEnabled) super.onTouchEvent(event) else false

    override fun onInterceptTouchEvent(event: MotionEvent) =
            if (isPagingEnabled) super.onInterceptTouchEvent(event) else false
}