package io.github.sds100.keymapper.typeconverter

import androidx.room.TypeConverter
import io.github.sds100.keymapper.ActionType

/**
 * Created by sds100 on 05/09/2018.
 */

class ActionTypeTypeConverter {
    @TypeConverter
    fun toActionTypeEnum(string: String) = ActionType.valueOf(string)

    @TypeConverter
    fun toString(actionType: ActionType) = actionType.toString()
}