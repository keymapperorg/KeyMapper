package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.common.utils.UndoRedoState

data class ConfigKeyMapScreenState(
    val isEnabled: Boolean = true,
    val showActionPulse: Boolean = false,
    val name: String = "",
    val isEditingName: Boolean = false,
    val undoRedo: UndoRedoState = UndoRedoState(),
)
