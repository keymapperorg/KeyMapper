package io.github.sds100.keymapper.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.ui.view.SquareImageButton
import io.github.sds100.keymapper.ui.view.StatusLayout
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.getBriefMessage
import io.noties.markwon.Markwon
import kotlinx.android.synthetic.main.list_item_status.view.*

/**
 * Created by sds100 on 25/01/2020.
 */

@BindingAdapter(
    "app:statusLayoutState",
    "app:fixedText",
    "app:warningText",
    "app:errorText",
    "app:showFixButton",
    "app:onFixClick",
    requireAll = false
)
fun StatusLayout.setStatusLayoutState(
    state: StatusLayout.State,
    fixedText: String?,
    warningText: String? = null,
    errorText: String?,
    showFixButton: Boolean = false,
    onFixClick: View.OnClickListener?
) {
    button.isVisible = state != StatusLayout.State.POSITIVE && showFixButton

    button.setOnClickListener(onFixClick)

    when (state) {
        StatusLayout.State.POSITIVE -> {
            textView.text = fixedText
        }

        StatusLayout.State.WARN -> {
            textView.text = warningText

            val color = color(R.color.warn)
            button.setBackgroundColor(color)
        }

        StatusLayout.State.ERROR -> {
            textView.text = errorText

            val color = styledColor(R.attr.colorError)
            button.setBackgroundColor(color)
        }
    }

    val drawable = when (state) {
        StatusLayout.State.POSITIVE -> context.safeVectorDrawable(R.drawable.ic_outline_check_circle_outline_24)
        StatusLayout.State.WARN -> context.safeVectorDrawable(R.drawable.ic_baseline_error_outline_24)
        StatusLayout.State.ERROR -> context.safeVectorDrawable(R.drawable.ic_baseline_error_outline_24)
    }

    val tint = when (state) {
        StatusLayout.State.POSITIVE -> color(R.color.green)
        StatusLayout.State.WARN -> color(R.color.warn)
        StatusLayout.State.ERROR -> styledColor(R.attr.colorError)
    }

    TextViewCompat.setCompoundDrawableTintList(textView, ColorStateList.valueOf(tint))
    textView.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
}

@BindingAdapter("app:onTextChanged")
fun EditText.onTextChangedListener(textWatcher: TextWatcher) {
    addTextChangedListener(textWatcher)
}

@BindingAdapter("app:markdown")
fun TextView.markdown(markdown: DataState<String>) {
    when (markdown) {
        is Data -> Markwon.create(context).apply {
            setMarkdown(this@markdown, markdown.data)
        }

        is Loading, is Empty -> text = ""
    }
}

@BindingAdapter("app:tintType")
fun AppCompatImageView.tintType(tintType: TintType?) {
    tintType ?: clearColorFilter()

    when (tintType) {
        TintType.NONE -> clearColorFilter()
        TintType.ON_SURFACE -> {
            val color = context.styledColor(R.attr.colorOnSurface)

            setColorFilter(color)
        }
        TintType.ERROR -> {
            val color = context.styledColor(R.attr.colorError)

            setColorFilter(color)
        }
    }
}

@BindingAdapter("app:errorWhenEmpty")
fun TextInputLayout.errorWhenEmpty(enabled: Boolean) {

    //need to set it up when the view is created
    if (editText.text.isNullOrBlank()) {
        error = if (enabled) {
            str(R.string.error_cant_be_empty)
        } else {
            null
        }
    }

    editText.addTextChangedListener {
        error = if (it.isNullOrBlank() && enabled) {
            str(R.string.error_cant_be_empty)
        } else {
            null
        }
    }
}

@BindingAdapter("app:errorText")
fun TextInputLayout.errorText(text: String?) {
    error = text
}

@BindingAdapter("app:onLongClick")
fun setLongClickListener(view: View, onLongClickListener: View.OnLongClickListener?) {
    view.setOnLongClickListener(onLongClickListener)
}

