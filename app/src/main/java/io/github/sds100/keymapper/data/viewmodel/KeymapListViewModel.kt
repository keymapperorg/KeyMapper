package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.KeymapListUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.launch

class KeymapListViewModel internal constructor(
    private val mKeymapRepository: KeymapListUseCase,
    private val mDeviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    val keymapModelList: MutableLiveData<State<List<KeymapListItemModel>>> =
        MutableLiveData(Loading())

    val selectionProvider: ISelectionProvider = SelectionProvider()

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(mKeymapRepository.keymapList) {
            value = BuildKeymapListModels(it)
        }
    }

    val eventStream: LiveData<Event> = _eventStream

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

    fun enableSelectedKeymaps() {
        viewModelScope.launch {
            mKeymapRepository.enableKeymapById(*selectionProvider.selectedIds)
        }
    }

    fun disableSelectedKeymaps() {
        viewModelScope.launch {
            mKeymapRepository.disableKeymapById(*selectionProvider.selectedIds)
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

    fun rebuildModels() {
        if (mKeymapRepository.keymapList.value.isNullOrEmpty()) {
            keymapModelList.value = Empty()
            return
        }

        keymapModelList.value = Loading()

        _eventStream.value =
            BuildKeymapListModels(mKeymapRepository.keymapList.value ?: emptyList())
    }

    fun setModelList(list: List<KeymapListItemModel>) {
        selectionProvider.updateIds(list.map { it.id }.toLongArray())

        keymapModelList.value = when {
            list.isEmpty() -> Empty()
            else -> Data(list)
        }
    }

    fun requestBackupSelectedKeymaps() = run { _eventStream.value = RequestBackupSelectedKeymaps() }

    fun fixError(failure: Failure) {
        _eventStream.value = FixFailure(failure)
    }

    fun getSelectedKeymaps(): List<KeyMap> {
        mKeymapRepository.keymapList.value?.let { keymapList ->
            return selectionProvider.selectedIds.map { selectedId ->
                keymapList.single { it.id == selectedId }
            }
        }

        return emptyList()
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