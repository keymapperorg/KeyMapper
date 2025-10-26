package io.github.sds100.keymapper.base.detection

interface KeyPressedCallback<T> {
    fun onDownEvent(button: T)

    fun onUpEvent(button: T)
}