@BindingAdapter("app:onCheckedChange")
fun CompoundButton.onCheckedChange(onCheckedChangeListener: CompoundButton.OnCheckedChangeListener) {
    this.setOnCheckedChangeListener(onCheckedChangeListener)
}

@BindingAdapter("app:actions", "app:errorClickCallback", requireAll = true)
fun ChipGroup.bindActions(actions: List<ActionChipModel>, callback: ErrorClickCallback) {

    actions.forEach { action ->
        if (action.hasError) {
            context.errorChipButton(action.error!!, callback).apply {
                addView(this)
            }
        } else {
            val iconTint = if (action.type == ActionType.SYSTEM_ACTION) {
                styledColorSL(R.attr.colorOnSurface)
            } else {
                null
            }

            context.normalChipButton(action.description, action.icon, iconTint).apply {
                addView(this)
            }
        }
    }
}

@BindingAdapter(
    "app:actions",
    "app:constraints",
    "app:constraintMode",
    "app:errorClickCallback",
    requireAll = true
)
fun ChipGroup.bindActionsAndConstraints(
    actions: List<ActionChipModel>,
    constraints: List<ConstraintModel>,
    constraintMode: Int,
    callback: ErrorClickCallback
) {
    removeAllViews()

    bindActions(actions, callback)

    if (actions.isNotEmpty() && constraints.isNotEmpty()) {
        Chip(context).apply {
            text = str(R.string.chip_while)

            isClickable = false
            isFocusable = false
            isCheckable = false
            isEnabled = false

            chipStrokeWidth = 0f

            addView(this)
        }
    }

    bindConstraints(constraints, constraintMode, callback)
}

@BindingAdapter(
    "app:isKeymapEnabled",
    "app:noActions",
    "app:noTrigger",
    "app:actionsHaveErrors",
    requireAll = false
)
fun TextView.setKeymapExtraInfo(
    isKeymapEnabled: Boolean = false,
    noActions: Boolean = false,
    noTrigger: Boolean = false,
    actionsHaveErrors: Boolean = false
) {
    text = buildString {
        val interpunct = str(R.string.interpunct)

        if (!isKeymapEnabled) {
            append(str(R.string.disabled))
        }

        if (actionsHaveErrors) {
            if (this.isNotEmpty()) {
                append(" $interpunct ")
            }

            append(str(R.string.tap_actions_to_fix))
        }

        if (noActions) {
            if (this.isNotEmpty()) {
                append(" $interpunct ")
            }

            append(str(R.string.no_actions))
        }

        if (noTrigger) {
            if (this.isNotEmpty()) {
                append(" $interpunct ")
            }
            append(str(R.string.no_trigger))
        }
    }
}

@BindingAdapter("app:triggerModel")
fun ChipGroup.bindTriggerModel(triggerChipModel: TriggerChipModel) {
    val separatorDrawable = when (triggerChipModel.triggerMode) {
        Trigger.PARALLEL -> drawable(R.drawable.ic_baseline_add_24)
        Trigger.SEQUENCE -> drawable(R.drawable.ic_baseline_arrow_forward_24)
        else -> drawable(R.drawable.ic_baseline_add_24)
    }

    removeAllViews()

    triggerChipModel.triggerKeyDescriptions.forEachIndexed { index, description ->

        //add a chip which is either a + or -> depending on the trigger mode
        if (index != 0) {
            Chip(context).apply {

                chipIcon = separatorDrawable

                chipStrokeWidth = 0f
                textStartPadding = 0f
                textEndPadding = 0f
                setChipIconTintResource(R.color.iconTintTriggerKeySeparator)

                addView(this)
            }
        }

        Chip(context).apply {
            text = description

            addView(this)
        }
    }
}

