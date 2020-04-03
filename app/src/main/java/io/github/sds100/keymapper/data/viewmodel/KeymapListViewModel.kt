package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.launch

class KeymapListViewModel internal constructor(
    private val repository: KeymapRepository
) : ViewModel(), ProgressCallback {

    val keymapModelList = MediatorLiveData<List<KeymapListItemModel>>().apply {
        addSource(repository.keymapList) { keymapList ->
            loadingContent.value = true

            val modelList = buildModelList(keymapList)
            selectionProvider.updateIds(keymapList.map { it.id }.toLongArray())

            value = modelList

            loadingContent.value = false
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

    fun rebuildModels() {
        if (repository.keymapList.value.isNullOrEmpty()) return

        loadingContent.value = true
        keymapModelList.value = buildModelList(repository.keymapList.value ?: listOf())
        loadingContent.value = false
    }

    private fun buildModelList(keymapList: List<KeyMap>) = keymapList.map {
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


    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mRepository: KeymapRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeymapListViewModel(mRepository) as T
        }
    }
}