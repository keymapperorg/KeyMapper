package io.github.sds100.keymapper.data.repository

import android.content.Context
import io.github.sds100.keymapper.util.SystemActionUtils

/**
 * Created by sds100 on 17/05/2020.
 */
class DefaultSystemActionRepository(context: Context) : SystemActionRepository {

    override val supportedSystemActions =
        SystemActionUtils.getSupportedSystemActions(context)

    override val unsupportedSystemActions =
        SystemActionUtils.getUnsupportedSystemActionsWithReasons(context)
}