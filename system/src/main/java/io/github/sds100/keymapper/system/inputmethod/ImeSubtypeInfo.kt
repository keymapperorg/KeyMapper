package io.github.sds100.keymapper.system.inputmethod

data class ImeSubtypeInfo(
    /**
     * The subtype's hashCode(). Android identifies subtypes by this in its settings.
     */
    val id: Int,
    val languageTag: String,
)
