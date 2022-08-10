package io.github.sds100.keymapper.actions.tapscreen

import android.graphics.Bitmap
import android.graphics.Point
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Created by sds100 on 03/08/20.
 */

@HiltViewModel
class PickDisplayCoordinateViewModel @Inject constructor(
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    private val x = MutableStateFlow<Int?>(null)
    private val y = MutableStateFlow<Int?>(null)

    val xString = x.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val yString = y.map {
        it ?: return@map ""

        it.toString()
    }.stateIn(viewModelScope, SharingStarted.Lazily, "")

    val isDoneButtonEnabled: StateFlow<Boolean> = combine(x, y) { x, y ->
        x ?: return@combine false
        y ?: return@combine false

        x >= 0 && y >= 0
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap = _bitmap.asStateFlow()

    private val _returnResult = MutableSharedFlow<PickCoordinateResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val description: MutableStateFlow<String?> = MutableStateFlow(null)

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
                    message = getString(R.string.create_tap_screen_incorrect_resolution_error)
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

            val description = showPopup(
                "coordinate_description",
                PopupUi.Text(
                    getString(R.string.hint_tap_coordinate_title),
                    allowEmpty = true,
                    text = description.value ?: ""
                )
            ) ?: return@launch

            _returnResult.emit(PickCoordinateResult(x, y, description))
        }
    }

    fun loadResult(result: PickCoordinateResult) {
        viewModelScope.launch {
            x.value = result.x
            y.value = result.y
            description.value = result.description
        }
    }

    override fun onCleared() {
        bitmap.value?.recycle()
        _bitmap.value = null

        super.onCleared()
    }
}
