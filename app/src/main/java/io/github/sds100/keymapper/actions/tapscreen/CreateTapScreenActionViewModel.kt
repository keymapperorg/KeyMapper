package io.github.sds100.keymapper.actions.tapscreen

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.system.display.DisplayAdapter
import javax.inject.Inject

/**
 * Created by sds100 on 03/08/20.
 */

@HiltViewModel
class CreateTapScreenActionViewModel @Inject constructor(private val displayAdapter: DisplayAdapter) : ViewModel() {

    var state: CreateTapScreenActionState by mutableStateOf(CreateTapScreenActionState.EMPTY)
        private set

    fun onSelectScreenshot(newBitmap: Bitmap) {
        //check whether the height and width of the bitmap match the display size, even when it is rotated.
        val displaySize = displayAdapter.getDisplaySize()

        if (
            (displaySize.width != newBitmap.width
                || displaySize.height != newBitmap.height) &&

            (displaySize.height != newBitmap.width
                || displaySize.width != newBitmap.height)
        ) {
            newBitmap.recycle()
            state = state.copy(showIncorrectResolutionSnackBar = true)
            return
        }

        state = state.copy(screenshot = newBitmap.asImageBitmap(), selectedPoint = null)
    }

    fun onDismissIncorrectResolutionSnackBar() {
        state = state.copy(showIncorrectResolutionSnackBar = false)
    }

    fun createResult(): PickCoordinateResult {
        val x = state.x.toInt()
        val y = state.y.toInt()

        return PickCoordinateResult(x, y, state.description)
    }

    fun onDescriptionChange(text: String) {
        state = state.copy(description = text)
    }

    fun onXTextChange(text: String) {
        val error = determineCoordinateError(text)
        state = state.copy(
            x = text,
            xError = error,
            isDoneButtonEnabled = isDoneButtonEnabled(error, state.yError)
        )
    }

    fun onYTextChange(text: String) {
        val error = determineCoordinateError(text)
        state = state.copy(
            y = text,
            yError = error,
            isDoneButtonEnabled = isDoneButtonEnabled(state.xError, error)
        )
    }

    fun onTouchScreenshot(offset: Offset, imageSize: IntSize) {
        state = state.copy(selectedPoint = offset)

        val xRatio = offset.x / imageSize.width
        val yRatio = offset.y / imageSize.height

        // use screenshot size to calculate the point on the screen and not the rendered image size because these differ
        val x: Int = (xRatio * state.screenshot!!.width).toInt()
        val y: Int = (yRatio * state.screenshot!!.height).toInt()

        state = state.copy(
            x = x.toString(),
            xError = CoordinateError.NONE,
            y = y.toString(),
            yError = CoordinateError.NONE,
            isDoneButtonEnabled = true
        )
    }

    private fun isDoneButtonEnabled(xError: CoordinateError, yError: CoordinateError): Boolean {
        return xError == CoordinateError.NONE && yError == CoordinateError.NONE
    }

    private fun determineCoordinateError(text: String): CoordinateError {
        return when {
            text.isEmpty() -> CoordinateError.EMPTY
            text.toIntOrNull() == null -> CoordinateError.NOT_INTEGER
            else -> CoordinateError.NONE
        }
    }
}

enum class CoordinateError {
    NONE, EMPTY, NOT_INTEGER
}

data class CreateTapScreenActionState(
    val x: String,
    val xError: CoordinateError,
    val y: String,
    val yError: CoordinateError,
    val description: String,
    val screenshot: ImageBitmap?,
    val selectedPoint: Offset?,
    val isDoneButtonEnabled: Boolean,
    val showIncorrectResolutionSnackBar: Boolean
) {
    companion object {
        val EMPTY = CreateTapScreenActionState(
            x = "",
            xError = CoordinateError.EMPTY,
            y = "",
            yError = CoordinateError.EMPTY,
            description = "",
            screenshot = null,
            selectedPoint = null,
            isDoneButtonEnabled = false,
            showIncorrectResolutionSnackBar = false
        )
    }
}