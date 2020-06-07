package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.DeviceInfoRepository
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.ISelectionProvider
import io.github.sds100.keymapper.util.SelectionProvider
import kotlinx.coroutines.launch

class KeymapListViewModel internal constructor(
    private val mKeymapRepository: KeymapRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel(), ProgressCallback {

    val keymapModelList = MutableLiveData(listOf<KeymapListItemModel>())

    val rebuildModelsEvent = MediatorLiveData<Event<List<KeyMap>>>().apply {
        addSource(mKeymapRepository.keymapList) {
            this.value = Event(it)
        }
    }

    val selectionProvider: ISelectionProvider = SelectionProvider()

    override val loadingContent = MutableLiveData(true)

    fun delete(vararg id: Long) {
        viewModelScope.launch {
            mKeymapRepository.deleteKeymap(*id)
        }
    }

    fun enableKeymaps(vararg id: Long) {
        viewModelScope.launch {
            mKeymapRepository.enableKeymapById(*id)
        }
    }

    fun disableKeymaps(vararg id: Long) {
        viewModelScope.launch {
            mKeymapRepository.disableKeymapById(*id)
        }
    }

    fun enableAll() {
        viewModelScope.launch {
            mKeymapRepository.enableAll()
        }
    }

    fun disableAll() {
        viewModelScope.launch {
            mKeymapRepository.disableAll()
        }
    }

    fun rebuildModels() = viewModelScope.launch {
        if (mKeymapRepository.keymapList.value.isNullOrEmpty()) return@launch

        rebuildModelsEvent.value = Event(mKeymapRepository.keymapList.value ?: listOf())
    }

    fun setModelList(list: List<KeymapListItemModel>) {
        loadingContent.value = true

        selectionProvider.updateIds(list.map { it.id }.toLongArray())

        keymapModelList.value = list

        loadingContent.value = false
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mKeymapRepository: KeymapRepository,
        private val mDeviceInfoRepository: DeviceInfoRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeymapListViewModel(mKeymapRepository, mDeviceInfoRepository) as T
        }
    }
}