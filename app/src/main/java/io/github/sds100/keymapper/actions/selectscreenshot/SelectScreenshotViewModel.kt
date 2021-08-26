package io.github.sds100.keymapper.actions.selectscreenshot

import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SelectScreenshotViewModel (
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap = _bitmap.asStateFlow()

    var displaySize = Point(0, 0)
    val blankScreen = GradientDrawable()

    fun selectedScreenshot(newBitmap: Bitmap) {
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
        Timber.d("Setting bitmap!")
        _bitmap.value = newBitmap
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

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return SelectScreenshotViewModel(resourceProvider) as T
        }
    }
}