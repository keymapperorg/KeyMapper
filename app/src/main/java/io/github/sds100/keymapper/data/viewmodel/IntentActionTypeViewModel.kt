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

    //set broadcast receiver as the default target
    val targetBroadcastReceiver = MutableLiveData(true)
    val targetService = MutableLiveData(false)

    val description = MutableLiveData("")
    val action = MutableLiveData("")
    val category = MutableLiveData("")
    val data = MutableLiveData("")
    val targetPackage = MutableLiveData("")
    val targetClass = MutableLiveData("")

    private val _isValid = MediatorLiveData<Boolean>().apply {

        fun invalidate() {
            if (description.value.isNullOrEmpty()) {
                value = false
                return
            }

            value = true
        }

        addSource(description) {
            invalidate()
        }
    }

    val isValid: LiveData<Boolean> = _isValid

    private val mExtras = MutableLiveData(emptyList<IntentExtraModel>())

    private val _extrasListItemModels = MutableLiveData<State<List<IntentExtraListItemModel>>>(Empty())
    val extrasListItemModels: LiveData<State<List<IntentExtraListItemModel>>> = _extrasListItemModels

    private val _eventStream = LiveEvent<Event>().apply {
        addSource(mExtras) {
            value = BuildIntentExtraListItemModels(it)
        }
    }
    val eventStream: LiveData<Event> = _eventStream

    fun setExtraName(uid: String, name: String) {
        mExtras.value = mExtras.value?.map {
            if (it.uid == uid) {
                return@map it.copy(name = name)
            }

            it
        }
    }

    fun setExtraValue(uid: String, value: String) {
        mExtras.value = mExtras.value?.map {
            if (it.uid == uid) {
                return@map it.copy(value = value)
            }

            it
        }
    }

    fun removeExtra(uid: String) {
        mExtras.value = mExtras.value?.toMutableList()?.apply {
            removeAll { it.uid == uid }
        }
    }

    fun addExtra(type: IntentExtraType) {
        val model = IntentExtraModel(type = type)

        mExtras.value = mExtras.value?.toMutableList()?.apply {
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

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return IntentActionTypeViewModel() as T
        }
    }
}
