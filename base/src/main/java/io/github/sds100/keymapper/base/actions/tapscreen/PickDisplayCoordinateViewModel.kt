package io.github.sds100.keymapper.base.actions.tapscreen

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.system.display.DisplayAdapter
import javax.inject.Inject
import kotlin.math.roundToInt
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

@HiltViewModel
class PickDisplayCoordinateViewModel @Inject constructor(
    private val displayAdapter: DisplayAdapter,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider {

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

    /**
     * The display size that the coordinate is for. See issue #2217. This is the size of the
     * screenshot if one is chosen because the coordinate is in the screenshot's pixel space,
     * otherwise the resolution of the action being edited, otherwise the current display size.
     */
    private val screenshotResolution: MutableStateFlow<SizeKM?> = MutableStateFlow(null)
    private val loadedResolution: MutableStateFlow<SizeKM?> = MutableStateFlow(null)

    fun selectedScreenshot(newBitmap: Bitmap) {
        val displaySize = displayAdapter.size

        // check whether the height and width of the bitmap match the display size, even when it is rotated.
        if ((displaySize.width != newBitmap.width && displaySize.height != newBitmap.height) &&
            (displaySize.height != newBitmap.width && displaySize.width != newBitmap.height)
        ) {
            viewModelScope.launch {
                val snackBar = DialogModel.SnackBar(
                    message = getString(R.string.toast_incorrect_screenshot_resolution),
                )

                showDialog("incorrect_resolution", snackBar)
            }

            return
        }

        screenshotResolution.value = SizeKM(newBitmap.width, newBitmap.height)
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

            val description = showDialog(
                "coordinate_description",
                DialogModel.Text(
                    getString(R.string.hint_tap_coordinate_title),
                    allowEmpty = true,
                    text = description.value ?: "",
                ),
            ) ?: return@launch

            _returnResult.emit(
                PickCoordinateResult(x, y, description, screenResolution()),
            )
        }
    }

    /**
     * See issue #2217. Prefer the screenshot's resolution because the coordinate is in its pixel
     * space, then the resolution the action was already saved with so that editing an action on a
     * device that has since changed resolution does not stamp the wrong one on unchanged
     * coordinates.
     */
    private fun screenResolution(): SizeKM {
        return screenshotResolution.value ?: loadedResolution.value ?: displayAdapter.size
    }

    fun loadResult(result: PickCoordinateResult) {
        viewModelScope.launch {
            x.value = result.x
            y.value = result.y
            description.value = result.description
            loadedResolution.value = result.screenResolution
        }
    }

    override fun onCleared() {
        bitmap.value?.recycle()
        _bitmap.value = null

        super.onCleared()
    }
}
