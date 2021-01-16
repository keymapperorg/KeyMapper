package io.github.sds100.keymapper.util.delegate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.util.DataState
import io.github.sds100.keymapper.util.ViewState

/**
 * Created by sds100 on 13/01/21.
 */

interface IModelState<T> {
    val model: LiveData<DataState<T>>
    val viewState: MutableLiveData<ViewState>
}

