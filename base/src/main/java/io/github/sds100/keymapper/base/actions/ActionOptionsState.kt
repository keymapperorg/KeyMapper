package io.github.sds100.keymapper.base.actions

import androidx.compose.ui.graphics.vector.ImageVector

data class ActionOptionsState(
    val title: String,
    val actionTypeTitle: String,
    val actionTypeIcon: ImageVector,

    val showEditButton: Boolean,

    val showRepeat: Boolean,
    val isRepeatChecked: Boolean,

    val showRepeatRate: Boolean,
    val showRepeatRateWarning: Boolean,
    val repeatRate: Int,
    val defaultRepeatRate: Int,

    val showRepeatDelay: Boolean,
    val repeatDelay: Int,
    val defaultRepeatDelay: Int,

    val showRepeatLimit: Boolean,
    val repeatLimit: Int,
    val defaultRepeatLimit: Int,

    val allowedRepeatModes: Set<RepeatMode>,
    val repeatMode: RepeatMode,

    val showHoldDown: Boolean,
    val isHoldDownChecked: Boolean,

    val showHoldDownDuration: Boolean,
    val holdDownDuration: Int,
    val defaultHoldDownDuration: Int,

    val holdDownMode: HoldDownMode,

    val multiplier: Int,
    val defaultMultiplier: Int,
)
