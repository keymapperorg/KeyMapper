package io.github.sds100.keymapper.system.intents

import android.content.Intent
import kotlinx.serialization.Serializable

@Serializable
sealed class IntentExtraType {
    abstract fun putInIntent(intent: Intent, name: String, value: String)

    /**
     * Return null if it is invalid
     */
    abstract fun parse(value: String): Any?

    fun isValid(value: String) = parse(value) != null
}

@Serializable
data object BoolExtraType : IntentExtraType() {
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
data object BoolArrayExtraType : IntentExtraType() {
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
data object IntExtraType : IntentExtraType() {
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
data object IntArrayExtraType : IntentExtraType() {
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
data object StringExtraType : IntentExtraType() {
    override fun putInIntent(intent: Intent, name: String, value: String) {
        intent.putExtra(name, parse(value))
    }

    override fun parse(value: String): String = value
}

@Serializable
data object StringArrayExtraType : IntentExtraType() {
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
data object LongExtraType : IntentExtraType() {
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
data object LongArrayExtraType : IntentExtraType() {
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
data object ByteExtraType : IntentExtraType() {
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
data object ByteArrayExtraType : IntentExtraType() {
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
data object DoubleExtraType : IntentExtraType() {
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
data object DoubleArrayExtraType : IntentExtraType() {
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
data object CharExtraType : IntentExtraType() {
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
data object CharArrayExtraType : IntentExtraType() {
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
data object FloatExtraType : IntentExtraType() {
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
data object FloatArrayExtraType : IntentExtraType() {
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
data object ShortExtraType : IntentExtraType() {
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
data object ShortArrayExtraType : IntentExtraType() {
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
