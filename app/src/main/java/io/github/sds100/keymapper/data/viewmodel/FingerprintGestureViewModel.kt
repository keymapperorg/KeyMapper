package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.model.options.FingerprintGestureMapOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.FingerprintGestureRepository
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FingerprintGestureViewModel(
    private val mRepository: FingerprintGestureRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    private val mFingerprintGestureMaps = combine(mRepository.swipeDown, mRepository.swipeUp) { swipeDown, swipeUp ->
        mapOf(
            FingerprintGestureUtils.SWIPE_DOWN to swipeDown,
            FingerprintGestureUtils.SWIPE_UP to swipeUp
        )
    }

    private val _models: MutableLiveData<State<List<FingerprintGestureMapListItemModel>>> = MutableLiveData(Loading())
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

    fun addAction(gestureId: String, action: Action) = viewModelScope.launch {
        mRepository.editGesture(gestureId) {
            val newActionList = it.actionList.toMutableList().apply {
                add(action)
            }

            it.copy(actionList = newActionList)
        }
    }

    fun removeAction(gestureId: String, actionId: String) = viewModelScope.launch {
        mRepository.editGesture(gestureId) {
            val newActionList = it.actionList.toMutableList().apply {
                removeAll { action ->
                    action.uid == actionId
                }
            }

            it.copy(actionList = newActionList)
        }
    }

    fun addConstraint(gestureId: String, constraint: Constraint) = viewModelScope.launch {
        val gestureMap = mFingerprintGestureMaps.firstOrNull()?.get(gestureId)

        if (gestureMap?.constraintList?.any { it.uniqueId == constraint.uniqueId } == true) {
            _eventStream.value = DuplicateConstraints()
            return@launch
        }

        mRepository.editGesture(gestureId) {
            val newConstraintList = it.constraintList.toMutableList().apply {
                add(constraint)
            }

            it.copy(constraintList = newConstraintList.toList())
        }
    }

    fun removeConstraint(gestureId: String, constraintId: String) = viewModelScope.launch {
        mRepository.editGesture(gestureId) {
            val newConstraintList = it.constraintList.toMutableList().apply {
                removeAll { constraint ->
                    constraint.uniqueId == constraintId
                }
            }

            it.copy(constraintList = newConstraintList)
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
            _eventStream.value = BuildFingerprintGestureModels(it)
        }
    }

    fun editOptions(gestureId: String) = viewModelScope.launch {
        mFingerprintGestureMaps.firstOrNull()?.get(gestureId)?.let {
            val options = FingerprintGestureMapOptions(gestureId, it)
            _eventStream.value = EditFingerprintGestureMapOptions(options)
        }
    }

    fun setOptions(options: FingerprintGestureMapOptions) = viewModelScope.launch {
        mFingerprintGestureMaps.firstOrNull()?.get(options.id)?.let {
            val newMap = options.apply(it)

            mRepository.editGesture(options.id) { newMap }
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