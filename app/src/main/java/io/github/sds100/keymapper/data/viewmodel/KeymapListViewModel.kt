package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.ISelectionProvider
import io.github.sds100.keymapper.util.SelectionProvider
import io.github.sds100.keymapper.util.buildActionModels
import kotlinx.coroutines.launch

class KeymapListViewModel internal constructor(
    private val repository: KeymapRepository
) : ViewModel(), ProgressCallback {

    val keymapList: LiveData<List<KeymapListItemModel>> =
        Transformations.map(repository.keymapList) { keyMapList ->
            loadingContent.value = true

            val keymapList = keyMapList?.map {
                KeymapListItemModel(it.id, it.buildActionModels(), it.isEnabled)
            } ?: listOf()

            selectionProvider.updateIds(keymapList.map { it.id }.toLongArray())

            loadingContent.value = false

            keymapList
        }

    val selectionProvider: ISelectionProvider = SelectionProvider()

    override val loadingContent = MutableLiveData(true)

    fun delete(vararg id: Long) {
        viewModelScope.launch {
            repository.deleteKeymap(*id)
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