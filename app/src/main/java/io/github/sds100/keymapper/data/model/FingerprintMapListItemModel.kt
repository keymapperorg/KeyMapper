package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 08/11/20.
 */

data class FingerprintMapListItemModel(
    val id: String,
    val header: String,
    val actionModels: List<ActionChipModel>,
    val constraintMode: Int,
    val constraintModels: List<ConstraintModel>,
    val optionsDescription: String,
    val isEnabled: Boolean
) {
    val hasActions: Boolean
        get() = actionModels.isNotEmpty()

    val hasConstraints: Boolean
        get() = constraintModels.isNotEmpty()

    val hasOptions: Boolean
        get() = optionsDescription.isNotBlank()

    val actionsHaveErrors: Boolean
        get() = actionModels.any { it.hasError }
}