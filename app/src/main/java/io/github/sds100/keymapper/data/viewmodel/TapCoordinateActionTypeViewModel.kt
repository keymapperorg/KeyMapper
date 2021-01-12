package io.github.sds100.keymapper.data.viewmodel

import android.graphics.Bitmap
import android.graphics.Point
import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.MessageEvent
import io.github.sds100.keymapper.util.SelectScreenshot
import kotlin.math.roundToInt

/**
 * Created by sds100 on 03/08/20.
 */

class TapCoordinateActionTypeViewModel : ViewModel() {

    val x = MutableLiveData<Int>()
    val y = MutableLiveData<Int>()

    val isValid = MediatorLiveData<Boolean>().apply {
        fun invalidate() {
            value = x.value?.let { it >= 0 } == true && y.value?.let { it >= 0 } == true
        }

        addSource(x) {
            invalidate()
        }

        addSource(y) {
            invalidate()
        }
    }

    val bitmap = MutableLiveData<Bitmap>()

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    fun selectedScreenshot(newBitmap: Bitmap, displaySize: Point) {
        //check whether the height and width of the bitmap match the display size, even when it is rotated.
        if (
            (displaySize.x != newBitmap.width
                && displaySize.y != newBitmap.height) &&

            (displaySize.y != newBitmap.width
                && displaySize.x != newBitmap.height)
        ) {
            _eventStream.value = MessageEvent(R.string.toast_incorrect_screenshot_resolution)
            return
        }

        bitmap.value = newBitmap
    }

    fun selectScreenshot() {
        _eventStream.value = SelectScreenshot()
    }

    /**
     * [screenshotXRatio] The ratio between the point where the user pressed to the width of the image.
     * [screenshotYRatio] The ratio between the point where the user pressed to the height of the image.
     */
    fun onScreenshotTouch(screenshotXRatio: Float, screenshotYRatio: Float) {
        bitmap.value?.let {

            val displayX = it.width * screenshotXRatio
            val displayY = it.height * screenshotYRatio

            x.value = displayX.roundToInt()
            y.value = displayY.roundToInt()
        }
    }

    override fun onCleared() {
        bitmap.value?.recycle()
        bitmap.value = null

        super.onCleared()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TapCoordinateActionTypeViewModel() as T
        }
    }
}
