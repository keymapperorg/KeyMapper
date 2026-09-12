package io.github.sds100.keymapper.base.actions.swipescreen

import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ScreenshotPickerDelegate
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.system.display.DisplayAdapter
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenshotTouchType {
    START,
    END,
}

@HiltViewModel
class SwipePickDisplayCoordinateViewModel @Inject constructor(
    private val displayAdapter: DisplayAdapter,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider {

    val screenshotTouchTypeStart = ScreenshotTouchType.START
    val screenshotTouchTypeEnd = ScreenshotTouchType.END
    private val xStart = MutableStateFlow<Int?>(null)
    private val yStart = MutableStateFlow<Int?>(null)
    private val xEnd = MutableStateFlow<Int?>(null)
    private val yEnd = MutableStateFlow<Int?>(null)
    private val fingerCount = MutableStateFlow<Int?>(1)
    private val duration = MutableStateFlow<Int?>(200)

    private val screenshotDelegate = ScreenshotPickerDelegate(
        viewModelScope,
        displayAdapter,
        resourceProvider,
        dialogProvider,
    )
    private val _returnResult = MutableSharedFlow<SwipePickCoordinateResult>()
    private val screenshotTouchType = MutableStateFlow(ScreenshotTouchType.START)

    private val description: MutableStateFlow<String?> = MutableStateFlow(null)

    val xStartString = xStart.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val yStartString = yStart.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val xEndString = xEnd.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val yEndString = yEnd.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val fingerCountString = fingerCount.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val fingerCountError: StateFlow<String?> = fingerCount.map { count ->
        if (count == null) {
            return@map resourceProvider.getString(R.string.error_cant_be_empty)
        }

        if (count <= 0) {
            return@map resourceProvider.getString(
                R.string.error_swipe_screen_fingercount_must_be_more_than_zero,
            )
        }

        var maxFingerCount = 10

        maxFingerCount = GestureDescription.getMaxStrokeCount()

        if (count > maxFingerCount) {
            return@map resourceProvider.getString(
                R.string.error_swipe_screen_must_be_ten_or_less_fingers,
                arrayOf(maxFingerCount),
            )
        }

        null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val durationString = duration.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val durationError: StateFlow<String?> = duration.map { d ->
        if (d == null) {
            return@map resourceProvider.getString(R.string.error_cant_be_empty)
        }

        if (d <= 0) {
            return@map resourceProvider.getString(
                R.string.error_swipe_screen_duration_must_be_more_than_zero,
            )
        }

        null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bitmap = screenshotDelegate.bitmap
    val returnResult = _returnResult.asSharedFlow()

    val isSelectStartEndSwitchEnabled: StateFlow<Boolean> = combine(bitmap) {
        bitmap.value != null
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val isCoordinatesValid: StateFlow<Boolean> =
        combine(xStart, yStart, xEnd, yEnd) { xStart, yStart, xEnd, yEnd ->
            xStart ?: return@combine false
            yStart ?: return@combine false
            xEnd ?: return@combine false
            yEnd ?: return@combine false

            xStart >= 0 && yStart >= 0 && xEnd >= 0 && yEnd >= 0
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val isOptionsValid: StateFlow<Boolean> =
        combine(fingerCount, duration) { fingerCount, duration ->
            fingerCount ?: return@combine false
            duration ?: return@combine false

            fingerCount > 0 && duration > 0
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isDoneButtonEnabled: StateFlow<Boolean> =
        combine(isCoordinatesValid, isOptionsValid) { isCoordinatesValid, isOptionsValid ->
            isCoordinatesValid && isOptionsValid
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun selectedScreenshot(newBitmap: Bitmap) {
        screenshotTouchType.value = ScreenshotTouchType.START
        screenshotDelegate.selectedScreenshot(newBitmap)
    }

    fun setXStart(x: String) {
        this.xStart.value = x.toIntOrNull()
    }

    fun setYStart(y: String) {
        this.yStart.value = y.toIntOrNull()
    }

    fun setXEnd(x: String) {
        this.xEnd.value = x.toIntOrNull()
    }

    fun setYEnd(y: String) {
        this.yEnd.value = y.toIntOrNull()
    }

    fun setFingerCount(fingerCount: String) {
        this.fingerCount.value = fingerCount.toIntOrNull()
    }

    fun setDuration(duration: String) {
        this.duration.value = duration.toIntOrNull()
    }

    fun setStartOrEndCoordinates(isChecked: Boolean, type: ScreenshotTouchType) {
        if (isChecked) this.screenshotTouchType.value = type
    }

    /**
     * [screenshotXRatio] The ratio between the point where the user pressed to the width of the image.
     * [screenshotYRatio] The ratio between the point where the user pressed to the height of the image.
     */
    fun onScreenshotTouch(screenshotXRatio: Float, screenshotYRatio: Float) {
        bitmap.value?.let {
            val displayX = it.width * screenshotXRatio
            val displayY = it.height * screenshotYRatio

            if (screenshotTouchType.value == ScreenshotTouchType.START) {
                xStart.value = displayX.roundToInt()
                yStart.value = displayY.roundToInt()
            } else {
                xEnd.value = displayX.roundToInt()
                yEnd.value = displayY.roundToInt()
            }
        }
    }

    fun onDoneClick() {
        viewModelScope.launch {
            val xStart = xStart.value ?: return@launch
            val yStart = yStart.value ?: return@launch
            val xEnd = xEnd.value ?: return@launch
            val yEnd = yEnd.value ?: return@launch
            val fingerCount = fingerCount.value ?: return@launch
            val duration = duration.value ?: return@launch

            val description = showDialog(
                "coordinate_description",
                DialogModel.Text(
                    getString(R.string.hint_tap_coordinate_title),
                    allowEmpty = true,
                    text = description.value ?: "",
                ),
            ) ?: return@launch

            _returnResult.emit(
                SwipePickCoordinateResult(
                    xStart,
                    yStart,
                    xEnd,
                    yEnd,
                    fingerCount,
                    duration,
                    description,
                    screenshotDelegate.screenResolution(),
                ),
            )
        }
    }

    fun loadResult(result: SwipePickCoordinateResult) {
        viewModelScope.launch {
            xStart.value = result.xStart
            yStart.value = result.yStart
            xEnd.value = result.xEnd
            yEnd.value = result.yEnd
            fingerCount.value = result.fingerCount
            duration.value = result.duration
            description.value = result.description
            screenshotDelegate.setLoadedResolution(result.screenResolution)
        }
    }

    override fun onCleared() {
        screenshotDelegate.recycle()

        super.onCleared()
    }
}
