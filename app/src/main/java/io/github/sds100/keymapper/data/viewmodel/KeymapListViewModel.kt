package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeymapListViewModel internal constructor(
    private val repository: KeymapRepository
) : ViewModel(), ProgressCallback {

    val keymapModelList = MediatorLiveData<List<KeymapListItemModel>>().apply {
        addSource(repository.keymapList) { keymapList ->

            viewModelScope.launch {
                loadingContent.value = true

                val modelList = buildModelList(keymapList)
                selectionProvider.updateIds(keymapList.map { it.id }.toLongArray())

                value = modelList

                loadingContent.value = false
            }
        }
    }


    val selectionProvider: ISelectionProvider = SelectionProvider()

    override val loadingContent = MutableLiveData(true)

    fun delete(vararg id: Long) {
        viewModelScope.launch {
            repository.deleteKeymap(*id)
        }
    }

    fun enableKeymaps(vararg id: Long) {
        viewModelScope.launch {
            repository.enableKeymapById(*id)
        }
    }

    fun disableKeymaps(vararg id: Long) {
        viewModelScope.launch {
            repository.disableKeymapById(*id)
        }
    }

    fun rebuildModels() = viewModelScope.launch {
        if (repository.keymapList.value.isNullOrEmpty()) return@launch

        keymapModelList.value = buildModelList(repository.keymapList.value ?: listOf())
    }

    private suspend fun buildModelList(keymapList: List<KeyMap>) =
        withContext(viewModelScope.coroutineContext + Dispatchers.Default) {
            keymapList.map {
                KeymapListItemModel(
                    id = it.id,
                    actionList = it.buildActionChipModels(),
                    triggerDescription = it.trigger.buildDescription(),
                    constraintList = it.buildConstraintModels(),
                    constraintMode = it.constraintMode,
                    flagsDescription = it.flags.buildKeymapFlagsDescription(),
                    isEnabled = it.isEnabled
                )
            }
        }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mRepository: KeymapRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeymapListViewModel(mRepository) as T
        }
    }
}