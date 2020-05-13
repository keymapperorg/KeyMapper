package io.github.sds100.keymapper.util.delegate

import androidx.lifecycle.LifecycleCoroutineScope
import io.github.sds100.keymapper.data.model.Action

/**
 * Created by sds100 on 05/05/2020.
 */
interface IKeymapDetectionDelegate {
    fun performAction(action: Action)
    fun imitateButtonPress(keyCode: Int)
    val lifecycleScope: LifecycleCoroutineScope
}