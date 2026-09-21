package io.github.sds100.keymapper.base.home

import io.github.sds100.keymapper.base.trigger.TriggerError
import io.github.sds100.keymapper.common.utils.KMError

sealed class KeyMapError {
    abstract val message: String
    abstract val isFixable: Boolean

    data class Trigger(val error: TriggerError, override val message: String) : KeyMapError() {
        override val isFixable: Boolean = error.isFixable
    }

    data class Action(
        val error: KMError,
        override val message: String,
        override val isFixable: Boolean,
    ) : KeyMapError()

    data class Constraint(
        val error: KMError,
        override val message: String,
        override val isFixable: Boolean,
    ) : KeyMapError()
}
