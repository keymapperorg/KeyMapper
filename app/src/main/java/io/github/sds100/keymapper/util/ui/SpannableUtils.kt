package io.github.sds100.keymapper.util.ui

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan

/**
 * Created by sds100 on 15/06/2021.
 */

object SpannableUtils {

    fun color(color: Int, vararg content: CharSequence): CharSequence =
        apply(content, ForegroundColorSpan(color))

    /**
     * Returns a CharSequence that concatenates the specified array of CharSequence
     * objects and then applies a list of zero or more tags to the entire range.
     *
     * @param content an array of character sequences to apply a style to
     * @param tags the styled span objects to apply to the content
     *        such as android.text.style.StyleSpan
     */

    private fun apply(content: Array<out CharSequence>, vararg tags: Any): CharSequence {
        return SpannableStringBuilder().apply {
            openTags(tags)
            content.forEach { charSequence ->
                append(charSequence)
            }
            closeTags(tags)
        }
    }

    /**
     * Iterates over an array of tags and applies them to the beginning of the specified
     * Spannable object so that future text appended to the text will have the styling
     * applied to it. Do not call this method directly.
     */
    private fun Spannable.openTags(tags: Array<out Any>) {
        tags.forEach { tag ->
            setSpan(tag, 0, 0, Spannable.SPAN_MARK_MARK)
        }
    }

    /**
     * "Closes" the specified tags on a Spannable by updating the spans to be
     * endpoint-exclusive so that future text appended to the end will not take
     * on the same styling. Do not call this method directly.
     */
    private fun Spannable.closeTags(tags: Array<out Any>) {
        tags.forEach { tag ->
            if (length > 0) {
                setSpan(tag, 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                removeSpan(tag)
            }
        }
    }

}