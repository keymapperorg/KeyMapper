@file:Suppress("UNCHECKED_CAST")

package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import io.github.sds100.keymapper.data.model.CheckBoxListItemModel
import io.github.sds100.keymapper.data.model.SliderListItemModel
import io.github.sds100.keymapper.data.model.options.BaseOptions
import io.github.sds100.keymapper.data.model.options.BoolOption
import io.github.sds100.keymapper.data.model.options.IntOption

/**
 * Created by sds100 on 28/11/20.
 */

abstract class BaseOptionsViewModel<O : BaseOptions<*>> : ViewModel() {

    abstract val stateKey: String
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

    fun saveState(outState: Bundle) {
        outState.putParcelable(stateKey, options.value)
    }

    @Suppress("UNCHECKED_CAST")
    fun restoreState(state: Bundle) {
        state.getParcelable<O>(stateKey)?.let {
            setOptions(it)
        }
    }

    open fun setValue(id: String, newValue: Int) {
        options.value = options.value?.setValue(id, newValue) as O?
    }

    open fun setValue(id: String, newValue: Boolean) {
        options.value = options.value?.setValue(id, newValue) as O?
    }

    fun setOptions(options: O) {
        this.options.value = options
    }

    abstract fun createSliderListItemModel(option: IntOption): SliderListItemModel
    abstract fun createCheckboxListItemModel(option: BoolOption): CheckBoxListItemModel
}