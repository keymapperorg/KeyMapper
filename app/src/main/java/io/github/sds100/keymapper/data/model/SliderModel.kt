package io.github.sds100.keymapper.data.model

import androidx.annotation.IntegerRes

/**
 * Created by sds100 on 04/06/20.
 */
data class SliderModel(val value: Int?,
                       val isDefaultStepEnabled: Boolean,
                       @IntegerRes val min: Int,
                       @IntegerRes val max: Int,
                       @IntegerRes val stepSize: Int)