package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.IPreferenceDataStore
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.behavior.BaseOptions
import io.github.sds100.keymapper.data.model.behavior.KeymapActionOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.data.usecase.ConfigKeymapUseCase
import io.github.sds100.keymapper.util.InvalidateOptionsCallback
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 22/11/20.
 */

class ConfigKeymapViewModel(private val mKeymapRepository: ConfigKeymapUseCase,
                            private val mDeviceInfoRepository: DeviceInfoRepository,
                            preferenceDataStore: IPreferenceDataStore,
                            private val mId: Long
) : ViewModel(), IPreferenceDataStore by preferenceDataStore, InvalidateOptionsCallback {

    companion object {
        const val NEW_KEYMAP_ID = -2L
    }

    val actionListViewModel = object : ActionListViewModel(viewModelScope, mDeviceInfoRepository, this) {
        override fun getActionOptions(action: Action): BaseOptions<Action> {
            //TODO use values from trigger viewmodel
            return KeymapActionOptions(
                action,
                actionList.value!!.size,
                Trigger.DEFAULT_TRIGGER_MODE,
                listOf()
            )
        }
    }

    val isEnabled = MutableLiveData<Boolean>()

    init {
        if (mId == NEW_KEYMAP_ID) {
            actionListViewModel.setActionList(listOf())
            isEnabled.value = true

        } else {
            viewModelScope.launch {
                val keymap = mKeymapRepository.getKeymap(mId)

                isEnabled.value = keymap.isEnabled

                actionListViewModel.setActionList(keymap.actionList)
            }
        }
    }

    override fun invalidateOptions() {

    }

    class Factory(
        private val mConfigKeymapUseCase: ConfigKeymapUseCase,
        private val mDeviceInfoRepository: DeviceInfoRepository,
        private val mIPreferenceDataStore: IPreferenceDataStore,
        private val mId: Long) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigKeymapViewModel(mConfigKeymapUseCase, mDeviceInfoRepository, mIPreferenceDataStore, mId) as T
    }
}