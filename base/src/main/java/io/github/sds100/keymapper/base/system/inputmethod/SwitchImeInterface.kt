package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.common.utils.KMResult

interface SwitchImeInterface {
    /**
     * Enable the input method in the settings.
     */
    fun enableIme(imeId: String): KMResult<Unit>

    /**
     * Switch the active input method to the given input method id.
     */
    fun switchIme(imeId: String): KMResult<Unit>
}
