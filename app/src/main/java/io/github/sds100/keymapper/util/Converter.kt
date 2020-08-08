package io.github.sds100.keymapper.util

import androidx.databinding.InverseMethod

/**
 * Created by sds100 on 08/08/20.
 */

object Converter {
    @InverseMethod("stringToInt")
    @JvmStatic
    fun intToString(value: Int?): String = value?.toString() ?: ""

    @JvmStatic
    fun stringToInt(value: String): Int? = value.toIntOrNull()
}