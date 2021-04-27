package io.github.sds100.keymapper.actions

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.createListState
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.getFullMessage
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 31/03/2020.
 */
class UnsupportedActionListViewModel(
    private val isSystemActionSupported: IsSystemActionSupportedUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider {

    companion object {
        private val isTapCoordinateActionSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    private val _state =
        MutableStateFlow<ListUiState<UnsupportedActionListItem>>(ListUiState.Loading)
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
                val unsupportedSystemActionsWithReasons = SystemActionId.values()
                    .map { it to isSystemActionSupported.invoke(it) }
                    .filter { it.second != null }

                _state.value = sequence {
                    if (!isTapCoordinateActionSupported) {
                        yield(
                            UnsupportedActionListItem(
                                id = "tap_coordinate_action",
                                description = getString(R.string.action_type_tap_coordinate),
                                icon = getDrawable(R.drawable.ic_outline_touch_app_24),
                                reason = Error.SdkVersionTooLow(Build.VERSION_CODES.N)
                                    .getFullMessage(this@UnsupportedActionListViewModel)
                            )
                        )
                    }

                    unsupportedSystemActionsWithReasons.forEach { (id, reason) ->
                        yield(
                            UnsupportedActionListItem(
                                id.toString(),
                                description = getString(SystemActionUtils.getTitle(id)),
                                icon = SystemActionUtils.getIcon(id)?.let { getDrawable(it) },
                                reason = reason!!.getFullMessage(this@UnsupportedActionListViewModel)
                            )
                        )
                    }
                }.toList().createListState()
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val isSystemActionSupported: IsSystemActionSupportedUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return UnsupportedActionListViewModel(
                isSystemActionSupported,
                resourceProvider
            ) as T
        }
    }
}