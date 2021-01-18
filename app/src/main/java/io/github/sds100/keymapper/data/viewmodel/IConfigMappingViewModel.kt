package io.github.sds100.keymapper.data.viewmodel

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.util.Event
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 17/01/21.
 */

interface IConfigMappingViewModel {
    val actionListViewModel: ActionListViewModel<*>
    val isEnabled: MutableLiveData<Boolean>
    val eventStream: LiveData<Event>

    fun save(coroutineScope: CoroutineScope)
    fun saveState(outState: Bundle)
    fun restoreState(state: Bundle)
}