package io.github.sds100.keymapper.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume

/**
 * Created by sds100 on 18/11/20.
 */

val Fragment.viewLifecycleScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycle.coroutineScope

fun <T> CancellableContinuation<T>.resumeIfNotCompleted(value: T) {
    if (!this.isCompleted) {
        this.resume(value)
    }
}

fun <T> Flow<T>.firstBlocking(): T = runBlocking { first() }