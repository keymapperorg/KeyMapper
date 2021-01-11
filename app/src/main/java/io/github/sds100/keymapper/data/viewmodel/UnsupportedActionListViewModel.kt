package io.github.sds100.keymapper.data.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.model.UnsupportedSystemActionListItemModel
import io.github.sds100.keymapper.data.repository.SystemActionRepository
import io.github.sds100.keymapper.util.Loading
import io.github.sds100.keymapper.util.getState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 31/03/2020.
 */
class UnsupportedActionListViewModel(
    private val repository: SystemActionRepository
) : ViewModel() {

    val isTapCoordinateActionSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    val unsupportedSystemActions = liveData {
        emit(Loading())

        val unsupportedActions = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            repository.unsupportedSystemActions.map {
                val systemAction = it.key
                val failure = it.value

                UnsupportedSystemActionListItemModel(systemAction.id,
                    systemAction.descriptionRes,
                    systemAction.iconRes,
                    failure)
            }.getState()
        }

        emit(unsupportedActions)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val systemActionRepository: SystemActionRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return UnsupportedActionListViewModel(systemActionRepository) as T
        }
    }
}