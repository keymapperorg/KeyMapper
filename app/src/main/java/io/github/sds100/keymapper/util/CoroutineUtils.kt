package io.github.sds100.keymapper.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Created by sds100 on 18/11/20.
 */

val Fragment.viewLifecycleScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycle.coroutineScope

fun <T> Flow<T>.collectWhenLifecycleStarted(lifecycleOwner: LifecycleOwner,
                                            block: suspend (value: T) -> Unit) {
    lifecycleOwner.lifecycle.coroutineScope.launchWhenStarted {
        collect {
            block.invoke(it)
        }
    }
}