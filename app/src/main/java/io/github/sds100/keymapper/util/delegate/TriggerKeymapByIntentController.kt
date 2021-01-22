package io.github.sds100.keymapper.util.delegate

import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 11/12/20.
 */
class TriggerKeymapByIntentController(
    coroutineScope: CoroutineScope,
    iConstraintDelegate: IConstraintDelegate,
    iActionError: IActionError
) : SimpleMappingController(coroutineScope, iConstraintDelegate, iActionError) {

    private var keymapList = emptyList<KeyMap>()

    fun onDetected(uid: String) {
        keymapList
            .find { it.uid == uid }
            ?.let {
                onDetected(it.uid,
                    it.actionList,
                    it.constraintList,
                    it.constraintMode,
                    it.isEnabled,
                    it.trigger.extras,
                    it.trigger.vibrate,
                    it.trigger.showToast
                )
            }
    }

    fun onKeymapListUpdate(keymapList: List<KeyMap>) {
        reset()

        this.keymapList = keymapList.filter { it.trigger.triggerFromOtherApps }
    }
}