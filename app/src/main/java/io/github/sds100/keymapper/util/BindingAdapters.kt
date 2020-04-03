package io.github.sds100.keymapper.util

import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.*
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import splitties.resources.appDrawable
import splitties.resources.appStr
import splitties.resources.styledColor
import splitties.resources.styledColorSL


/**
 * Created by sds100 on 25/01/2020.
 */

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
    if (editText?.text.isNullOrBlank()) {
        error = appStr(R.string.error_cant_be_empty)
    }

    editText?.addTextChangedListener {
        error = if (it.isNullOrBlank() && enabled) {
            appStr(R.string.error_cant_be_empty)
        } else {
            null
        }
    }
}

@BindingAdapter("app:onLongClick")
fun setLongClickListener(view: View, onLongClickListener: View.OnLongClickListener) {
    view.setOnLongClickListener(onLongClickListener)
}

@BindingAdapter("app:onCheckedChange")
fun CompoundButton.onCheckedChange(onCheckedChangeListener: CompoundButton.OnCheckedChangeListener) {
    this.setOnCheckedChangeListener(onCheckedChangeListener)
}

@BindingAdapter("app:actions", "app:errorClickCallback", requireAll = true)
fun ChipGroup.bindActions(actions: List<ActionChipModel>, callback: ErrorClickCallback) {

    actions.forEach {
        Chip(context).apply {
            text = it.description
            chipIcon = it.icon
            isCloseIconVisible = it.hasError

            if (it.type == ActionType.SYSTEM_ACTION) {
                chipIconTint = context.styledColorSL(R.attr.colorOnSurface)
            }

            if (it.description == null && it.hasError) {
                text = it.error?.briefMessage
            }

            if (it.hasError) {
                isClickable = true
                setOnClickListener { _ ->
                    callback.onErrorClick(it.error!!)
                }
            } else {
                isClickable = false
            }

            addView(this)
        }
    }
}

@BindingAdapter("app:actions", "app:constraints", "app:constraintMode", "app:errorClickCallback", requireAll = true)
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
            text = appStr(R.string.chip_while)

            chipStrokeWidth = 0f

            addView(this)
        }
    }

    bindConstraints(constraints, constraintMode, callback)
}

@BindingAdapter("app:isKeymapEnabled", "app:noActions", "app:noTrigger", requireAll = false)
fun TextView.setKeymapExtraInfo(isKeymapEnabled: Boolean = false, noActions: Boolean = false, noTrigger: Boolean = false) {
    text = buildString {
        val interpunct = appStr(R.string.interpunct)

        if (!isKeymapEnabled) {
            append(appStr(R.string.disabled))
        }

        if (noActions) {
            if (this.isNotEmpty()) {
                append(" $interpunct ")
            }

            append(appStr(R.string.no_actions))
        }

        if (noTrigger) {
            if (this.isNotEmpty()) {
                append(" $interpunct ")
            }
            append(appStr(R.string.no_trigger))
        }
    }
}

@BindingAdapter("app:triggerModel")
fun ChipGroup.bindTriggerModel(triggerChipModel: TriggerChipModel) {
    val separatorDrawable = when (triggerChipModel.triggerMode) {
        Trigger.PARALLEL -> appDrawable(R.drawable.ic_baseline_add_24)
        Trigger.SEQUENCE -> appDrawable(R.drawable.ic_baseline_arrow_forward_24)
        else -> appDrawable(R.drawable.ic_baseline_add_24)
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

@BindingAdapter("app:constraints", "app:constraintMode", "app:errorClickCallback", requireAll = true)
fun ChipGroup.bindConstraints(
    constraintList: List<ConstraintModel>,
    constraintMode: Int,
    callback: ErrorClickCallback
) {
    val separatorText = when (constraintMode) {
        Constraint.MODE_AND -> appStr(R.string.constraint_mode_and)
        Constraint.MODE_OR -> appStr(R.string.constraint_mode_or)
        else -> appStr(R.string.constraint_mode_and)
    }

    constraintList.forEachIndexed { index, model ->

        //add a chip which is either a + or -> depending on the trigger mode
        if (index != 0) {
            Chip(context).apply {
                text = separatorText

                chipStrokeWidth = 0f

                addView(this)
            }
        }

        Chip(context).apply {
            text = model.description
            chipIcon = model.icon
            isCloseIconVisible = model.hasError

            if (model.description == null && model.hasError) {
                text = model.failure?.briefMessage
            }

            if (model.hasError) {
                isClickable = true
                setOnClickListener {
                    callback.onErrorClick(model.failure!!)
                }
            }

            addView(this)
        }
    }
}

@BindingAdapter("app:flagModels")
fun ChipGroup.bindFlagModels(flagModels: List<FlagModel>) {
    removeAllViews()

    flagModels.forEach {
        Chip(context).apply {
            text = it.text
            chipIcon = appDrawable(it.icon!!)

            addView(this)
        }
    }
}