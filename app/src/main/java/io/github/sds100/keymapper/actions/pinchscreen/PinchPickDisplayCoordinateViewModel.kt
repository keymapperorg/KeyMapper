package io.github.sds100.keymapper.actions.pinchscreen

import android.graphics.Bitmap
import android.graphics.Point
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PinchPickDisplayCoordinateViewModel(
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    private val pinchTypes = arrayOf(PinchScreenType.PINCH_IN.name, PinchScreenType.PINCH_OUT.name)
    private val pinchTypesDisplayValues = arrayOf(getString(R.string.hint_coordinate_type_PINCH_IN), getString(R.string.hint_coordinate_type_PINCH_OUT))
    val pinchTypeSpinnerAdapter = ArrayAdapter(getContext(), android.R.layout.simple_spinner_dropdown_item, pinchTypesDisplayValues)

    private val x = MutableStateFlow<Int?>(null)
    private val y = MutableStateFlow<Int?>(null)
    private val radius = MutableStateFlow<Int?>(null)
    private val pinchType = MutableStateFlow<PinchScreenType?>(PinchScreenType.PINCH_IN)
    private val fingerCount = MutableStateFlow<Int?>(2)
    private val duration = MutableStateFlow<Int?>(null)

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

    val radiusString = radius.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val pinchTypeSpinnerSelection = pinchType.map {
        it ?: return@map 0

        this.pinchTypes.indexOf(it.name)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val fingerCountString = fingerCount.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val durationString = duration.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val bitmap = _bitmap.asStateFlow()
    val returnResult = _returnResult.asSharedFlow()

    val isSelectStartEndSwitchEnabled:StateFlow<Boolean> = combine(bitmap) {
        bitmap?.value != null
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val isCoordinatesValid: StateFlow<Boolean> = combine(x, y, radius, pinchType) { x, y, radius, pinchType ->
        x ?: return@combine false
        y ?: return@combine false
        radius ?: return@combine false
        pinchType ?: return@combine false

        x >= 0 && y >= 0 && radius >= 0 && (pinchType == PinchScreenType.PINCH_IN || pinchType == PinchScreenType.PINCH_OUT)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val isOptionsValid: StateFlow<Boolean> = combine(fingerCount, duration) { fingerCount, duration ->
        fingerCount ?: return@combine false
        duration ?: return@combine false

        fingerCount in 2..9 && duration > 0
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val isDoneButtonEnabled: StateFlow<Boolean> = combine(isCoordinatesValid, isOptionsValid) { isCoordinatesValid, isOptionsValid ->
        isCoordinatesValid && isOptionsValid
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun selectedScreenshot(newBitmap: Bitmap, displaySize: Point) {
        //check whether the height and width of the bitmap match the display size, even when it is rotated.
        if (
            (displaySize.x != newBitmap.width
                && displaySize.y != newBitmap.height) &&

            (displaySize.y != newBitmap.width
                && displaySize.x != newBitmap.height)
        ) {
            viewModelScope.launch {
                val snackBar = PopupUi.SnackBar(
                    message = getString(R.string.toast_incorrect_screenshot_resolution)
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

    fun setRadius(radius: String) {
        this.radius.value = radius.toIntOrNull()
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
            val radius = radius.value ?: return@launch
            val pinchType = pinchType.value ?: return@launch
            val fingerCount = fingerCount.value ?: return@launch
            val duration = duration.value ?: return@launch

            val description = showPopup(
                "coordinate_description",
                PopupUi.Text(
                    getString(R.string.hint_tap_coordinate_title),
                    allowEmpty = true,
                    text = description.value ?: ""
                )
            ) ?: return@launch

            _returnResult.emit(PinchPickCoordinateResult(x, y, radius, pinchType, fingerCount, duration, description))
        }
    }

    fun onPinchTypeSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        this.setPinchType(pinchTypes[position])
    }

    fun loadResult(result: PinchPickCoordinateResult) {
        viewModelScope.launch {
            x.value = result.x
            y.value = result.y
            radius.value = result.radius
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
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PinchPickDisplayCoordinateViewModel(resourceProvider) as T
        }
    }
}
