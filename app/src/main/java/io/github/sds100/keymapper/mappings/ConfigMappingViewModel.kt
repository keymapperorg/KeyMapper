package io.github.sds100.keymapper.mappings

import android.os.Bundle
import io.github.sds100.keymapper.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 17/01/21.
 */

interface ConfigMappingViewModel {
    val state: StateFlow<ConfigMappingUiState>
    fun setEnabled(enabled: Boolean)

    val configActionsViewModel: ConfigActionsViewModel<*, *>
    val editActionViewModel: EditActionViewModel<*, *>
    val configConstraintsViewModel: ConfigConstraintsViewModel

    fun save()
    fun saveState(outState: Bundle)
    fun restoreState(state: Bundle)
}

interface ConfigMappingUiState {
    val isEnabled: Boolean
}
