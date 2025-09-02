package io.github.sds100.keymapper.base.utils

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.system.intents.BoolArrayExtraType
import io.github.sds100.keymapper.system.intents.BoolExtraType
import io.github.sds100.keymapper.system.intents.ByteArrayExtraType
import io.github.sds100.keymapper.system.intents.ByteExtraType
import io.github.sds100.keymapper.system.intents.CharArrayExtraType
import io.github.sds100.keymapper.system.intents.CharExtraType
import io.github.sds100.keymapper.system.intents.DoubleArrayExtraType
import io.github.sds100.keymapper.system.intents.DoubleExtraType
import io.github.sds100.keymapper.system.intents.FloatArrayExtraType
import io.github.sds100.keymapper.system.intents.FloatExtraType
import io.github.sds100.keymapper.system.intents.IntArrayExtraType
import io.github.sds100.keymapper.system.intents.IntExtraType
import io.github.sds100.keymapper.system.intents.IntentExtraType
import io.github.sds100.keymapper.system.intents.LongArrayExtraType
import io.github.sds100.keymapper.system.intents.LongExtraType
import io.github.sds100.keymapper.system.intents.ShortArrayExtraType
import io.github.sds100.keymapper.system.intents.ShortExtraType
import io.github.sds100.keymapper.system.intents.StringArrayExtraType
import io.github.sds100.keymapper.system.intents.StringExtraType

fun IntentExtraType.getLabelStringRes(): Int = when (this) {
    is BoolExtraType -> R.string.intent_type_bool_header
    is BoolArrayExtraType -> R.string.intent_type_bool_array_header
    is IntExtraType -> R.string.intent_type_int_header
    is IntArrayExtraType -> R.string.intent_type_int_array_header
    is StringExtraType -> R.string.intent_type_string_header
    is StringArrayExtraType -> R.string.intent_type_string_array_header
    is LongExtraType -> R.string.intent_type_long_header
    is LongArrayExtraType -> R.string.intent_type_long_array_header
    is ByteExtraType -> R.string.intent_type_byte_header
    is ByteArrayExtraType -> R.string.intent_type_byte_array_header
    is DoubleExtraType -> R.string.intent_type_double_header
    is DoubleArrayExtraType -> R.string.intent_type_double_array_header
    is CharExtraType -> R.string.intent_type_char_header
    is CharArrayExtraType -> R.string.intent_type_char_array_header
    is FloatExtraType -> R.string.intent_type_float_header
    is FloatArrayExtraType -> R.string.intent_type_float_array_header
    is ShortExtraType -> R.string.intent_type_short_header
    is ShortArrayExtraType -> R.string.intent_type_short_array_header
}

fun IntentExtraType.getExampleStringRes(): Int = when (this) {
    is BoolExtraType -> R.string.intent_type_bool_example
    is BoolArrayExtraType -> R.string.intent_type_bool_array_example
    is IntExtraType -> R.string.intent_type_int_example
    is IntArrayExtraType -> R.string.intent_type_int_array_example
    is StringExtraType -> R.string.intent_type_string_example
    is StringArrayExtraType -> R.string.intent_type_string_array_example
    is LongExtraType -> R.string.intent_type_long_example
    is LongArrayExtraType -> R.string.intent_type_long_array_example
    is ByteExtraType -> R.string.intent_type_byte_example
    is ByteArrayExtraType -> R.string.intent_type_byte_array_example
    is DoubleExtraType -> R.string.intent_type_double_example
    is DoubleArrayExtraType -> R.string.intent_type_double_array_example
    is CharExtraType -> R.string.intent_type_char_example
    is CharArrayExtraType -> R.string.intent_type_char_array_example
    is FloatExtraType -> R.string.intent_type_float_example
    is FloatArrayExtraType -> R.string.intent_type_float_array_example
    is ShortExtraType -> R.string.intent_type_short_example
    is ShortArrayExtraType -> R.string.intent_type_short_array_example
}
