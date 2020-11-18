package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
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

    private val _models: MutableStateFlow<State<List<FingerprintGestureMapListItemModel>>> = MutableStateFlow(Loading())
    val models = _models.asStateFlow()

    private val _buildModels: MutableSharedFlow<Map<String, FingerprintGestureMap>> = MutableSharedFlow()
    val buildModels: SharedFlow<Map<String, FingerprintGestureMap>> = _buildModels

    init {
        viewModelScope.launch {
            mFingerprintGestureMaps.collect {
                _buildModels.emit(it)
            }
        }
    }

    fun setModels(models: List<FingerprintGestureMapListItemModel>) = viewModelScope.launch {
        _models.emit(Data(models))
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
        _models.emit(Loading())

        mFingerprintGestureMaps.firstOrNull()?.let {
            _buildModels.emit(it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mRepository: FingerprintGestureRepository) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(mRepository) as T
        }
    }
}