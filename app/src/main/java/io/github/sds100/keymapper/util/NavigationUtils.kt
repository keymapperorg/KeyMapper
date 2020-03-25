package io.github.sds100.keymapper.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.navigation.NavBackStackEntry

/**
 * Created by sds100 on 25/03/2020.
 */

fun <T> NavBackStackEntry.observeLiveData(lifecycleOwner: LifecycleOwner, key: String, observe: (t: T) -> Unit) {
    savedStateHandle.getLiveData<T>(key).observe(lifecycleOwner) {
        observe(it)
    }
}

fun <T> NavBackStackEntry.setLiveData(key: String, value: T) {
    savedStateHandle.set(key, value)
}

fun <T> NavBackStackEntry.removeLiveData(key: String) {
    savedStateHandle.remove<T>(key)
}