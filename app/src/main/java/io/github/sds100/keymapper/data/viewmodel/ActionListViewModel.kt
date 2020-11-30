package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by sds100 on 22/11/20.
 */

abstract class ActionListViewModel<O : BaseOptions<Action>>(
    private val mCoroutineScope: CoroutineScope,
    private val mDeviceInfoRepository: DeviceInfoRepository) {

    private val _actionList = MutableLiveData<List<Action>>(listOf())
    val actionList: LiveData<List<Action>> = _actionList

    private val _modelList = MutableLiveData<State<List<ActionModel>>>(Loading())
    val modelList: LiveData<State<List<ActionModel>>> = _modelList

    private val _eventStream = LiveEvent<SealedEvent>().apply {
        addSource(actionList) {
            value = BuildActionListModels(it ?: listOf())
        }
    }

    val eventStream: LiveData<SealedEvent> = _eventStream

    fun setActionList(actionList: List<Action>) {
        _actionList.value = actionList
    }

    fun setModels(modelList: List<ActionModel>) {
        when {
            modelList.isEmpty() -> _modelList.value = Empty()
            else -> _modelList.value = Data(modelList)
        }
    }

    fun addAction(action: Action) {
        _actionList.value = _actionList.value?.toMutableList()?.apply {
            add(action)
        }

        invalidateOptions()

        onAddAction(action)
    }

    fun moveAction(fromIndex: Int, toIndex: Int) {
        _actionList.value = actionList.value?.toMutableList()?.apply {
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

        invalidateOptions()
    }

    fun removeAction(id: String) {
        _actionList.value = _actionList.value?.toMutableList()?.apply {
            removeAll { it.uid == id }
        }

        invalidateOptions()
    }

    fun editOptions(id: String) {
        val action = actionList.value?.singleOrNull { it.uid == id } ?: return
        _eventStream.value = EditActionOptions(getActionOptions(action))
    }

    fun onModelClick(id: String) {
        mCoroutineScope.launch {
            modelList.value?.ifIsData { modelList ->
                modelList.singleOrNull { it.id == id }?.apply {
                    when {
                        hasError -> _eventStream.value = FixFailure(failure!!)

                        else -> {
                            val action = actionList.value?.singleOrNull { it.uid == id } ?: return@apply
                            _eventStream.value = TestAction(action)
                        }
                    }
                }
            }
        }
    }

    fun setOptions(options: O) {
        _actionList.value = actionList.value?.map {
            if (it.uid == options.id) {
                return@map options.apply(it)
            }

            it
        }

        invalidateOptions()
    }

    fun promptToEnableAccessibilityService() {
        _eventStream.value = EnableAccessibilityServicePrompt()
    }

    fun rebuildModels() {
        _eventStream.value = BuildActionListModels(actionList.value ?: emptyList())
    }

    fun invalidateOptions() {
        val newActionList = actionList.value?.map {
            getActionOptions(it).apply(it)
        }

        _actionList.value = newActionList ?: emptyList()
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    abstract fun getActionOptions(action: Action): O
    open fun onAddAction(action: Action) {}
}