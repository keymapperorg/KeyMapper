package io.github.sds100.keymapper.base.util

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object TimeUtils {
    fun localeDateFormatter(style: FormatStyle): DateTimeFormatter {
        return DateTimeFormatter.ofLocalizedTime(style).withLocale(Locale.getDefault())
    }
}
