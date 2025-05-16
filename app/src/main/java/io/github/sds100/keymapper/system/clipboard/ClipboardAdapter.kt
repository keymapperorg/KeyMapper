package io.github.sds100.keymapper.system.clipboard

interface ClipboardAdapter {
    fun copy(label: String, text: String)
}
