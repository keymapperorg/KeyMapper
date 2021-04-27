package io.github.sds100.keymapper.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 18/11/20.
 */

val Fragment.viewLifecycleScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycle.coroutineScope

fun <T> Flow<T>.collectWhenResumed(
    lifecycleOwner: LifecycleOwner,
    block: suspend (value: T) -> Unit
) {
    lifecycleOwner.lifecycle.coroutineScope.launchWhenResumed {
        collect {
            block.invoke(it)
        }
    }
}

fun <T> Flow<T>.collectWhenStarted(
    lifecycleOwner: LifecycleOwner,
    block: suspend (value: T) -> Unit
) {
    lifecycleOwner.lifecycle.coroutineScope.launchWhenStarted {
        collect {
            block.invoke(it)
        }
    }
}

fun <T> Flow<T>.firstBlocking(): T = runBlocking { first() }