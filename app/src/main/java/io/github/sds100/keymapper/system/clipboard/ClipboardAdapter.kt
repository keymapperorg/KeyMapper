package io.github.sds100.keymapper.system.clipboard

/**
 * Created by sds100 on 14/05/2021.
 */
interface ClipboardAdapter {
    fun copy(label: String, text: String)
}
