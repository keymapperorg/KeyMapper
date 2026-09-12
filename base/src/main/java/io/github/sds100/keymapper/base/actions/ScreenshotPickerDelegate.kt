package io.github.sds100.keymapper.base.actions

import android.graphics.Bitmap
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.system.display.DisplayAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared logic for picking a screenshot to tap/pinch/swipe on. See issue #2217 for why the
 * chosen screen resolution is tracked, and why an aspect ratio match (rather than an exact
 * resolution match) is now sufficient: coordinates are scaled against the runtime display size
 * when the action is performed.
 */
class ScreenshotPickerDelegate(
    private val coroutineScope: CoroutineScope,
    private val displayAdapter: DisplayAdapter,
    resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
) : ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider {

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    private val screenshotResolution = MutableStateFlow<SizeKM?>(null)
    private val loadedResolution = MutableStateFlow<SizeKM?>(null)

    fun selectedScreenshot(newBitmap: Bitmap) {
        val newBitmapSize = SizeKM(newBitmap.width, newBitmap.height)

        if (!displayAdapter.size.hasSameAspectRatio(newBitmapSize)) {
            coroutineScope.launch {
                val snackBar = DialogModel.SnackBar(
                    message = getString(R.string.toast_incorrect_screenshot_resolution),
                )

                showDialog("incorrect_resolution", snackBar)
            }

            return
        }

        screenshotResolution.value = newBitmapSize
        _bitmap.value = newBitmap
    }

    fun setLoadedResolution(size: SizeKM?) {
        loadedResolution.value = size
    }

    /**
     * See issue #2217. Prefer the screenshot's resolution because the coordinates are in its
     * pixel space, then the resolution the action was already saved with so that editing an
     * action on a device that has since changed resolution does not stamp the wrong one on
     * unchanged coordinates.
     */
    fun screenResolution(): SizeKM =
        screenshotResolution.value ?: loadedResolution.value ?: displayAdapter.size

    fun recycle() {
        _bitmap.value?.recycle()
        _bitmap.value = null
    }
}
