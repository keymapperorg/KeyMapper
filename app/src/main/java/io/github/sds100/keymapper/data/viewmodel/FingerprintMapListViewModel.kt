package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.FingerprintMapListItemModel
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.IModelState
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FingerprintMapListViewModel(
    private val repository: FingerprintMapRepository,
    private val deviceInfoRepository: DeviceInfoRepository
) : ViewModel(), IModelState<List<FingerprintMapListItemModel>> {

    private val fingerprintGestureMaps =
        combine(
            repository.swipeDown,
            repository.swipeUp,
            repository.swipeLeft,
            repository.swipeRight
        ) { swipeDown, swipeUp, swipeLeft, swipeRight ->
            mapOf(
                FingerprintMapUtils.SWIPE_DOWN to swipeDown,
                FingerprintMapUtils.SWIPE_UP to swipeUp,
                FingerprintMapUtils.SWIPE_LEFT to swipeLeft,
                FingerprintMapUtils.SWIPE_RIGHT to swipeRight
            )
        }

    private val _fingerprintGesturesAvailable = MutableLiveData<Boolean>()
    val fingerprintGesturesAvailable: LiveData<Boolean> = _fingerprintGesturesAvailable

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(fingerprintGestureMaps.asLiveData()) {
            //this is important to prevent events being sent in the wrong order
            postValue(BuildFingerprintMapModels(it))
        }
    }

    val eventStream: LiveData<Event> = _eventStream

    private val _model = MutableLiveData<DataState<List<FingerprintMapListItemModel>>>()
    override val model = _model
    override val viewState = MutableLiveData<ViewState>(ViewLoading())

    init {
        viewModelScope.launch {
            repository.fingerprintGesturesAvailable.collect {
                _fingerprintGesturesAvailable.value = it
            }
        }
    }

    fun setModels(models: List<FingerprintMapListItemModel>) {
        _model.value = Data(models)
    }

    fun setEnabled(id: String, isEnabled: Boolean) = viewModelScope.launch {
        repository.updateGesture(id) {
            it.copy(isEnabled = isEnabled)
        }
    }

    fun rebuildModels() {
        _model.value = Loading()

        viewModelScope.launch {
            fingerprintGestureMaps.firstOrNull()?.let {
                _eventStream.postValue(BuildFingerprintMapModels(it))
            }
        }
    }

    fun fixError(failure: Failure) {
        _eventStream.value = FixFailure(failure)
    }

    fun backupAll() = run { _eventStream.value = BackupFingerprintMaps() }

    fun requestReset() = run { _eventStream.value = RequestFingerprintMapReset() }

    fun reset() {
        viewModelScope.launch {
            repository.reset()
        }
    }

    suspend fun getDeviceInfoList() = deviceInfoRepository.getAll()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val repository: FingerprintMapRepository,
        private val deviceInfoRepository: DeviceInfoRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintMapListViewModel(repository, deviceInfoRepository) as T
        }
    }
}