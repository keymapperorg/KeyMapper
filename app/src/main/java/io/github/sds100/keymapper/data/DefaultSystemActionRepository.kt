package io.github.sds100.keymapper.data

import android.content.Context
import io.github.sds100.keymapper.util.SystemActionUtils

/**
 * Created by sds100 on 17/05/2020.
 */
class DefaultSystemActionRepository private constructor(context: Context) : SystemActionRepository {

    companion object {
        @Volatile
        private var instance: DefaultSystemActionRepository? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance
                    ?: DefaultSystemActionRepository(context).also { instance = it }
            }
    }

    override val supportedSystemActions = SystemActionUtils.getSupportedSystemActions(context)
    override val unsupportedSystemActions = SystemActionUtils.getUnsupportedSystemActionsWithReasons(context)
}