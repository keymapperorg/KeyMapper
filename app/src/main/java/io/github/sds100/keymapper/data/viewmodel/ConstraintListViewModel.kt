package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.ConstraintModel
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.Empty
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 29/11/20.
 */

class ConstraintListViewModel(private val mCoroutineScope: CoroutineScope) {

    private val _constraintList = MutableLiveData<List<Constraint>>()
    val constraintList: LiveData<List<Constraint>> = _constraintList

    val constraintAndMode = MutableLiveData<Boolean>()
    val constraintOrMode = MutableLiveData<Boolean>()

    private val _modelList = MutableLiveData<State<List<ConstraintModel>>>(Empty())
    val modelList: LiveData<State<List<ConstraintModel>>> = _modelList

    private val _buildModelsEvent = MutableSharedFlow<List<Constraint>>()
    val buildModelsEvent = _buildModelsEvent.asSharedFlow()

    private val _showFixPrompt = MutableSharedFlow<Failure>()
    val showFixPrompt = _showFixPrompt.asSharedFlow()

    //TODO replace with liveevent
    private val _duplicateConstraintsEvent = MutableSharedFlow<Unit>()
    val duplicateConstraintsEvent = _duplicateConstraintsEvent.asSharedFlow()

    init {
        mCoroutineScope.launch {
            constraintList.asFlow().collect {
                buildModelsEvent.onSubscription {
                    _buildModelsEvent.emit(it)
                }
            }
        }
    }

    fun setConstraintList(constraintList: List<Constraint>, constraintMode: Int) {
        _constraintList.value = constraintList

        when (constraintMode) {
            Constraint.MODE_AND -> {
                constraintAndMode.value = true
                constraintOrMode.value = false
            }

            Constraint.MODE_OR -> {
                constraintOrMode.value = true
                constraintAndMode.value = false
            }
        }
    }

    fun getConstraintMode(): Int = when {
        constraintAndMode.value == true -> Constraint.MODE_AND
        constraintOrMode.value == true -> Constraint.MODE_OR
        else -> Constraint.DEFAULT_MODE
    }

    fun addConstraint(constraint: Constraint) {
        if (constraintList.value?.any { it.uniqueId == constraint.uniqueId } == true) {
            duplicateConstraintsEvent.onSubscription {
                _duplicateConstraintsEvent.emit(Unit)
            }

            return
        }

        _constraintList.value = constraintList.value?.toMutableList()?.apply {
            add(constraint)
        }
    }

    fun removeConstraint(id: String) {
        _constraintList.value = constraintList.value?.toMutableList()?.apply {
            removeAll { it.uniqueId == id }
        }
    }

    fun onModelClick(id: String) {
        mCoroutineScope.launch {
            modelList.value?.ifIsData { modelList ->
                val constraint = modelList.singleOrNull { it.id == id } ?: return@launch
                _showFixPrompt.emit(constraint.failure!!)
            }
        }
    }

    fun setModels(models: List<ConstraintModel>) {
        _modelList.value = when {
            models.isEmpty() -> Empty()
            else -> Data(models)
        }
    }

    fun rebuildModels() = mCoroutineScope.launch {
        _buildModelsEvent.emit(constraintList.value ?: listOf())
    }
}