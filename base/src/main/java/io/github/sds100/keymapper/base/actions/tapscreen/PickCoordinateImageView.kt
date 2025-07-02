package io.github.sds100.keymapper.base.actions.tapscreen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

class PickCoordinateImageView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    val pointCoordinates = MutableStateFlow<Point?>(null)

    private val coordinateLinePaint = Paint().apply {
        color = context.color(R.color.coordinate_line)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        pointCoordinates.value?.let {
            canvas.drawLine(
                it.x.toFloat(),
                0f,
                it.x.toFloat(),
                height.toFloat(),
                coordinateLinePaint,
            )
            canvas.drawLine(
                0f,
                it.y.toFloat(),
                width.toFloat(),
                it.y.toFloat(),
                coordinateLinePaint,
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                pointCoordinates.value = Point(event.x.roundToInt(), event.y.roundToInt())

                invalidate()
                requestLayout()
            }
        }

        return super.onTouchEvent(event)
    }
}
