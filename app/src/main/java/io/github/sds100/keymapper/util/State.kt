@file:Suppress("CanSealedSubClassBeObject")

package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 06/11/20.
 */

sealed class State<out T>

data class Data<out T>(val data: T) : State<T>()
class Loading : State<Nothing>()
class Empty : State<Nothing>()

fun <T, O> State<T>.mapData(block: (data: T) -> O): State<O> = when (this) {
    is Loading -> Loading()
    is Empty -> Empty()
    is Data -> Data(block.invoke(this.data))
}

inline fun <T> State<T>.ifIsData(block: (data: T) -> Unit) {
    if (this is Data) {
        block.invoke(this.data)
    }
}

fun <T> List<T>?.getState() =
    if (this.isNullOrEmpty()) {
        Empty()
    } else {
        Data(this)
    }

fun <K, T> Map<K, T>?.getState() =
    if (this.isNullOrEmpty()) {
        Empty()
    } else {
        Data(this)
    }