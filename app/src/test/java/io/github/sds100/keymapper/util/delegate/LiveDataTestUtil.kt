package io.github.sds100.keymapper.util.delegate

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.github.sds100.keymapper.util.Event
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by sds100 on 19/05/2020.
 */

@VisibleForTesting(otherwise = VisibleForTesting.NONE)
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    afterObserve: () -> Unit = {}
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(o: T?) {
            data = o
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }
    this.observeForever(observer)

    try {
        afterObserve.invoke()

        // Don't wait indefinitely if the LiveData is not set.
        if (!latch.await(time, timeUnit)) {
            throw TimeoutException("LiveData value was never set.")
        }

    } finally {
        this.removeObserver(observer)
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}


@VisibleForTesting(otherwise = VisibleForTesting.NONE)
suspend fun <T> LiveData<T>.getOrAwaitValueCoroutine(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS,
    afterObserve: () -> Unit = {}
): T {
    var data: T? = null

    withTimeout(timeUnit.toMillis(time)) {
        var observer: Observer<T>? = null

        try {
            observer = object : Observer<T> {
                override fun onChanged(o: T?) {
                    if (o is Event<*>) {
                        if (o.hasBeenHandled) return
                    }

                    if (o == null) {
                        return
                    }

                    data = o
                }
            }

            observeForever(observer)

        } catch (e: TimeoutCancellationException) {
            throw e
        } finally {
            afterObserve.invoke()
            observer?.let { removeObserver(it) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}