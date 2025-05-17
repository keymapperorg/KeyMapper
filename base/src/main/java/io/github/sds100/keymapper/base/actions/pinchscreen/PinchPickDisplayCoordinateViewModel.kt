package io.github.sds100.keymapper.base.actions.pinchscreen

import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.view.View
import android.widget.AdapterView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.base.utils.ui.PopupUi
import io.github.sds100.keymapper.base.utils.ui.PopupViewModel
import io.github.sds100.keymapper.base.utils.ui.PopupViewModelImpl
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showPopup
import io.github.sds100.keymapper.common.utils.PinchScreenType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PinchPickDisplayCoordinateViewModel @Inject constructor(
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    private val pinchTypes = arrayOf(PinchScreenType.PINCH_IN.name, PinchScreenType.PINCH_OUT.name)

    private val x = MutableStateFlow<Int?>(null)
    private val y = MutableStateFlow<Int?>(null)
    private val distance = MutableStateFlow<Int?>(null)
    private val pinchType = MutableStateFlow<PinchScreenType?>(PinchScreenType.PINCH_IN)
    private val fingerCount = MutableStateFlow<Int?>(2)
    private val duration = MutableStateFlow<Int?>(200)

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    private val _returnResult = MutableSharedFlow<PinchPickCoordinateResult>()

    private val description: MutableStateFlow<String?> = MutableStateFlow(null)

    val xString = x.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val yString = y.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val distanceString = distance.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val distanceError: StateFlow<String?> = distance.map { distance ->
        if (distance == null) {
            return@map resourceProvider.getString(R.string.error_cant_be_empty)
        }

        if (distance <= 0) {
            return@map resourceProvider.getString(R.string.error_pinch_screen_distance_too_low)
        }

        null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pinchTypeSpinnerSelection = pinchType.map {
        it ?: return@map 0

        this.pinchTypes.indexOf(it.name)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val fingerCountString = fingerCount.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val fingerCountError: StateFlow<String?> = fingerCount.map { count ->
        if (count == null) {
            return@map resourceProvider.getString(R.string.error_cant_be_empty)
        }

        if (count < 2) {
            return@map resourceProvider.getString(R.string.error_pinch_screen_must_be_two_or_more_fingers)
        }

        var maxFingerCount = 10

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            maxFingerCount = GestureDescription.getMaxStrokeCount()
        }

        if (count > maxFingerCount) {
            return@map resourceProvider.getString(
                R.string.error_pinch_screen_must_be_ten_or_less_fingers,
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
            return@map resourceProvider.getString(R.string.error_pinch_screen_duration_must_be_more_than_zero)
        }

        null
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bitmap = _bitmap.asStateFlow()
    val returnResult = _returnResult.asSharedFlow()

    private val isCoordinatesValid: StateFlow<Boolean> =
        combine(x, y, distance, pinchType) { x, y, distance, pinchType ->
            x ?: return@combine false
            y ?: return@combine false
            distance ?: return@combine false
            pinchType ?: return@combine false

            x >= 0 && y >= 0 && distance > 0 && (pinchType == PinchScreenType.PINCH_IN || pinchType == PinchScreenType.PINCH_OUT)
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isDoneButtonEnabled: StateFlow<Boolean> =
        combine(
            isCoordinatesValid,
            fingerCountError,
            durationError,
        ) { isCoordinatesValid, fingerCountError, durationError ->
            isCoordinatesValid && fingerCountError == null && durationError == null
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun selectedScreenshot(newBitmap: Bitmap, displaySize: Point) {
        // check whether the height and width of the bitmap match the display size, even when it is rotated.
        if ((displaySize.x != newBitmap.width && displaySize.y != newBitmap.height) &&
            (displaySize.y != newBitmap.width && displaySize.x != newBitmap.height)
        ) {
            viewModelScope.launch {
                val snackBar = PopupUi.SnackBar(
                    message = getString(R.string.toast_incorrect_screenshot_resolution),
                )

                showPopup("incorrect_resolution", snackBar)
            }

            return
        }

        _bitmap.value = newBitmap
    }

    fun setX(x: String) {
        this.x.value = x.toIntOrNull()
    }

    fun setY(y: String) {
        this.y.value = y.toIntOrNull()
    }

    fun setDistance(distance: String) {
        this.distance.value = distance.toIntOrNull()
    }

    private fun setPinchType(type: String) {
        if (type == PinchScreenType.PINCH_IN.name) {
            this.pinchType.value = PinchScreenType.PINCH_IN
        } else {
            this.pinchType.value = PinchScreenType.PINCH_OUT
        }
    }

    fun setFingerCount(fingerCount: String) {
        this.fingerCount.value = fingerCount.toIntOrNull()
    }

    fun setDuration(duration: String) {
        this.duration.value = duration.toIntOrNull()
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

    fun onDoneClick() {
        viewModelScope.launch {
            val x = x.value ?: return@launch
            val y = y.value ?: return@launch
            val distance = distance.value ?: return@launch
            val pinchType = pinchType.value ?: return@launch
            val fingerCount = fingerCount.value ?: return@launch
            val duration = duration.value ?: return@launch

            val description = showPopup(
                "coordinate_description",
                PopupUi.Text(
                    getString(R.string.hint_tap_coordinate_title),
                    allowEmpty = true,
                    text = description.value ?: "",
                ),
            ) ?: return@launch

            _returnResult.emit(
                PinchPickCoordinateResult(
                    x,
                    y,
                    distance,
                    pinchType,
                    fingerCount,
                    duration,
                    description,
                ),
            )
        }
    }

    fun onPinchTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        this.setPinchType(pinchTypes[position])
    }

    fun loadResult(result: PinchPickCoordinateResult) {
        viewModelScope.launch {
            x.value = result.x
            y.value = result.y
            distance.value = result.distance
            pinchType.value = result.pinchType
            fingerCount.value = result.fingerCount
            duration.value = result.duration
            description.value = result.description
        }
    }

    override fun onCleared() {
        bitmap.value?.recycle()
        _bitmap.value = null

        super.onCleared()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PinchPickDisplayCoordinateViewModel(resourceProvider) as T
    }
}
