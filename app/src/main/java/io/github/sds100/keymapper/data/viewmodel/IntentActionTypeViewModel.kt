package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.IntentExtraListItemModel
import io.github.sds100.keymapper.data.model.IntentExtraModel
import io.github.sds100.keymapper.data.model.IntentExtraType
import io.github.sds100.keymapper.util.*

/**
 * Created by sds100 on 01/01/21.
 */
class IntentActionTypeViewModel : ViewModel() {
    val targetActivity = MutableLiveData(false)
    val targetBroadcastReceiver = MutableLiveData(false)
    val targetService = MutableLiveData(false)

    val description = MutableLiveData("")
    val action = MutableLiveData("")
    val categoriesString = MutableLiveData("")
    val categoriesList = categoriesString.map {
        it.split(',')
    }

    val data = MutableLiveData("")
    val targetPackage = MutableLiveData("")
    val targetClass = MutableLiveData("")

    private val _extras = MutableLiveData(emptyList<IntentExtraModel>())
    val extras: LiveData<List<IntentExtraModel>> = _extras

    private val _extrasListItemModels = MutableLiveData<State<List<IntentExtraListItemModel>>>(Empty())
    val extrasListItemModels: LiveData<State<List<IntentExtraListItemModel>>> = _extrasListItemModels

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(_extras) {
            value = BuildIntentExtraListItemModels(it)
        }
    }
    val eventStream: LiveData<Event> = _eventStream

    private val _isValid = MediatorLiveData<Boolean>().apply {

        fun invalidate() {
            if (description.value.isNullOrEmpty()) {
                value = false
                return
            }

            if (_extras.value?.any { !it.isValidValue || it.name.isEmpty() } == true) {
                value = false
                return
            }

            value = true
        }

        addSource(description) {
            invalidate()
        }

        addSource(_extras) {
            invalidate()
        }
    }

    val isValid: LiveData<Boolean> = _isValid

    init {
        //set broadcast receiver as the default target
        targetBroadcastReceiver.value = true
    }

    fun setExtraName(uid: String, name: String) {
        _extras.value = _extras.value?.map {
            if (it.uid == uid) {
                return@map it.copy(name = name)
            }

            it
        }
    }

    fun setExtraValue(uid: String, value: String) {
        _extras.value = _extras.value?.map {
            if (it.uid == uid) {
                return@map it.copy(value = value)
            }

            it
        }
    }

    fun removeExtra(uid: String) {
        _extras.value = _extras.value?.toMutableList()?.apply {
            removeAll { it.uid == uid }
        }
    }

    fun addExtra(type: IntentExtraType) {
        val model = IntentExtraModel(type = type)

        _extras.value = _extras.value?.toMutableList()?.apply {
            add(model)
        }
    }

    fun setListItemModels(models: List<IntentExtraListItemModel>) {
        if (models.isEmpty()) {
            _extrasListItemModels.value = Empty()
            return
        }

        _extrasListItemModels.value = Data(models)
    }

    fun getTarget(): IntentTarget = when {
        targetActivity.value == true -> IntentTarget.ACTIVITY
        targetBroadcastReceiver.value == true -> IntentTarget.BROADCAST_RECEIVER
        targetService.value == true -> IntentTarget.SERVICE
        else -> throw Exception("No target selected.")
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return IntentActionTypeViewModel() as T
        }
    }
}
