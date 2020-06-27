package io.github.sds100.keymapper.data.model

import java.io.Serializable

/**
 * Created by sds100 on 27/06/2020.
 */
data class ChooseActionOptions(
    val actionId: String,
    val currentFlags: Int,
    val currentExtras: List<Extra>,
    val allowedFlags: List<Int>
) : Serializable