@BindingAdapter(
    "app:constraints",
    "app:constraintMode",
    "app:errorClickCallback",
    requireAll = true
)
fun ChipGroup.bindConstraints(
    constraintList: List<ConstraintModel>,
    constraintMode: Int,
    callback: ErrorClickCallback
) {
    val separatorText = when (constraintMode) {
        Constraint.MODE_AND -> str(R.string.constraint_mode_and)
        Constraint.MODE_OR -> str(R.string.constraint_mode_or)
        else -> str(R.string.constraint_mode_and)
    }

    constraintList.forEachIndexed { index, model ->

        //add a chip which is either a + or -> depending on the trigger mode
        if (index != 0) {
            Chip(context).apply {
                text = separatorText

                isClickable = false
                isFocusable = false
                isCheckable = false
                isEnabled = false

                chipStrokeWidth = 0f

                addView(this)
            }
        }

        if (model.hasError) {
            context.errorChipButton(model.failure!!, callback).apply {
                addView(this)
            }
        } else {
            val iconTint = if (model.iconTintOnSurface) {
                styledColorSL(R.attr.colorOnSurface)
            } else {
                null
            }

            context.normalChipButton(model.description, model.icon, iconTint).apply {
                addView(this)
            }
        }
    }
}

@BindingAdapter("app:flagModels")
fun ChipGroup.bindFlagModels(flagModels: List<FlagModel>) {
    removeAllViews()

    flagModels.forEach {
        Chip(context).apply {
            text = it.text
            chipIcon = drawable(it.icon!!)

            addView(this)
        }
    }
}

@BindingAdapter("app:onChangeListener")
fun SeekBar.setOnChangeListener(onChangeListener: SeekBar.OnSeekBarChangeListener) {
    setOnSeekBarChangeListener(onChangeListener)
}

@BindingAdapter("app:onSliderChangeListener")
fun Slider.setOnChangeListener(onChangeListener: Slider.OnChangeListener) {
    addOnChangeListener(onChangeListener)
}

@BindingAdapter("app:seekBarEnabled")
fun Slider.enabled(enabled: Boolean) {
    isEnabled = enabled
}

@BindingAdapter("app:customBackgroundTint")
fun MaterialButton.backgroundTint(@ColorInt color: Int) {
    backgroundTintList = ColorStateList.valueOf(color)
}

@BindingAdapter("app:openUrlOnClick")
fun Button.openUrlOnClick(url: String?) {
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

private fun Context.normalChipButton(
    text: String?,
    icon: Drawable?,
    iconTint: ColorStateList?
) = baseChipButton().apply {
    isEnabled = false

    this.text = text
    this.icon = icon
    this.iconTint = iconTint
    setBackgroundColor(styledColor(R.attr.colorSurface))
}

private fun Context.errorChipButton(
    error: Failure,
    callback: ErrorClickCallback
) = baseChipButton().apply {
    isEnabled = true
    icon = context.safeVectorDrawable(R.drawable.ic_outline_error_outline_64)
    setBackgroundColor(color(R.color.cardTintRed))
    iconTint = styledColorSL(R.attr.colorError)

    text = error.getBriefMessage(context)
    setOnClickListener { callback.onErrorClick(error) }
}

private fun Context.baseChipButton() =
    MaterialButton(this, null, R.attr.materialButtonOutlinedStyle).apply {
        id = View.generateViewId()
        setTextColor(styledColorSL(R.attr.colorOnSurface))

        shapeAppearanceModel = ShapeAppearanceModel.builder(
            context,
            R.style.ShapeAppearanceOverlay_MaterialComponents_Chip,
            R.style.ShapeAppearanceOverlay_MaterialComponents_Chip
        ).build()

        TextViewCompat.setTextAppearance(this, R.style.TextAppearance_MaterialComponents_Body2)
        isAllCaps = false

        isClickable = false
        isCheckable = false
        isFocusable = false

        setTextColor(styledColor(R.attr.colorOnSurface))

        iconSize = resources.getDimensionPixelSize(R.dimen.button_chip_icon_size)
    }