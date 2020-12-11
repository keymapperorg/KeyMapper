package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber

class FingerprintGestureViewModel(
    private val mRepository: FingerprintMapRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    private val mFingerprintGestureMaps =
        combine(
            mRepository.swipeDown,
            mRepository.swipeUp,
            mRepository.swipeLeft,
            mRepository.swipeRight
        ) { swipeDown, swipeUp, swipeLeft, swipeRight ->
            mapOf(
                FingerprintGestureUtils.SWIPE_DOWN to swipeDown,
                FingerprintGestureUtils.SWIPE_UP to swipeUp,
                FingerprintGestureUtils.SWIPE_LEFT to swipeLeft,
                FingerprintGestureUtils.SWIPE_RIGHT to swipeRight
            )
        }

    private val _models =
        MutableLiveData<State<List<FingerprintGestureMapListItemModel>>>(Loading())

    val models: LiveData<State<List<FingerprintGestureMapListItemModel>>> = _models

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(mFingerprintGestureMaps.asLiveData()) {
            value = BuildFingerprintGestureModels(it)
        }
    }

    val eventStream: LiveData<Event> = _eventStream

    fun setModels(models: List<FingerprintGestureMapListItemModel>) = viewModelScope.launch {
        _models.value = Data(models)
    }

    fun setEnabled(id: String, isEnabled: Boolean) = viewModelScope.launch {
        mRepository.editGesture(id) {
            Timber.d("edit $id $isEnabled")
            it.copy(isEnabled = isEnabled)
        }
    }

    fun rebuildModels() = viewModelScope.launch {
        _models.value = Loading()

        mFingerprintGestureMaps.firstOrNull()?.let {
            _eventStream.value = BuildFingerprintGestureModels(it)
        }
    }

    fun fixError(failure: Failure) {
        _eventStream.value = FixFailure(failure)
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mRepository: FingerprintMapRepository,
        private val mDeviceInfoRepository: DeviceInfoRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(mRepository, mDeviceInfoRepository) as T
        }
    }
}