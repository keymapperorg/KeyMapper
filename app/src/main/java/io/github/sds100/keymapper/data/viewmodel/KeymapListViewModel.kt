package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.repository.KeymapRepository
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.launch

class KeymapListViewModel internal constructor(
    private val mKeymapRepository: KeymapRepository,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    val keymapModelList: MutableLiveData<State<List<KeymapListItemModel>>> = MutableLiveData(Loading())

    val rebuildModelsEvent = MediatorLiveData<Event<List<KeyMap>>>().apply {
        addSource(mKeymapRepository.keymapList) {
            this.value = Event(it)
        }
    }

    val selectionProvider: ISelectionProvider = SelectionProvider()

    fun duplicate(vararg id: Long) {
        viewModelScope.launch {
            mKeymapRepository.duplicateKeymap(*id)
        }
    }

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
        keymapModelList.value = Loading()

        rebuildModelsEvent.value = Event(mKeymapRepository.keymapList.value ?: listOf())
    }

    fun setModelList(list: List<KeymapListItemModel>) {
        selectionProvider.updateIds(list.map { it.id }.toLongArray())

        keymapModelList.value = when {
            list.isEmpty() -> Empty()
            else -> Data(list)
        }
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