package io.github.sds100.keymapper.base.utils.ui

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.system.url.UrlUtils

@BindingAdapter("app:onTextChanged")
fun EditText.onTextChangedListener(textWatcher: TextWatcher) {
    addTextChangedListener(textWatcher)
}

@BindingAdapter("app:tintType")
fun AppCompatImageView.tintType(tintType: TintType?) {
    tintType?.toColor(context)?.let { setColorFilter(it) } ?: clearColorFilter()
}

@BindingAdapter("app:tintType")
fun MaterialTextView.tintType(tintType: TintType?) {
    tintType?.toColor(context)?.let { setTextColor(it) }
}

@BindingAdapter("app:errorWhenEmpty")
fun TextInputLayout.errorWhenEmpty(enabled: Boolean) {
    // need to set it up when the view is created
    if (editText?.text.isNullOrBlank()) {
        error = if (enabled) {
            str(R.string.error_cant_be_empty)
        } else {
            null
        }
    }

    editText?.addTextChangedListener {
        error = if (it.isNullOrBlank() && enabled) {
            str(R.string.error_cant_be_empty)
        } else {
            null
        }
    }
}

@BindingAdapter("app:onLongClick")
fun setLongClickListener(view: View, onLongClickListener: View.OnLongClickListener?) {
    view.setOnLongClickListener(onLongClickListener)
}

@BindingAdapter("app:seekBarEnabled")
fun Slider.enabled(enabled: Boolean) {
    isEnabled = enabled
}

@BindingAdapter("app:customBackgroundTint")
fun View.backgroundTint(@ColorInt color: Int) {
    backgroundTintList = ColorStateList.valueOf(color)
}

@BindingAdapter("app:harmonizeDrawableTint")
fun MaterialTextView.harmonizeDrawableTint(@ColorInt color: Int) {
    val harmonizedColor = MaterialColors.harmonizeWithPrimary(context, color)

    setCompoundDrawablesRelativeWithIntrinsicBounds(
        compoundDrawablesRelative[0]?.also { it.setTint(harmonizedColor) },
        compoundDrawablesRelative[1]?.also { it.setTint(harmonizedColor) },
        compoundDrawablesRelative[2]?.also { it.setTint(harmonizedColor) },
        compoundDrawablesRelative[3]?.also { it.setTint(harmonizedColor) },
    )
}

@BindingAdapter("app:openUrlOnClick")
fun View.openUrlOnClick(url: String?) {
    url ?: return

    setOnClickListener {
        UrlUtils.openUrl(context, url)
    }
}

@BindingAdapter("app:openUrlOnClick")
fun SquareImageButton.openUrlOnClick(url: Int?) {
    url ?: return

    setOnClickListener {
        UrlUtils.openUrl(context, context.str(url))
    }
}

@BindingAdapter("app:enabled")
fun View.enabled(isEnabled: Boolean) {
    if (isEnabled) {
        setEnabled(true)
        alpha = 1.0f
    } else {
        setEnabled(false)
        alpha = styledFloat(android.R.attr.disabledAlpha)
    }
}

// TODO check this is correct
fun TintType.toColor(ctx: Context): Int? = when (this) {
    TintType.None -> null
    TintType.OnSurface -> ctx.color(R.color.md_theme_onSurface)
    TintType.Error -> ctx.color(R.color.md_theme_error)
    is TintType.Color -> this.color
}
