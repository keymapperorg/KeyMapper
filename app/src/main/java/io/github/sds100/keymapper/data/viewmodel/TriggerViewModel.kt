package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.IDataStoreManager
import io.github.sds100.keymapper.data.model.DeviceInfo
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.TriggerKeyModel
import io.github.sds100.keymapper.data.model.options.TriggerKeyOptions
import io.github.sds100.keymapper.data.model.options.TriggerOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.util.*
import java.util.*

/**
 * Created by sds100 on 24/11/20.
 */

class TriggerViewModel(
    private val deviceInfoRepository: DeviceInfoRepository,
    dataStoreManager: IDataStoreManager,
    keymapUid: LiveData<String>
) : IDataStoreManager by dataStoreManager {

    val optionsViewModel = TriggerOptionsViewModel(
        dataStoreManager,
        getTriggerKeys = { keys.value ?: emptyList() },
        getTriggerMode = { mode.value ?: Trigger.DEFAULT_TRIGGER_MODE },
        keymapUid
    )

    private val _keys = MutableLiveData<List<Trigger.Key>>()
    val keys: LiveData<List<Trigger.Key>> = _keys

    val triggerInParallel: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerInSequence: MutableLiveData<Boolean> = MutableLiveData(false)
    val triggerModeUndefined: MutableLiveData<Boolean> = MutableLiveData(false)

    val mode: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(triggerInParallel) { newValue ->
            if (newValue == true) {

                /* when the user first chooses to make parallel a trigger, show a dialog informing them that
                the order in which they list the keys is the order in which they will need to be held down.
                 */
                if (keys.value?.size!! > 1 &&
                    !getBoolPref(R.string.key_pref_shown_parallel_trigger_order_dialog)) {

                    notifyUser(R.string.dialog_message_parallel_trigger_order) {
                        setBoolPref(R.string.key_pref_shown_parallel_trigger_order_dialog, true)
                    }
                }

                if (value != Trigger.PARALLEL) {
                    keys.value?.let { oldKeys ->
                        var newKeys = oldKeys.toMutableList()

                        if (newKeys.isEmpty()) {
                            return@let
                        }

                        // set all the keys to a short press if coming from a non-parallel trigger
                        // because they must all be the same click type and can't all be double pressed
                        newKeys = newKeys.map { key ->
                            key.copy(clickType = Trigger.SHORT_PRESS)
                        }.toMutableList()

                        //remove duplicates of keys that have the same keycode and device id
                        newKeys = newKeys.distinctBy { Pair(it.keyCode, it.deviceId) }.toMutableList()

                        _keys.value = newKeys
                    }
                }

                value = Trigger.PARALLEL
            }
        }

        addSource(triggerInSequence) {
            if (it == true) {
                value = Trigger.SEQUENCE

                if (_keys.value?.size!! > 1 &&
                    !getBoolPref(R.string.key_pref_shown_sequence_trigger_explanation_dialog)) {

                    notifyUser(R.string.dialog_message_sequence_trigger_explanation) {
                        setBoolPref(R.string.key_pref_shown_sequence_trigger_explanation_dialog, true)
                    }
                }
            }
        }

        addSource(triggerModeUndefined) {
            if (it == true) {
                value = Trigger.UNDEFINED

                triggerInSequence.value = false
                triggerInParallel.value = false
            }
        }

        observeForever {
            optionsViewModel.invalidateOptions()
        }
    }

    val isParallelTriggerClickTypeShortPress = _keys.map {
        if (!it.isNullOrEmpty()) {
            it[0].clickType == Trigger.SHORT_PRESS
        } else {
            false
        }
    }

    val isParallelTriggerClickTypeLongPress = _keys.map {
        if (!it.isNullOrEmpty()) {
            it[0].clickType == Trigger.LONG_PRESS
        } else {
            false
        }
    }

    private val _modelList = MutableLiveData<DataState<List<TriggerKeyModel>>>()
    val modelList: LiveData<DataState<List<TriggerKeyModel>>> = _modelList

    val triggerKeyCount = modelList.map {
        when (it) {
            is Data -> it.data.size
            else -> 0
        }
    }

    val recordTriggerTimeLeft = MutableLiveData(0)
    val recordingTrigger = MutableLiveData(false)

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(keys) {
            value = BuildTriggerKeyModels(it ?: listOf())
        }

        addSource(optionsViewModel.eventStream) {
            value = it
        }
    }

    val eventStream: LiveData<Event> = _eventStream

    fun setTrigger(trigger: Trigger) {
        _keys.value = trigger.keys

        optionsViewModel.setOptions(TriggerOptions(trigger))

        when (trigger.mode) {
            Trigger.PARALLEL -> {
                triggerInParallel.value = true
                triggerInSequence.value = false
                triggerModeUndefined.value = false
            }

            Trigger.SEQUENCE -> {
                triggerInSequence.value = true
                triggerInParallel.value = false
                triggerModeUndefined.value = false
            }

            Trigger.UNDEFINED -> {
                triggerInSequence.value = false
                triggerInParallel.value = false
                triggerModeUndefined.value = true
            }
        }
    }

    fun createTrigger(): Trigger? {
        return optionsViewModel.options.value?.apply(
            Trigger(
                keys = keys.value ?: listOf(),
                mode = mode.value ?: Trigger.DEFAULT_TRIGGER_MODE
            ))
    }

    fun setParallelTriggerClickType(@Trigger.ClickType clickType: Int) {
        _keys.value = keys.value?.map {
            it.copy(clickType = clickType)
        }

        optionsViewModel.invalidateOptions()
    }

    fun setTriggerKeyDevice(uid: String, descriptor: String) {
        _keys.value = keys.value?.map {
            if (it.uid == uid) {
                return@map it.copy(deviceId = descriptor)
            }

            it
        }

        optionsViewModel.invalidateOptions()
    }

    /**
     * @return whether the key already exists has been added to the list
     */
    suspend fun addTriggerKey(keyCode: Int, deviceDescriptor: String, deviceName: String, isExternal: Boolean): Boolean {
        deviceInfoRepository.insertDeviceInfo(DeviceInfo(deviceDescriptor, deviceName))

        var clickType = Trigger.SHORT_PRESS

        val deviceId = if (isExternal) {
            deviceDescriptor
        } else {
            Trigger.Key.DEVICE_ID_THIS_DEVICE
        }

        val containsKey = keys.value?.any {
            if (mode.value != Trigger.SEQUENCE) {
                val sameKeyCode = keyCode == it.keyCode

                //if the key is not external, check whether a trigger key already exists for this device
                val sameDeviceId = if (
                    (it.deviceId == Trigger.Key.DEVICE_ID_THIS_DEVICE
                        || it.deviceId == Trigger.Key.DEVICE_ID_ANY_DEVICE)
                    && !isExternal) {
                    true

                } else {
                    it.deviceId == deviceDescriptor
                }

                sameKeyCode && sameDeviceId

            } else {
                false
            }
        } ?: false

        if (containsKey) {
            triggerInSequence.value = true

            if (!getBoolPref(R.string.key_pref_shown_multiple_of_same_key_in_sequence_trigger_info)) {
                notifyUser(R.string.dialog_message_use_key_multiple_times_in_sequence_trigger)

                setBoolPref(R.string.key_pref_shown_multiple_of_same_key_in_sequence_trigger_info,
                    true)
            }
        }

        /*
        multiple of the same key from the same device in a sequence trigger must all have the same click type.
        Therefore, set the click type of the new key to the same as the other keys.
         */
        if (triggerInSequence.value == true) {
            val keysWithSameKeycodeAndDevice = keys.value?.filter {
                it.keyCode == keyCode && it.deviceId == deviceId
            } ?: listOf()

            if (keysWithSameKeycodeAndDevice.isNotEmpty()) {
                clickType = keysWithSameKeycodeAndDevice[0].clickType

                if (!getBoolPref(
                        R.string.key_pref_shown_multiple_of_same_key_in_sequence_trigger_info)) {

                    notifyUser(R.string.dialog_message_use_key_multiple_times_in_sequence_trigger)

                    setBoolPref(
                        R.string.key_pref_shown_multiple_of_same_key_in_sequence_trigger_info,
                        true
                    )
                }
            }
        }

        _keys.value = keys.value?.toMutableList()?.apply {

            val triggerKey = Trigger.Key(keyCode, deviceId, clickType)
            add(triggerKey)
        }

        if (keys.value!!.size <= 1) {
            triggerModeUndefined.value = true
        }

        /* Automatically make it a parallel trigger when the user makes a trigger with more than one key
        because this is what most users are expecting when they make a trigger with multiple keys */
        if (keys.value!!.size == 2 && !containsKey) {
            triggerInParallel.value = true
        }

        if (keyCode == KeyEvent.KEYCODE_CAPS_LOCK) {
            _eventStream.value = EnableCapsLockKeyboardLayoutPrompt()
        }

        optionsViewModel.invalidateOptions()

        return true
    }

    fun removeTriggerKey(uid: String) {
        _keys.value = keys.value?.toMutableList()?.apply {
            removeAll { it.uid == uid }
        }

        if (keys.value!!.size <= 1) {
            triggerModeUndefined.value = true
        }

        optionsViewModel.invalidateOptions()
    }

    fun moveTriggerKey(fromIndex: Int, toIndex: Int) {
        _keys.value = keys.value?.toMutableList()?.apply {
            if (fromIndex < toIndex) {
                for (i in fromIndex until toIndex) {
                    Collections.swap(this, i, i + 1)
                }
            } else {
                for (i in fromIndex downTo toIndex + 1) {
                    Collections.swap(this, i, i - 1)
                }
            }
        }

        optionsViewModel.invalidateOptions()
    }

    fun editTriggerKeyOptions(id: String) {
        val key = keys.value?.find { it.uid == id } ?: return

        val options = TriggerKeyOptions(key, mode.value!!)

        _eventStream.value = EditTriggerKeyOptions(options)
    }

    fun setTriggerKeyOptions(options: TriggerKeyOptions) {
        val newTrigger = options.apply(createTrigger() ?: return)
        setTrigger(newTrigger)
    }

    fun recordTrigger() {
        if (!recordingTrigger.value!!) {
            _eventStream.value = StartRecordingTriggerInService()
        }
    }

    fun stopRecording() {
        if (recordingTrigger.value == true) {
            _eventStream.value = StopRecordingTriggerInService()
        }
    }

    fun rebuildModels() {
        _eventStream.value = BuildTriggerKeyModels(keys.value ?: emptyList())
    }

    fun setModelList(models: List<TriggerKeyModel>) {
        _modelList.value = when {
            models.isEmpty() -> Empty()
            else -> Data(models)
        }
    }

    fun promptToEnableAccessibilityService() {
        _eventStream.value = EnableAccessibilityServicePrompt()
    }

    private fun notifyUser(@StringRes message: Int, onOk: () -> Unit = {}) {
        _eventStream.value = OkDialog(message, onOk)
    }

    suspend fun getDeviceInfoList() = deviceInfoRepository.getAll()
}