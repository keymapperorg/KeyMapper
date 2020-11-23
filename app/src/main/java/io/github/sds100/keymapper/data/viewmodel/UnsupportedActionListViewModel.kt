package io.github.sds100.keymapper.data.viewmodel

import android.os.Build
import androidx.lifecycle.*
import io.github.sds100.keymapper.data.SystemActionRepository
import io.github.sds100.keymapper.data.model.UnsupportedSystemActionListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 31/03/2020.
 */
class UnsupportedActionListViewModel(
    private val mRepository: SystemActionRepository
) : ViewModel(), ProgressCallback {

    override val loadingContent = MutableLiveData(false)

    val isTapCoordinateActionSupported =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    val unsupportedSystemActions = liveData {
        val unsupportedActions = withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            mRepository.unsupportedSystemActions.map {
                val systemAction = it.key
                val failure = it.value

                UnsupportedSystemActionListItemModel(systemAction.id,
                    systemAction.descriptionRes,
                    systemAction.iconRes,
                    failure)
            }
        }

        emit(unsupportedActions)
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mSystemActionRepository: SystemActionRepository) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return UnsupportedActionListViewModel(mSystemActionRepository) as T
        }
    }
}