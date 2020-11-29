@file:Suppress("UNCHECKED_CAST")

package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.model.options.BehaviorOption
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 28/11/20.
 */

abstract class BaseOptionsViewModel<O : BaseOptions<*>> : ViewModel() {

    val options = MediatorLiveData<O>()

    val sliderModels = options.map { options ->
        sequence {
            options.intOptions.forEach {
                if (it.isAllowed) {
                    yield(createSliderListItemModel(it))
                }
            }
        }.toList()
    }

    val checkBoxModels = options.map { options ->
        sequence {
            options.boolOptions.forEach {
                if (it.isAllowed) {
                    yield(createCheckboxListItemModel(it))
                }
            }
        }.toList()
    }

    private val _onSaveEvent = MutableSharedFlow<BaseOptions<*>>()
    val onSaveEvent = _onSaveEvent.asSharedFlow()

    fun setValue(id: String, newValue: Int) {
        options.value = options.value?.setValue(id, newValue) as O?
    }

    fun setValue(id: String, newValue: Boolean) {
        options.value = options.value?.setValue(id, newValue) as O?
    }

    fun setOptions(options: O) {
        this.options.value = options
    }

    fun save() {
        viewModelScope.launch {
            _onSaveEvent.emit(options.value!!)
        }
    }

    fun saveState(outState: Bundle) {
        outState.putSerializable(stateKey, options.value)
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreState(state: Bundle) {
        val options = state.getSerializable(stateKey) as O
        setOptions(options)
    }

    abstract val stateKey: String
    abstract fun createSliderListItemModel(option: BehaviorOption<Int>): SliderListItemModel
    abstract fun createCheckboxListItemModel(option: BehaviorOption<Boolean>): CheckBoxListItemModel
}