package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.*
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.IntentExtraListItemModel
import io.github.sds100.keymapper.data.model.IntentExtraModel
import io.github.sds100.keymapper.data.model.IntentExtraType
import io.github.sds100.keymapper.util.Event

/**
 * Created by sds100 on 01/01/21.
 */
class IntentActionTypeViewModel : ViewModel() {
    val targetActivity = MutableLiveData(false)
    val targetBroadcastReceiver = MutableLiveData(false)
    val targetService = MutableLiveData(false)

    val action = MutableLiveData("")
    val category = MutableLiveData("")
    val data = MutableLiveData("")
    val targetPackage = MutableLiveData("")
    val targetClass = MutableLiveData("")

    private val _isValid = MutableLiveData(false)
    val isValid: LiveData<Boolean> = _isValid

    private val mExtras = MutableLiveData(emptyList<IntentExtraModel>())

    val extrasListItemModels = mExtras.map { extras ->
        extras.map {
            it.toListItemModel()
        }
    }

    private val _eventStream = LiveEvent<Event>()
    val eventStream: LiveData<Event> = _eventStream

    private fun IntentExtraModel.toListItemModel(): IntentExtraListItemModel {
        return IntentExtraListItemModel(
            type.labelStringRes,
            name,
            value,
            type.isValid(value),
            type.exampleStringRes
        )
    }

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

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return IntentActionTypeViewModel() as T
        }
    }
}
