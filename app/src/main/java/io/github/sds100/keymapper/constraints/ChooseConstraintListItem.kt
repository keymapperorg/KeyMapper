package io.github.sds100.keymapper.constraints

/**
 * Created by sds100 on 22/03/2020.
 */
data class ChooseConstraintListItem(
    val id: ChooseConstraintType,
    val title: String,
    val isEnabled: Boolean = true,
    val errorMessage: String? = null
)