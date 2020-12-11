package io.github.sds100.keymapper.data.model.options

import android.os.Parcelable
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.ExtraId
import kotlinx.android.parcel.Parcelize
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

@Parcelize
class BoolOption(val id: String, var value: Boolean, var isAllowed: Boolean) : Parcelable {

    companion object {
        /**
         * save this option's value as a bit flag
         */
        fun Int.saveBoolOption(option: BoolOption, flagId: Int): Int {
            return if (option.isAllowed) {
                if (option.value) {
                    withFlag(flagId)
                } else {
                    minusFlag(flagId)
                }
            } else {
                minusFlag(flagId)
            }
        }
    }
}

@Parcelize
class IntOption(val id: String, var value: Int, var isAllowed: Boolean) : Parcelable {
    companion object {
        const val DEFAULT = -1

        val Int.nullIfDefault: Int?
            get() = if (this == DEFAULT) {
                null
            } else {
                this
            }

        fun List<Extra>.saveIntOption(option: IntOption, @ExtraId extraId: String) =
            toMutableList().apply {
                removeAll { it.id == extraId }

                if (option.isAllowed) {
                    if (option.value != DEFAULT) {
                        add(Extra(extraId, option.value.toString()))
                    }
                }
            }

    }
}

