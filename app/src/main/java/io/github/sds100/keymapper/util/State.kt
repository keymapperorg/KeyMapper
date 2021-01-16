@file:Suppress("CanSealedSubClassBeObject")

package io.github.sds100.keymapper.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map

/**
 * Created by sds100 on 06/11/20.
 */

sealed class ViewState
class ViewPopulated : ViewState()
class ViewLoading : ViewState()
class ViewEmpty : ViewState()

sealed class DataState<out T>

data class Data<out T>(val data: T) : DataState<T>()
class Loading : DataState<Nothing>()
class Empty : DataState<Nothing>()

fun <T, O> DataState<T>.mapData(block: (data: T) -> O): DataState<O> = when (this) {
    is Loading -> Loading()
    is Empty -> Empty()
    is Data -> Data(block.invoke(this.data))
}

fun <T, O> LiveData<DataState<T>>.mapData(block: (data: T) -> O): LiveData<DataState<O>> =
    map {
        when (it) {
            is Loading -> Loading()
            is Empty -> Empty()
            is Data -> Data(block.invoke(it.data))
        }
    }

fun <T, O> DataState<T>.switchMapData(block: (data: T) -> DataState<O>): DataState<O> =
    when (this) {
        is Loading -> Loading()
        is Empty -> Empty()
        is Data -> block.invoke(this.data)
    }

inline fun <T> DataState<T>.ifIsData(block: (data: T) -> Unit) {
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

fun MutableLiveData<ViewState>.populated() {
    value = ViewPopulated()
}

fun MutableLiveData<ViewState>.loading() {
    value = ViewLoading()
}

fun MutableLiveData<ViewState>.empty() {
    value = ViewEmpty()
}