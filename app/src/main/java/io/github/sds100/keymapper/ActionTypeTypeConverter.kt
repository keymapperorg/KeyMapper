package io.github.sds100.keymapper

import androidx.room.TypeConverter

/**
 * Created by sds100 on 05/09/2018.
 */

class ActionTypeTypeConverter {
    @TypeConverter
    fun toActionTypeEnum(string: String) = ActionType.valueOf(string)

    @TypeConverter
    fun toString(actionType: ActionType) = actionType.toString()
}