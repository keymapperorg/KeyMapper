package io.github.sds100.keymapper.base.constraints

import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo

data class ConstraintListItemModel(
    val id: String,
    val icon: ComposeIconInfo,
    val text: String,
    val isNot: Boolean = false,
    val error: String? = null,
    val isErrorFixable: Boolean = true,
)

data class ConstraintGroupListItemModel(
    val uid: String,
    val name: String? = null,
    val mode: ConstraintMode,
    val constraints: List<ConstraintListItemModel>,
    val description: String,
) {
    val error: String? = constraints.firstNotNullOfOrNull { it.error }
}
