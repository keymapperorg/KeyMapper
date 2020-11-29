@file:Suppress("UNCHECKED_CAST")

package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.model.options.BaseOptions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 28/11/20.
 */

abstract class BaseOptionsDialogViewModel<O : BaseOptions<*>> : BaseOptionsViewModel<O>() {

    private val _onSaveEvent = MutableSharedFlow<BaseOptions<*>>()
    val onSaveEvent = _onSaveEvent.asSharedFlow()

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
}