package io.github.sds100.keymapper.system.intents

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed class IntentExtraRow : Parcelable {
    companion object {
        fun toType(row: IntentExtraRow): IntentExtraType2 {
            return when (row) {
                is BooleanExtra -> IntentExtraType2.BOOLEAN
                is BooleanArrayExtra -> IntentExtraType2.BOOLEAN_ARRAY
                is StringExtra -> IntentExtraType2.STRING
                is ByteArrayExtra -> IntentExtraType2.BYTE_ARRAY
                is ByteExtra -> IntentExtraType2.BYTE
                is CharArrayExtra -> IntentExtraType2.CHAR_ARRAY
                is CharExtra -> IntentExtraType2.CHAR
                is DoubleArrayExtra -> IntentExtraType2.DOUBLE_ARRAY
                is DoubleExtra -> IntentExtraType2.DOUBLE
                is FloatArrayExtra -> IntentExtraType2.FLOAT_ARRAY
                is FloatExtra -> IntentExtraType2.FLOAT
                is IntegerArrayExtra -> IntentExtraType2.INTEGER_ARRAY
                is IntegerExtra -> IntentExtraType2.INTEGER
                is LongArrayExtra -> IntentExtraType2.LONG_ARRAY
                is LongExtra -> IntentExtraType2.LONG
                is ShortArrayExtra -> IntentExtraType2.SHORT_ARRAY
                is ShortExtra -> IntentExtraType2.SHORT
                is StringArrayExtra -> IntentExtraType2.STRING_ARRAY
            }
        }

        fun fromType(type: IntentExtraType2, name: String): IntentExtraRow {
            return when (type) {
                IntentExtraType2.BOOLEAN -> BooleanExtra(name = name)
                IntentExtraType2.BOOLEAN_ARRAY -> BooleanArrayExtra(name = name)
                IntentExtraType2.INTEGER -> IntegerExtra(name = name)
                IntentExtraType2.INTEGER_ARRAY -> IntegerArrayExtra(name = name)
                IntentExtraType2.STRING -> StringExtra(name = name)
                IntentExtraType2.STRING_ARRAY -> StringArrayExtra(name = name)
                IntentExtraType2.LONG -> LongExtra(name = name)
                IntentExtraType2.LONG_ARRAY -> LongArrayExtra(name = name)
                IntentExtraType2.BYTE -> ByteExtra(name = name)
                IntentExtraType2.BYTE_ARRAY -> ByteArrayExtra(name = name)
                IntentExtraType2.DOUBLE -> DoubleExtra(name = name)
                IntentExtraType2.DOUBLE_ARRAY -> DoubleArrayExtra(name = name)
                IntentExtraType2.CHAR -> CharExtra(name = name)
                IntentExtraType2.CHAR_ARRAY -> CharArrayExtra(name = name)
                IntentExtraType2.FLOAT -> FloatExtra(name = name)
                IntentExtraType2.FLOAT_ARRAY -> FloatArrayExtra(name = name)
                IntentExtraType2.SHORT -> ShortExtra(name = name)
                IntentExtraType2.SHORT_ARRAY -> ShortArrayExtra(name = name)
            }
        }
    }

    abstract val name: String
    abstract val valueString: String
    abstract val isFormattedCorrectly: Boolean

    @Parcelize
    data class BooleanExtra(override val name: String, val value: Boolean = true) : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = if (value) {
            "true"
        } else {
            "false"
        }

        override val isFormattedCorrectly: Boolean = true
    }

    @Parcelize
    data class BooleanArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it == "true" || it == "false" }
            }
    }

    @Parcelize
    data class IntegerExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = value.isEmpty() || value.toIntOrNull() != null
    }

    @Parcelize
    data class IntegerArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it.toIntOrNull() != null }
            }
    }

    @Parcelize
    data class LongExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = value.isEmpty() || value.toLongOrNull() != null
    }

    @Parcelize
    data class LongArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it.toLongOrNull() != null }
            }
    }

    @Parcelize
    data class ShortExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = value.isEmpty() || value.toShortOrNull() != null
    }

    @Parcelize
    data class ShortArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it.toShortOrNull() != null }
            }
    }

    @Parcelize
    data class ByteExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = value.isEmpty() || value.toByteOrNull() != null
    }

    @Parcelize
    data class ByteArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it.toByteOrNull() != null }
            }
    }

    @Parcelize
    data class DoubleExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = value.isEmpty() || value.toDoubleOrNull() != null
    }

    @Parcelize
    data class DoubleArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it.toDoubleOrNull() != null }
            }
    }

    @Parcelize
    data class FloatExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = value.isEmpty() || value.toFloatOrNull() != null
    }

    @Parcelize
    data class FloatArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it.toFloatOrNull() != null }
            }
    }

    @Parcelize
    data class CharExtra(override val name: String, val value: Char? = null) : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value.toString()

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = true
    }

    @Parcelize
    data class CharArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean =
            if (value.isEmpty()) {
                true
            } else {
                value.split(',')
                    .map { it.trim() }
                    .all { it.length == 1 }
            }
    }

    @Parcelize
    data class StringExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = true
    }

    @Parcelize
    data class StringArrayExtra(override val name: String, val value: String = "") : IntentExtraRow() {
        @IgnoredOnParcel
        override val valueString: String = value

        @IgnoredOnParcel
        override val isFormattedCorrectly: Boolean = true
    }
}