package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.model.behavior.FingerprintGestureMapOptions
import io.github.sds100.keymapper.data.repository.FingerprintGestureRepository
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.FingerprintGestureUtils
import io.github.sds100.keymapper.util.Loading
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FingerprintGestureViewModel(private val mRepository: FingerprintGestureRepository) : ViewModel() {

    private val mFingerprintGestureMaps = combine(mRepository.swipeDown, mRepository.swipeUp) { swipeDown, swipeUp ->
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

    fun setAction(id: String, action: Action) {
        viewModelScope.launch {
            mRepository.edit(id) {
                it.clone(action = action)
            }
        }
    }

    fun removeAction(id: String) = viewModelScope.launch {
        mRepository.edit(id) {
            it.clone(action = null)
        }
    }

    fun setEnabled(id: String, isEnabled: Boolean) = viewModelScope.launch {
        mRepository.edit(id) {
            it.clone(isEnabled = isEnabled)
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

            mRepository.edit(options.gestureId) { newMap }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mRepository: FingerprintGestureRepository) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(mRepository) as T
        }
    }
}