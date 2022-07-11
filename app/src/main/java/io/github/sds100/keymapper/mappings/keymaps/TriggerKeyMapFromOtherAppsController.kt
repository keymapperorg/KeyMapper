package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.mappings.detection.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.detection.PerformActionsUseCase
import io.github.sds100.keymapper.mappings.detection.SimpleMappingController
import io.github.sds100.keymapper.mappings.detection.DetectKeyMapsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

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
        val keyMap = keyMapList.find { it.uid == uid }
        if (keyMap != null) {
            onDetected(keyMap.uid, keyMap)

            Timber.d("Triggered key map successfully from Intent, $keyMap")
        }else{
            Timber.d("Failed to trigger key map from intent because key map doesn't exist, uid = $uid")
        }
    }
}