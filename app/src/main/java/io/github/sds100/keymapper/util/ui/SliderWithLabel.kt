package io.github.sds100.keymapper.util.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.databinding.BindingAdapter
import com.google.android.material.slider.Slider
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 04/06/20.
 */
class SliderWithLabel(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
) : FrameLayout(context, attrs, defStyleAttr) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    private val slider by lazy { findViewById<Slider>(R.id.slider) }
    private val sliderValue by lazy { findViewById<Button>(R.id.textViewSliderValue) }

    private var isDefaultStepEnabled = false

    private var buttonDefaultText = str(R.string.slider_default)

    init {
        inflate(context, R.layout.slider_with_label, this)

        slider.addOnChangeListener { _, value, _ ->
            setSliderValueTextViewText(value)
        }

        slider.setLabelFormatter {
            // Set text to "default" if the slider is in the "default" step position.
            if (isDefaultStepEnabled && it == slider.valueFrom) {
                buttonDefaultText
            } else {
                try {
                    it.toInt().toString()
                } catch (e: NumberFormatException) {
                    it.toString()
                }
            }
        }
    }

    fun applyModel(model: SliderModel) {
        val min = model.min
        val max = model.max
        var stepSize = model.stepSize

        if (model.customButtonDefaultText != null) {
            buttonDefaultText = model.customButtonDefaultText
        } else {
            buttonDefaultText = str(R.string.slider_default)
        }

        if (model.value is Defaultable.Custom) {
            if (model.value.data % stepSize != 0 || model.value.data > max) {
                stepSize = 1
            }
        }

        val defaultStepValue = calculateDefaultStepValue(min.toFloat(), stepSize.toFloat())

        slider.valueFrom = if (model.isDefaultStepEnabled) {
            defaultStepValue
        } else {
            min.toFloat()
        }

        slider.valueTo = max.toFloat()

        slider.stepSize = stepSize.toFloat()
        isDefaultStepEnabled = model.isDefaultStepEnabled

        if (model.value is Defaultable.Custom) {
            when {
                model.value.data > max -> {
                    // set the max slider value to a multiple of the step size greater than the value
                    val remainder = if (stepSize == 0) {
                        0
                    } else {
                        model.value.data % stepSize
                    }

                    slider.valueTo = ((model.value.data + stepSize) - remainder).toFloat()
                    slider.value = model.value.data.toFloat()
                }

                model.value.data < min -> slider.value = min.toFloat()

                else -> slider.value = model.value.data.toFloat()
            }
        } else {
            slider.value = defaultStepValue
        }

        setSliderValueTextViewText(slider.value)
    }

    fun setOnSliderValueClickListener(onClickListener: OnClickListener) {
        sliderValue.setOnClickListener(onClickListener)
    }

    fun setOnSliderChangeListener(onChangeListener: Slider.OnChangeListener) {
        slider.clearOnChangeListeners()
        slider.addOnChangeListener(onChangeListener)
    }

    fun setOnSliderTouchListener(listener: Slider.OnSliderTouchListener) {
        slider.clearOnSliderTouchListeners()
        slider.addOnSliderTouchListener(listener)
    }

    private fun calculateDefaultStepValue(min: Float, stepSize: Float): Float = min - stepSize

    private fun setSliderValueTextViewText(value: Float) {
        // Set text to "default" if the slider is in the "default" step position.
        if (isDefaultStepEnabled && value == slider.valueFrom) {
            sliderValue.text = buttonDefaultText
        } else {
            val text = try {
                value.toInt().toString()
            } catch (e: NumberFormatException) {
                value.toString()
            }

            sliderValue.text = text
        }
    }
}

@BindingAdapter("app:sliderModel")
fun SliderWithLabel.setModel(model: SliderModel) {
    applyModel(model)
}

@BindingAdapter("app:onChangeListener")
fun SliderWithLabel.onChangeListener(onChangeListener: Slider.OnChangeListener) {
    setOnSliderChangeListener(onChangeListener)
}

@BindingAdapter("app:onSliderValueClickListener")
fun SliderWithLabel.onSliderValueClickListener(clickListener: View.OnClickListener) {
    setOnSliderValueClickListener(clickListener)
}

@BindingAdapter("app:onSliderTouchListener")
fun SliderWithLabel.onSliderTouchListener(listener: Slider.OnSliderTouchListener) {
    setOnSliderTouchListener(listener)
}
