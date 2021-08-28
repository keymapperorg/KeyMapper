package io.github.sds100.keymapper.actions.swipegesture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Created by sds100 on 08/08/20.
 */
class PickSwipeImageView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : AppCompatImageView(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    private val mPaint = Paint().apply {
        color = context.color(R.color.yellow)
        strokeWidth = 10F
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    private val TOUCH_TOLERANCE = 4F
    private var mX = 0F
    private var mY = 0F

    private var displayPath = SerializablePath()
    private val _finishedPath = MutableStateFlow<SerializablePath?>(null)
    val finishedPath = _finishedPath.asStateFlow()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas == null || displayPath.isEmpty) return

        parent.requestDisallowInterceptTouchEvent(true)
        canvas.drawPath(displayPath, mPaint)
    }

    private fun onTouchStart(x: Float, y: Float) {
        displayPath.reset()
        displayPath.addMove(x, y)
        mX = x
        mY = y
    }

    private fun onTouchMove(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            displayPath.addQuad(mX, mY, (x+mX)/2, (y+mY)/2)
            mX = x
            mY = y
        }
    }

    private fun onTouchEnd() {
        displayPath.addLine(mX, mY)
        _finishedPath.value = displayPath
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                onTouchStart(event.x, event.y)
                invalidate()
                requestLayout()
            }
            MotionEvent.ACTION_MOVE -> {
                onTouchMove(event.x, event.y)
                invalidate()
                requestLayout()
            }
            MotionEvent.ACTION_UP -> {
                onTouchEnd()
                invalidate()
                requestLayout()
            }
        }
        return true
    }
}