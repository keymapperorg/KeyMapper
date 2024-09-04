package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 17/03/2021.
 */
sealed class State<out T> {
    data class Data<T>(val data: T) : State<T>()
    object Loading : State<Nothing>()
}

fun <T> State<T>.dataOrNull(): T? = when (this) {
    is State.Data -> this.data
    State.Loading -> null
}

inline fun <T, O> State<T>.mapData(block: (data: T) -> O): State<O> = when (this) {
    is State.Loading -> State.Loading
    is State.Data -> State.Data(block.invoke(this.data))
}

inline fun <T> State<T>.ifIsData(block: (data: T) -> Unit) {
    if (this is State.Data) {
        block.invoke(this.data)
    }
}
