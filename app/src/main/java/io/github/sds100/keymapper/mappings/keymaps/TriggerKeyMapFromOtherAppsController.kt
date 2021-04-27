package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.mappings.SimpleMappingController
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 11/12/20.
 */
class TriggerKeyMapFromOtherAppsController(
    coroutineScope: CoroutineScope,
    detectKeyMapsUseCase: DetectKeyMapsUseCase,
    performActionsUseCase: PerformActionsUseCase,
    detectConstraintsUseCase: DetectConstraintsUseCase
) : SimpleMappingController(
    coroutineScope,
    detectKeyMapsUseCase,
    performActionsUseCase,
    detectConstraintsUseCase
) {

    private var keyMapList = emptyList<KeyMap>()

    init {
        coroutineScope.launch {
            detectKeyMapsUseCase.keyMapsToTriggerFromOtherApps.collectLatest { keyMaps ->
                reset()
                this@TriggerKeyMapFromOtherAppsController.keyMapList = keyMaps
            }
        }
    }

    fun onDetected(uid: String) {
        val keyMap = keyMapList.find { it.uid == uid } ?: return

        onDetected(keyMap.uid, keyMap)
    }
}