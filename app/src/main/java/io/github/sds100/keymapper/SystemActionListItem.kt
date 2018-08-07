package io.github.sds100.keymapper

/**
 * Created by sds100 on 31/07/2018.
 */

data class SystemActionListItem(val action: SystemAction,
                                val stringId: Int,
                                val iconId: Int) {

    constructor(action: SystemAction) : this(
            action,
            SystemActionHelper.getDescription(action),
            SystemActionHelper.getIconResource(action)
    )
}