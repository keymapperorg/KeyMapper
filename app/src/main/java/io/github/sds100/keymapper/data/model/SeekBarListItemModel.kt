package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 07/05/20.
 */
data class SeekBarListItemModel(
    val id: String,
    val title: String,
    val min: Int,
    val max: Int,
    val stepSize: Int,
    val defaultValue: Int = min
)