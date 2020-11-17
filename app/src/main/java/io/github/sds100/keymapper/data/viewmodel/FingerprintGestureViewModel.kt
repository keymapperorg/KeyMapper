package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.FingerprintGestureMap
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.repository.FingerprintGestureRepository
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FingerprintGestureViewModel(private val mRepository: FingerprintGestureRepository) : ViewModel() {

    val models = MutableLiveData<State<List<FingerprintGestureMapListItemModel>>>()

    val buildModels = MutableLiveData<Event<Map<String, FingerprintGestureMap>>>()

    init {
        rebuildModels()
    }

    fun setModels(models: List<FingerprintGestureMapListItemModel>) {
        this.models.value = Data(models)
    }

    fun setAction(id: String, action: Action) {
        mRepository.edit(id) {
            it.clone(action = action)
        }

        rebuildModels()
    }

    fun removeAction(id: String) {
        mRepository.edit(id) {
            it.clone(action = null)
        }

        rebuildModels()
    }

    fun setEnabled(id: String, isEnabled: Boolean) {
        mRepository.edit(id) {
            it.clone(isEnabled = isEnabled)
        }

        rebuildModels()
    }

    fun rebuildModels() {
        models.value = Loading()

        viewModelScope.launch(Dispatchers.IO) {
            val gestureMaps = FingerprintGestureUtils.GESTURES.map {
                it to (mRepository.get(FingerprintGestureUtils.PREF_KEYS[it]!!)
                    ?: FingerprintGestureMap())
            }.toMap()

            withContext(Dispatchers.Main) {
                buildModels.value = Event(gestureMaps)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val mRepository: FingerprintGestureRepository) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return FingerprintGestureViewModel(mRepository) as T
        }
    }
}