package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 25/01/2020.
 */

data class KeymapListItemModel(
    val id: Long,
    val actionList: List<ActionChipModel>,
    val triggerModel: TriggerModel,
    val constraintList: List<ConstraintModel>,
    val constraintMode: Int,
    val flagList: List<FlagModel>,
    val isEnabled: Boolean
)