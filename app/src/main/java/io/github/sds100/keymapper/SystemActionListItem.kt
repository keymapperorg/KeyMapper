package io.github.sds100.keymapper

import io.github.sds100.keymapper.Utils.SystemActionUtils

/**
 * Created by sds100 on 31/07/2018.
 */

data class SystemActionListItem(val action: SystemAction,
                                val stringId: Int,
                                val iconId: Int) {

    constructor(action: SystemAction) : this(
            action,
            SystemActionUtils.getDescription(action),
            SystemActionUtils.getIconResource(action)
    )
}