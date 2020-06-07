package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 04/06/20.
 */
data class SliderModel(val value: Int?,
                       val isDefaultStepEnabled: Boolean,
                       val min: Int,
                       val max: Int,
                       val stepSize: Int)