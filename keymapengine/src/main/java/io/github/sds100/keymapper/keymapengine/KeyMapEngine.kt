package io.github.sds100.keymapper.keymapengine

object KeyMapEngine {
    init {
        System.loadLibrary("keymap_engine")
    }

    external fun version(): String
}
