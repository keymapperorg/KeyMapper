package io.github.sds100.keymapper.common.utils

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume

fun <T> CancellableContinuation<T>.resumeIfNotCompleted(value: T) {
    if (!this.isCompleted) {
        this.resume(value)
    }
}

fun <T> Flow<T>.firstBlocking(): T = runBlocking { first() }
