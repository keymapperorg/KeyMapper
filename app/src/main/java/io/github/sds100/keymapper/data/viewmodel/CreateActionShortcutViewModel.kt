package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.data.model.behavior.ActionBehavior
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 08/09/20.
 */
class CreateActionShortcutViewModel(private val mDeviceInfoRepository: DeviceInfoRepository) : ViewModel() {

    val actionList = MutableLiveData(listOf<Action>())
    val actionModelList = MutableLiveData<List<ActionModel>>()
    val buildActionModelList = actionList.map { Event(it) }

    val chooseActionEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val testAction: MutableLiveData<Event<Action>> = MutableLiveData()
    val chooseActionBehavior: MutableLiveData<Event<ActionBehavior>> = MutableLiveData()
    val showFixPrompt: MutableLiveData<Event<Failure>> = MutableLiveData()
    val promptToEnableAccessibilityService: MutableLiveData<Event<Unit>> = MutableLiveData()

    fun chooseAction() {
        chooseActionEvent.value = Event(Unit)
    }

    /**
     * @return whether the action already exists has been added to the list
     */
    fun addAction(action: Action) {
        actionList.value = actionList.value?.toMutableList()?.apply {
            add(action)
        }
    }

    fun setActionBehavior(actionBehavior: ActionBehavior) {
        actionList.value = actionList.value?.map {

            if (it.uid == actionBehavior.id) {
                return@map actionBehavior.applyToAction(it)
            }

            it
        }
    }

    fun onActionModelClick(id: String) {
        val model = actionModelList.value?.find { it.id == id } ?: return

        if (model.hasError) {
            showFixPrompt.value = Event(model.failure!!)
        } else {
            if (model.hasError) {
                showFixPrompt.value = Event(model.failure!!)
            } else {
                actionList.value?.find { it.uid == id }?.let {
                    testAction.value = Event(it)
                }
            }
        }
    }

    fun removeAction(id: String) {
        actionList.value = actionList.value?.toMutableList()?.apply {
            removeAll { it.uid == id }
        }
    }

    fun chooseActionBehavior(id: String) {
        val action = actionList.value?.find { it.uid == id } ?: return
        val behavior = ActionBehavior(action, actionList.value!!.size)

        chooseActionBehavior.value = Event(behavior)
    }

    fun setActionModels(models: List<ActionModel>) {
        actionModelList.value = models
    }

    fun rebuildActionModels() {
        actionList.value = actionList.value
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    class Factory(private val mDeviceInfoRepository: DeviceInfoRepository) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            CreateActionShortcutViewModel(mDeviceInfoRepository) as T
    }
}