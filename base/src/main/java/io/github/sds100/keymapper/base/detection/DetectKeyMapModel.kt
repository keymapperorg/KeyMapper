package io.github.sds100.keymapper.base.detection

import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.keymaps.KeyMap

data class DetectKeyMapModel(
    val keyMap: KeyMap,
    val groupConstraintStates: List<ConstraintState> = emptyList(),
)
