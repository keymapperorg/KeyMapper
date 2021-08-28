package io.github.sds100.keymapper.actions.swipegesture

import android.graphics.Point
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 03/08/20.
 */
class PickSwipeViewModel(
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider, PopupViewModel by PopupViewModelImpl() {

    private val _returnResult = MutableSharedFlow<PickSwipeResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val description: MutableStateFlow<String?> = MutableStateFlow(null)

    private val path = MutableStateFlow<SerializablePath?>(null)
    private var displaySize = Point(0, 0)
    private var screenshotWidth = 0
    private var screenshotHeight = 0


    val isDoneButtonEnabled: StateFlow<Boolean> = path.map {
        return@map !(it == null || it.isEmpty)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun updatePath(serializablePath: SerializablePath, displaySize: Point, screenshotWidth: Int,
                   screenshotHeight: Int) {
        path.value = serializablePath
        this.displaySize = displaySize
        this.screenshotWidth = screenshotWidth
        this.screenshotHeight = screenshotHeight
    }

    fun onDoneClick() {
        viewModelScope.launch {
            val unscaledPath = path.value ?: return@launch
            val description = showPopup(
                "swipe_description",
                PopupUi.Text(
                    getString(R.string.hint_swipe_gesture_title),
                    allowEmpty = true,
                    text = description.value ?: ""
                )
            ) ?: return@launch

            val xfactor = displaySize.x.toFloat() / screenshotWidth.toFloat()
            val yfactor = displaySize.y.toFloat() / screenshotHeight.toFloat()

            unscaledPath.scalePoints(xfactor, yfactor)
            _returnResult.emit(PickSwipeResult(unscaledPath, description))
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return PickSwipeViewModel(resourceProvider) as T
        }
    }
}
