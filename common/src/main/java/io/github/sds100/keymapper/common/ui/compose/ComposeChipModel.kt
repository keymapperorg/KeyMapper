package io.github.sds100.keymapper.common.ui.compose

sealed class ComposeChipModel {
    abstract val id: String
    abstract val text: String

    data class Normal(
        override val id: String,
        val icon: ComposeIconInfo?,
        override val text: String,
    ) : ComposeChipModel()

    data class Error(
        override val id: String,
        override val text: String,
        val error: io.github.sds100.keymapper.common.util.result.Error,
        val isFixable: Boolean = true,
    ) : ComposeChipModel()
}
