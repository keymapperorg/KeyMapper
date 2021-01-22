package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.CreateKeymapShortcutUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.launch
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 08/09/20.
 */
class CreateKeymapShortcutViewModel(
    private val keymapRepository: CreateKeymapShortcutUseCase,
    private val deviceInfoRepository: DeviceInfoRepository
) : ViewModel() {

    private val _model: MutableLiveData<State<List<KeymapListItemModel>>> =
        MutableLiveData(Loading())
    val model: LiveData<State<List<KeymapListItemModel>>> = _model

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(keymapRepository.keymapList) {
            postValue(BuildKeymapListModels(it))
        }
    }

    val eventStream: LiveData<Event> = _eventStream

    fun rebuildModels() {
        if (keymapRepository.keymapList.value == null) return

        if (keymapRepository.keymapList.value?.isEmpty() == true) {
            _model.value = Empty()
            return
        }

        _model.value = Loading()

        _eventStream.value =
            BuildKeymapListModels(keymapRepository.keymapList.value ?: emptyList())
    }

    fun setModelList(list: List<KeymapListItemModel>) {
        _model.value = when {
            list.isEmpty() -> Empty()
            else -> Data(list)
        }
    }

    fun chooseKeymap(uid: String) {
        keymapRepository.keymapList.value
            ?.find { it.uid == uid }
            ?.let {
                val newTriggerFlags =
                    it.trigger.flags.withFlag(Trigger.TRIGGER_FLAG_FROM_OTHER_APPS)

                val newKeymap = it.copy(trigger = it.trigger.copy(flags = newTriggerFlags))

                viewModelScope.launch {
                    keymapRepository.updateKeymap(newKeymap)

                    _eventStream.value = CreateKeymapShortcutEvent(uid, newKeymap.actionList)
                }
            }
    }

    fun fixError(failure: Failure) {
        _eventStream.value = FixFailure(failure)
    }

    suspend fun getDeviceInfoList() = deviceInfoRepository.getAll()

    class Factory(
        private val keymapRepository: CreateKeymapShortcutUseCase,
        private val deviceInfoRepository: DeviceInfoRepository) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            CreateKeymapShortcutViewModel(keymapRepository, deviceInfoRepository) as T
    }
}