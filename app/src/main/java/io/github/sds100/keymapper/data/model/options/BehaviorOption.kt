package io.github.sds100.keymapper.data.model.options

import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.ExtraId
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import java.io.Serializable

/**
 * Created by sds100 on 27/06/20.
 */
class BehaviorOption<T>(val id: String, var value: T, var isAllowed: Boolean) : Serializable {
    companion object {
        const val DEFAULT = -1

        fun List<Extra>.applyBehaviorOption(behaviorOption: BehaviorOption<Int>, @ExtraId extraId: String) =
            toMutableList().apply {
                removeAll { it.id == extraId }

                if (behaviorOption.isAllowed) {
                    if (behaviorOption.value != DEFAULT) {
                        add(Extra(extraId, behaviorOption.value.toString()))
                    }
                }
            }

        fun Int.applyBehaviorOption(behaviorOption: BehaviorOption<Boolean>, flagId: Int): Int {
            return if (behaviorOption.isAllowed) {
                if (behaviorOption.value) {
                    withFlag(flagId)
                } else {
                    minusFlag(flagId)
                }
            } else {
                minusFlag(flagId)
            }
        }

        val Int.nullIfDefault: Int?
            get() = if (this == DEFAULT) {
                null
            } else {
                this
            }
    }
}

