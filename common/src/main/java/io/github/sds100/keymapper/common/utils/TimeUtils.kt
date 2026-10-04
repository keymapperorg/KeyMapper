package io.github.sds100.keymapper.common.utils

import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object TimeUtils {
    fun localeDateFormatter(style: FormatStyle): DateTimeFormatter {
        return DateTimeFormatter.ofLocalizedTime(style).withLocale(Locale.getDefault())
    }

    /**
     * @return the milliseconds as seconds without a trailing ".0", e.g. 1000 -> "1", 1500 -> "1.5".
     */
    fun formatSeconds(ms: Int, locale: Locale = Locale.getDefault()): String {
        val seconds = ms / 1000f

        return if (seconds % 1f == 0f) {
            seconds.toInt().toString()
        } else {
            String.format(locale, "%.1f", seconds)
        }
    }
}
