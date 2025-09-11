package io.github.sds100.keymapper.base.trigger

enum class RemapStatus {
    /** The button cannot be remapped. */
    UNSUPPORTED,

    /** The button might be remappable, but it requires special conditions or is uncertain. */
    UNCERTAIN,

    /** The button can be remapped. */
    SUPPORTED,
}
