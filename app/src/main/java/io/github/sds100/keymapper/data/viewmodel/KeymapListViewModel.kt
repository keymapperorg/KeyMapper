package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.launch

class KeymapListViewModel internal constructor(
    private val repository: KeymapRepository
) : ViewModel(), ProgressCallback {

    val keymapModelList: LiveData<List<KeymapListItemModel>> =
        Transformations.map(repository.keymapList) { keymapList ->
            loadingContent.value = true

            val modelList = keymapList?.map {
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

            selectionProvider.updateIds(keymapList.map { it.id }.toLongArray())

            loadingContent.value = false

            modelList
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

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mRepository: KeymapRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeymapListViewModel(mRepository) as T
        }
    }
}