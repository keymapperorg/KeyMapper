package io.github.sds100.keymapper.base.keymaps.detection

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.scopes.ServiceScoped
import io.github.sds100.keymapper.base.actions.PerformActionsUseCase
import io.github.sds100.keymapper.base.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.keymaps.SimpleMappingController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@ServiceScoped
class TriggerKeyMapFromOtherAppsController @AssistedInject constructor(
    @Assisted
    coroutineScope: CoroutineScope,
    detectKeyMapsUseCase: DetectKeyMapsUseCase,
    performActionsUseCase: PerformActionsUseCase,
    detectConstraintsUseCase: DetectConstraintsUseCase,
) : SimpleMappingController(
    coroutineScope,
    detectKeyMapsUseCase,
    performActionsUseCase,
    detectConstraintsUseCase,
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
            onDetected(keyMap)

            Timber.d("Triggered key map successfully from Intent, $keyMap")
        } else {
            Timber.d("Failed to trigger key map from intent because key map doesn't exist, uid = $uid")
        }
    }
}
