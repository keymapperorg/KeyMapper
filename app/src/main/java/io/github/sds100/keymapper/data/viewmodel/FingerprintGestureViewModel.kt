package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.model.behavior.FingerprintGestureMapOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintGestureRepository
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.FingerprintGestureUtils
import io.github.sds100.keymapper.util.Loading
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class FingerprintGestureViewModel(
    private val mRepository: FingerprintGestureRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    private val mFingerprintGestureMaps = combine(mRepository.swipeDown, mRepository.swipeUp) { swipeDown, swipeUp ->
        Timber.d(swipeDown.actionList.joinToString { it.uid })
        mapOf(
            FingerprintGestureUtils.SWIPE_DOWN to swipeDown,
            FingerprintGestureUtils.SWIPE_UP to swipeUp
        )
    }

    private val _models: MutableLiveData<State<List<FingerprintGestureMapListItemModel>>> = MutableLiveData(Loading())
    val models: LiveData<State<List<FingerprintGestureMapListItemModel>>> = _models

    private val _buildModels: MutableSharedFlow<Map<String, FingerprintGestureMap>> = MutableSharedFlow()
    val buildModels = _buildModels.asSharedFlow()

    private val _editOptions: MutableSharedFlow<FingerprintGestureMapOptions> = MutableSharedFlow()
    val editOptions = _editOptions.asSharedFlow()

    init {
        viewModelScope.launch {
            mFingerprintGestureMaps.collect {
                _buildModels.emit(it)
            }
        }
    }

    fun setModels(models: List<FingerprintGestureMapListItemModel>) = viewModelScope.launch {
        _models.value = Data(models)
    }

    fun addAction(gestureId: String, action: Action) = viewModelScope.launch {
        mRepository.editGesture(gestureId) {
            val newActionList = it.actionList.toMutableList().apply {
                add(action)
            }

            it.copy(actionList = newActionList)
        }
    }

    fun addConstraint(gestureId: String, constraint: Constraint) = viewModelScope.launch {
        mRepository.editGesture(gestureId) {
            val newConstraintList = it.constraintList.toMutableList().apply {
                add(constraint)
            }

            it.copy(constraintList = newConstraintList.toList())
        }
    }

    fun removeAction(gestureId: String, actionId: String) = viewModelScope.launch {
        mRepository.editGesture(gestureId) {
            val newActionList = it.actionList.toMutableList().apply {
                removeAll { action ->
                    Timber.d("${action.uid} $actionId")
                    action.uid == actionId
                }
            }

            it.copy(actionList = newActionList)
        }
    }

    fun setEnabled(id: String, isEnabled: Boolean) = viewModelScope.launch {
        mRepository.editGesture(id) {
            it.copy(isEnabled = isEnabled)
        }
    }

    fun rebuildModels() = viewModelScope.launch {
        _models.value = Loading()

        mFingerprintGestureMaps.firstOrNull()?.let {
            _buildModels.emit(it)
        }
    }

    fun editOptions(gestureId: String) = viewModelScope.launch {
        mFingerprintGestureMaps.firstOrNull()?.get(gestureId)?.let {
            val options = FingerprintGestureMapOptions(gestureId, it)
            _editOptions.emit(options)
        }
    }

    fun setOptions(options: FingerprintGestureMapOptions) = viewModelScope.launch {
        mFingerprintGestureMaps.firstOrNull()?.get(options.gestureId)?.let {
            val newMap = options.applyToFingerprintGestureMap(it)

            mRepository.editGesture(options.gestureId) { newMap }
        }
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mRepository: FingerprintGestureRepository,
        private val mDeviceInfoRepository: DeviceInfoRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(mRepository, mDeviceInfoRepository) as T
        }
    }
}