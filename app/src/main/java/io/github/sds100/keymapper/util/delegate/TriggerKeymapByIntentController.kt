package io.github.sds100.keymapper.util.delegate

import io.github.sds100.keymapper.IConstraintDelegate
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.util.IActionError
import io.github.sds100.keymapper.util.triggerFromOtherApps
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 11/12/20.
 */
class TriggerKeymapByIntentController(
    coroutineScope: CoroutineScope,
    iConstraintDelegate: IConstraintDelegate,
    iActionError: IActionError
) : SimpleMappingController(coroutineScope, iConstraintDelegate, iActionError) {

    private var mKeymapList = emptyList<KeyMap>()

    fun onDetected(uid: String) {
        mKeymapList
            .find { it.uid == uid }
            ?.let {
                onDetected(it.uid,
                    it.actionList,
                    it.constraintList,
                    it.constraintMode,
                    it.isEnabled,
                    it.trigger.flags,
                    it.trigger.extras
                )
            }
    }

    fun onKeymapListUpdate(keymapList: List<KeyMap>) {
        reset()

        mKeymapList = keymapList.filter { it.trigger.triggerFromOtherApps }
    }
}