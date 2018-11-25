package io.github.sds100.keymapper

import android.content.Context
import io.github.sds100.keymapper.Selection.SelectableItem
import io.github.sds100.keymapper.Utils.ActionUtils

/**
 * Created by sds100 on 25/11/2018.
 */

class KeymapAdapterModel(override val id: Long,
                         val isEnabled: Boolean,
                         val triggerList: List<Trigger>,
                         val actionDescription: ActionDescription) : SelectableItem() {
    companion object {
        fun createModelsFromKeymaps(ctx: Context, keyMapList: List<KeyMap>): List<KeymapAdapterModel> {
            return sequence {
                keyMapList.forEach {
                    val actionDescription = ActionUtils.getDescription(ctx, it.action)

                    yield(KeymapAdapterModel(
                            it.id,
                            it.isEnabled,
                            it.triggerList,
                            actionDescription
                    ))
                }
            }.toList()
        }
    }
}