package io.github.sds100.keymapper.util.ui

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.styledColor
import io.github.sds100.keymapper.util.styledColorSL
import io.github.sds100.keymapper.util.styledFloat

/**
 * Created by sds100 on 25/01/2020.
 */

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

@BindingAdapter("app:chipUiModels", "app:onChipClickCallback", requireAll = true)
fun ChipGroup.setChipUiModels(
    models: List<ChipUi>,
    callback: OnChipClickCallback,
) {
    removeAllViews()

    val colorTintError by lazy { styledColorSL(R.attr.colorError) }
    val colorOnSurface by lazy { styledColorSL(R.attr.colorOnPrimaryContainer) }

    models.forEach { model ->
        when (model) {
            is ChipUi.Error -> {
                MaterialButton(context, null, R.attr.errorChipButtonStyle).apply {
                    id = View.generateViewId()

                    text = model.text
                    setOnClickListener { callback.onChipClick(model) }
                    addView(this)
                }
            }

            is ChipUi.Normal -> {
                MaterialButton(context, null, R.attr.normalChipButtonStyle).apply {
                    id = View.generateViewId()

                    this.text = model.text
                    this.icon = model.icon?.drawable

                    if (model.icon != null) {
                        this.iconTint = when (model.icon.tintType) {
                            TintType.None -> null
                            TintType.OnSurface -> colorOnSurface
                            TintType.Error -> colorTintError
                            is TintType.Color -> ColorStateList.valueOf(model.icon.tintType.color)
                        }
                    }

                    addView(this)
                }
            }

            is ChipUi.Transparent -> {
                MaterialButton(context, null, R.attr.transparentChipButtonStyle).apply {
                    id = View.generateViewId()

                    text = model.text
                    addView(this)
                }
            }
        }
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

fun TintType.toColor(ctx: Context): Int? =
    when (this) {
        TintType.None -> null
        TintType.OnSurface -> ctx.styledColor(R.attr.colorOnPrimaryContainer)
        TintType.Error -> ctx.styledColor(R.attr.colorError)
        is TintType.Color -> this.color
    }
