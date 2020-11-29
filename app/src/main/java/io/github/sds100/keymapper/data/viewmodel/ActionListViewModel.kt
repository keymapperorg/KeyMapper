package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.ActionModel
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
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

    private val _testActionEvent = MutableSharedFlow<Action>()
    val testActionEvent = _testActionEvent.asSharedFlow()

    private val _promptToEnableAccessibilityServiceEvent = MutableSharedFlow<Unit>()
    val promptToEnableAccessibilityServiceEvent = _promptToEnableAccessibilityServiceEvent.asSharedFlow()

    private val _editActionOptionsEvent = MutableSharedFlow<O>()
    val editActionOptionsEvent = _editActionOptionsEvent.asSharedFlow()

    private val _buildModelsEvent = MutableSharedFlow<List<Action>>()
    val buildModelsEvent = _buildModelsEvent.asSharedFlow()

    private val _showFixPrompt = MutableSharedFlow<Failure>()
    val showFixPrompt = _showFixPrompt.asSharedFlow()

    init {
        mCoroutineScope.launch {
            actionList.asFlow().collect {
                _buildModelsEvent.emit(it)
            }
        }
    }

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
        mCoroutineScope.launch {
            val action = actionList.value?.singleOrNull { it.uid == id } ?: return@launch
            _editActionOptionsEvent.emit(getActionOptions(action))
        }
    }

    fun onModelClick(id: String) {
        mCoroutineScope.launch {
            modelList.value?.ifIsData { modelList ->
                modelList.singleOrNull { it.id == id }?.apply {
                    when {
                        hasError -> _showFixPrompt.emit(failure!!)
                        else -> {
                            val action = actionList.value?.singleOrNull { it.uid == id } ?: return@apply
                            _testActionEvent.emit(action)
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


    fun promptToEnableAccessibilityService() = mCoroutineScope.launch {
        _promptToEnableAccessibilityServiceEvent.emit(Unit)
    }

    fun rebuildModels() = mCoroutineScope.launch {
        _buildModelsEvent.emit(actionList.value ?: listOf())
    }

    fun invalidateOptions() {
        val newActionList = actionList.value?.map {
            getActionOptions(it).apply(it)
        }

        _actionList.value = newActionList ?: listOf()
    }

    suspend fun getDeviceInfoList() = mDeviceInfoRepository.getAll()

    abstract fun getActionOptions(action: Action): O
    open fun onAddAction(action: Action) {}
}