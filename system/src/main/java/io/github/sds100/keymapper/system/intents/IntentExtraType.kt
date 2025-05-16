package io.github.sds100.keymapper.system.intents

import android.content.Intent
import io.github.sds100.keymapper.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class IntentExtraType {
    abstract val labelStringRes: Int
    abstract val exampleStringRes: Int

    abstract fun putInIntent(intent: Intent, name: String, value: String)

    /**
     * Return null if it is invalid
     */
    abstract fun parse(value: String): Any?

    fun isValid(value: String) = parse(value) != null
}

@Serializable
class BoolExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_bool_header

    @Transient
    override val exampleStringRes = R.string.intent_type_bool_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Boolean? = when (value.trim()) {
        "true" -> true
        "false" -> false
        else -> null
    }
}

@Serializable
class BoolArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_bool_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_bool_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): BooleanArray? {
        return value
            .trim()
            .split(',')
            .map {
                when (it.trim()) {
                    "true" -> true
                    "false" -> false
                    else -> return null
                }
            }
            .toBooleanArray()
    }
}

@Serializable
class IntExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_int_header

    @Transient
    override val exampleStringRes = R.string.intent_type_int_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Int? = try {
        value.trim().toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

@Serializable
class IntArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_int_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_int_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): IntArray? {
        return value
            .trim()
            .split(',')
            .map {
                try {
                    it.trim().toInt()
                } catch (e: NumberFormatException) {
                    return null
                }
            }
            .toIntArray()
    }
}

@Serializable
class StringExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_string_header

    @Transient
    override val exampleStringRes = R.string.intent_type_string_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): String = value
}

@Serializable
class StringArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_string_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_string_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Array<String> = value
        .trim()
        .split(',')
        .map {
            it
        }
        .toTypedArray()
}

@Serializable
class LongExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_long_header

    @Transient
    override val exampleStringRes = R.string.intent_type_long_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Long? = try {
        value.trim().toLong()
    } catch (e: NumberFormatException) {
        null
    }
}

@Serializable
class LongArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_long_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_long_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): LongArray? {
        return value
            .trim()
            .split(',')
            .map {
                try {
                    it.trim().toLong()
                } catch (e: NumberFormatException) {
                    return null
                }
            }
            .toLongArray()
    }
}

@Serializable
class ByteExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_byte_header

    @Transient
    override val exampleStringRes = R.string.intent_type_byte_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Byte? = try {
        value.trim().toByte()
    } catch (e: NumberFormatException) {
        null
    }
}

@Serializable
class ByteArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_byte_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_byte_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): ByteArray? {
        return value
            .trim()
            .split(',')
            .map {
                try {
                    it.trim().toByte()
                } catch (e: NumberFormatException) {
                    return null
                }
            }
            .toByteArray()
    }
}

@Serializable
class DoubleExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_double_header

    @Transient
    override val exampleStringRes = R.string.intent_type_double_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Double? = try {
        value.trim().toDouble()
    } catch (e: NumberFormatException) {
        null
    }
}

@Serializable
class DoubleArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_double_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_double_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): DoubleArray? {
        return value
            .trim()
            .split(',')
            .map {
                try {
                    it.trim().toDouble()
                } catch (e: NumberFormatException) {
                    return null
                }
            }
            .toDoubleArray()
    }
}

@Serializable
class CharExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_char_header

    @Transient
    override val exampleStringRes = R.string.intent_type_char_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Char? = if (value.length == 1) {
        value.toCharArray()[0]
    } else {
        null
    }
}

@Serializable
class CharArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_char_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_char_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): CharArray? {
        return value
            .trim()
            .split(',')
            .map {
                if (value.length == 1) {
                    value.toCharArray()[0]
                } else {
                    return null
                }
            }
            .toCharArray()
    }
}

@Serializable
class FloatExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_float_header

    @Transient
    override val exampleStringRes = R.string.intent_type_float_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Float? = try {
        value.trim().toFloat()
    } catch (e: NumberFormatException) {
        null
    }
}

@Serializable
class FloatArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_float_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_float_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): FloatArray? {
        return value
            .trim()
            .split(',')
            .map {
                try {
                    it.trim().toFloat()
                } catch (e: NumberFormatException) {
                    return null
                }
            }
            .toFloatArray()
    }
}

@Serializable
class ShortExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_short_header

    @Transient
    override val exampleStringRes = R.string.intent_type_short_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): Short? = try {
        value.trim().toShort()
    } catch (e: NumberFormatException) {
        null
    }
}

@Serializable
class ShortArrayExtraType : IntentExtraType() {
    @Transient
    override val labelStringRes = R.string.intent_type_short_array_header

    @Transient
    override val exampleStringRes = R.string.intent_type_short_array_example

    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): ShortArray? {
        return value
            .trim()
            .split(',')
            .map {
                try {
                    it.trim().toShort()
                } catch (e: NumberFormatException) {
                    return null
                }
            }
            .toShortArray()
    }
}
