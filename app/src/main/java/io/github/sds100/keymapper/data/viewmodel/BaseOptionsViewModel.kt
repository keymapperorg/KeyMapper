@file:Suppress("UNCHECKED_CAST")

package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.*
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.model.options.BoolOption
import io.github.sds100.keymapper.data.model.options.IntOption
import io.github.sds100.keymapper.data.model.options.OptionsListModel
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.DataState
import io.github.sds100.keymapper.util.ViewLoading
import io.github.sds100.keymapper.util.ViewState
import io.github.sds100.keymapper.util.delegate.IModelState

/**
 * Created by sds100 on 28/11/20.
 */

abstract class BaseOptionsViewModel<O : BaseOptions<*>>
    : ViewModel(), IModelState<OptionsListModel> {

    abstract val stateKey: String
    val options = MediatorLiveData<O>()
    override val viewState = MutableLiveData<ViewState>(ViewLoading())

    override val model: LiveData<DataState<OptionsListModel>> = options.map { options ->
        val sliderModels = sequence {
            options.intOptions.forEach {
                if (it.isAllowed) {
                    yield(createSliderListItemModel(it))
                }
            }
        }.toList()

        val checkBoxModels = sequence {
            options.boolOptions.forEach {
                if (it.isAllowed) {
                    yield(createCheckboxListItemModel(it))
                }
            }
        }.toList()

        Data(OptionsListModel(checkBoxModels, sliderModels))
    }

    fun saveState(outState: Bundle) {
        options.value?.let {
            outState.putParcelable(stateKey, it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreState(state: Bundle) {
        state.getParcelable<O>(stateKey)?.let {
            setOptions(it)
        }
    }

    open fun setValue(id: String, newValue: Int) {
        options.value = options.value?.let {
            it.setValue(id, newValue) as O
        }
    }

    open fun setValue(id: String, newValue: Boolean) {
        options.value = options.value?.let {
            it.setValue(id, newValue) as O
        }
    }

    fun setOptions(options: O) {
        this.options.value = options
    }

    abstract fun createSliderListItemModel(option: IntOption): SliderListItemModel
    abstract fun createCheckboxListItemModel(option: BoolOption): CheckBoxListItemModel
}