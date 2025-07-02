package io.github.sds100.keymapper.base.utils.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
