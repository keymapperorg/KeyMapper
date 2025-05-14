package io.github.sds100.keymapper.trigger

import io.github.sds100.keymapper.keymaps.ClickType
import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 21/02/2021.
 */

@Serializable
sealed class TriggerMode : Comparable<TriggerMode> {
    override fun compareTo(other: TriggerMode) = this.javaClass.name.compareTo(other.javaClass.name)

    @Serializable
    data class Parallel(val clickType: ClickType) : TriggerMode() {
        override fun compareTo(other: TriggerMode): Int {
            if (other !is Parallel) {
                return super.compareTo(other)
            }

            return clickType.compareTo(other.clickType)
        }
    }

    @Serializable
    object Sequence : TriggerMode()

    @Serializable
    object Undefined : TriggerMode()

    override fun toString(): String = when (this) {
        is Parallel -> Parallel(clickType).toString()
        Sequence -> "Sequence"
        Undefined -> "Undefined"
    }
}
