package io.github.sds100.keymapper.keymaps.detection

import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.keymaps.KeyMap

data class DetectKeyMapModel(
    val keyMap: KeyMap,
    val groupConstraintStates: List<ConstraintState> = emptyList(),
)
