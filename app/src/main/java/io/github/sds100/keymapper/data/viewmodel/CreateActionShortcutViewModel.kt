package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.ActionBehavior
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.dataExtraString
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 08/09/20.
 */
class CreateActionShortcutViewModel : ViewModel() {

    val actionList = MutableLiveData(listOf<Action>())

    val chooseActionEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val testAction: MutableLiveData<Event<Action>> = MutableLiveData()
    val chooseActionBehavior: MutableLiveData<Event<ActionBehavior>> = MutableLiveData()
    val duplicateActionsEvent: MutableLiveData<Event<Unit>> = MutableLiveData()
    val showFixPrompt: MutableLiveData<Event<Failure>> = MutableLiveData()
    val promptToEnableAccessibilityService: MutableLiveData<Event<Unit>> = MutableLiveData()

    fun chooseAction() {
        chooseActionEvent.value = Event(Unit)
    }

    /**
     * @return whether the action already exists has been added to the list
     */
    fun addAction(action: Action) {
        if (actionList.value?.find {
                it.type == action.type && it.data == action.data && it.dataExtraString == action.dataExtraString
            } != null) {
            duplicateActionsEvent.value = Event(Unit)
            return
        }

        actionList.value = actionList.value?.toMutableList()?.apply {
            add(action)
        }
    }

    fun setActionBehavior(actionBehavior: ActionBehavior) {
        actionList.value = actionList.value?.map {

            if (it.uniqueId == actionBehavior.actionId) {
                return@map actionBehavior.applyToAction(it)
            }

            it
        }
    }

    fun onActionModelClick(model: ActionModel) {
        if (model.hasError) {
            showFixPrompt.value = Event(model.failure!!)
        } else {
            if (model.hasError) {
                showFixPrompt.value = Event(model.failure!!)
            } else {
                actionList.value?.single { it.uniqueId == model.id }?.let {
                    testAction.value = Event(it)
                }
            }
        }
    }

    fun removeAction(id: String) {
        actionList.value = actionList.value?.toMutableList()?.apply {
            removeAll { it.uniqueId == id }
        }
    }

    fun chooseActionBehavior(id: String) {
        val action = actionList.value?.find { it.uniqueId == id } ?: return
        val behavior = ActionBehavior(action, actionList.value!!.size)

        chooseActionBehavior.value = Event(behavior)
    }

    fun rebuildActionModels() {
        actionList.value = actionList.value
    }

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = CreateActionShortcutViewModel() as T
    }
}