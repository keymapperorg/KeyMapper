package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import io.github.sds100.keymapper.actions.uielement.NodeInteractionType

/**
 * Created by sds100 on 05/09/2018.
 */

class NodeInteractionTypeSetTypeConverter {
    @TypeConverter
    fun toSet(mask: Int): Set<NodeInteractionType> {
        val interactionTypeSet = mutableSetOf<NodeInteractionType>()

        for (type in NodeInteractionType.entries) {
            if (mask and type.accessibilityActionId == type.accessibilityActionId) {
                interactionTypeSet.add(type)
            }
        }

        return interactionTypeSet
    }

    @TypeConverter
    fun toMask(set: Set<NodeInteractionType>): Int {
        var nodeActionMask = 0

        for (nodeAction in set) {
            nodeActionMask = nodeActionMask or nodeAction.accessibilityActionId
        }

        return nodeActionMask
    }
}
