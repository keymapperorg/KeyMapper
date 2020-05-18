package io.github.sds100.keymapper.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.observe
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * Created by sds100 on 25/03/2020.
 */

fun <T> NavBackStackEntry.observeLiveDataEvent(lifecycleOwner: LifecycleOwner, key: String, observe: (t: T) -> Unit) {
    /* use the Event class because any observers observing the saved state will receive the same callback multiple
    * times on a configuration change for example. */
    val observer = EventObserver<T> {
        observe(it)
    }

    savedStateHandle.getLiveData<Event<T>>(key).observe(lifecycleOwner, observer)
}

fun <T> NavBackStackEntry.setLiveDataEvent(key: String, value: T) {
    savedStateHandle.set(key, Event(value))
}

fun <T> NavController.observeCurrentDestinationEvent(
    lifecycleOwner: LifecycleOwner,
    key: String,
    observe: (t: T) -> Unit
) {
    currentDestination?.id?.let {
        getBackStackEntry(it).observeLiveDataEvent(lifecycleOwner, key, observe)
    }
}

fun <T> NavController.setCurrentDestinationEvent(key: String, value: T) {
    currentDestination?.id?.let {
        getBackStackEntry(it).setLiveDataEvent(key, Event(value))
    }
}

fun <T> NavBackStackEntry.observeLiveData(lifecycleOwner: LifecycleOwner, key: String, observe: (t: T) -> Unit) {
    savedStateHandle.getLiveData<T>(key).observe(lifecycleOwner) {
        observe(it)
    }
}

fun <T> NavBackStackEntry.setLiveData(key: String, value: T) {
    savedStateHandle.set(key, value)
}

fun <T> NavController.observeCurrentDestinationLiveData(
    lifecycleOwner: LifecycleOwner,
    key: String,
    observe: (t: T) -> Unit
) {
    currentDestination?.id?.let {
        getBackStackEntry(it).observeLiveData(lifecycleOwner, key, observe)
    }
}

fun <T> NavController.setCurrentDestinationLiveData(key: String, value: T) {
    currentDestination?.id?.let {
        getBackStackEntry(it).setLiveData(key, value)
    }
}