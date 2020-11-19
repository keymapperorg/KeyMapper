package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.KeymapListUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class KeymapListViewModel internal constructor(
    private val mKeymapRepository: KeymapListUseCase,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    val keymapModelList: MutableLiveData<State<List<KeymapListItemModel>>> = MutableLiveData(Loading())

    val rebuildModelsEvent = MediatorLiveData<Event<List<KeyMap>>>().apply {
        addSource(mKeymapRepository.keymapList) {
            this.value = Event(it)
        }
    }

    val selectionProvider: ISelectionProvider = SelectionProvider()

    val backupEvent = MutableLiveData<Event<Unit>>()

    private val _promptFix = MutableSharedFlow<Failure>()
    val promptFix = _promptFix.asSharedFlow()

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

    fun backup() {
        backupEvent.value = Event(Unit)
    }

    fun fixError(failure: Failure) = viewModelScope.launch {
        _promptFix.emit(failure)
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val mKeymapListUseCase: KeymapListUseCase,
        private val mDeviceInfoRepository: DeviceInfoRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeymapListViewModel(mKeymapListUseCase, mDeviceInfoRepository) as T
        }
    }
}