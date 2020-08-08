package io.github.sds100.keymapper.data.viewmodel

import android.graphics.Bitmap
import android.graphics.Point
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.util.Event
import kotlin.math.roundToInt

/**
 * Created by sds100 on 03/08/20.
 */

class TapCoordinateActionTypeViewModel : ViewModel() {

    val x = MutableLiveData<Int>()
    val y = MutableLiveData<Int>()

    val isValid = MediatorLiveData<Boolean>().apply {
        fun invalidate() {
            value = x.value?.let { it > 0 } == true && y.value?.let { it > 0 } == true
        }

        addSource(x) {
            invalidate()
        }

        addSource(y) {
            invalidate()
        }
    }

    val bitmap = MutableLiveData<Bitmap>()

    val selectScreenshotEvent = MutableLiveData<Event<Unit>>()
    val incorrectScreenshotResolutionEvent = MutableLiveData<Event<Unit>>()

    fun selectedScreenshot(newBitmap: Bitmap, displaySize: Point) {
        //check whether the height and width of the bitmap match the display size, even when it is rotated.
        if (
            (displaySize.x != newBitmap.width
                && displaySize.y != newBitmap.height) &&

            (displaySize.y != newBitmap.width
                && displaySize.x != newBitmap.height)
        ) {
            incorrectScreenshotResolutionEvent.value = Event(Unit)
            return
        }

        bitmap.value = newBitmap
    }

    fun selectScreenshot() {
        selectScreenshotEvent.value = Event(Unit)
    }

    /**
     * [screenshotXRatio] The ratio between the point where the user pressed to the width of the image.
     * [screenshotYRatio] The ratio between the point where the user pressed to the height of the image.
     */
    fun onScreenshotTouch(screenshotXRatio: Float, screenshotYRatio: Float) {
        bitmap.value?.let {
            //origin is top left of the device's display

            val isBitmapLandscape = it.width > it.height

            val displayX = if (isBitmapLandscape) {
                it.height * screenshotYRatio
            } else {
                it.width * screenshotXRatio
            }

            val displayY = if (isBitmapLandscape) {
                it.width * screenshotXRatio
            } else {
                it.height * screenshotYRatio
            }

            x.value = displayX.roundToInt()
            y.value = displayY.roundToInt()
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TapCoordinateActionTypeViewModel() as T
        }
    }
}
