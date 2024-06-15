package io.github.sds100.keymapper.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume

/**
 * Created by sds100 on 18/11/20.
 */

val Fragment.viewLifecycleScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycle.coroutineScope

fun LifecycleOwner.launchRepeatOnLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit,
) {
    lifecycleScope.launch {
        this@launchRepeatOnLifecycle.repeatOnLifecycle(state, block)
    }
}

fun <T> CancellableContinuation<T>.resumeIfNotCompleted(value: T) {
    if (!this.isCompleted) {
        this.resume(value)
    }
}

fun <T> Flow<T>.firstBlocking(): T = runBlocking { first() }
