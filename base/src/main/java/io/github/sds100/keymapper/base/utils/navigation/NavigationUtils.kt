package io.github.sds100.keymapper.base.utils.navigation

import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.toRoute

fun <T> NavBackStackEntry.observeLiveData(
    lifecycleOwner: LifecycleOwner,
    key: String,
    observe: (t: T) -> Unit,
) {
    savedStateHandle.getLiveData<T>(key).observe(lifecycleOwner, {
        observe(it)
    })
}

fun <T> NavBackStackEntry.setLiveData(
    key: String,
    value: T,
) {
    savedStateHandle.set(key, value)
}

fun <T> NavController.observeCurrentDestinationLiveData(
    lifecycleOwner: LifecycleOwner,
    key: String,
    observe: (t: T) -> Unit,
) {
    currentDestination?.id?.let {
        getBackStackEntry(it).observeLiveData(lifecycleOwner, key, observe)
    }
}

fun <T> NavController.setCurrentDestinationLiveData(
    key: String,
    value: T,
) {
    currentDestination?.id?.let {
        getBackStackEntry(it).setLiveData(key, value)
    }
}

inline fun <reified R> NavBackStackEntry.handleRouteArgs(block: (R) -> Unit) {
    if (
        savedStateHandle.contains("handled_args")
    ) {
        return
    }

    val args = toRoute<R>()

    block(args)

    savedStateHandle["handled_args"] = true
}
