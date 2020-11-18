package io.github.sds100.keymapper.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.coroutineScope

/**
 * Created by sds100 on 18/11/20.
 */

val Fragment.viewLifecycleScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycle.coroutineScope