package io.github.sds100.keymapper.util.ui

import io.github.sds100.keymapper.util.Defaultable

/**
 * Created by sds100 on 04/06/20.
 */
data class SliderModel(
    /**
     * null if the default value is set
     */
    val value: Defaultable<Int>,
    val isDefaultStepEnabled: Boolean,
    val min: Int,
    val max: Int,
    val stepSize: Int,
    val customButtonDefaultText: String? = null,
)
