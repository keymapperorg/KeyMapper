package io.github.sds100.keymapper.data.migration.fingerprintmaps

import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.set
import com.google.gson.JsonObject
import io.github.sds100.keymapper.common.utils.hasFlag
import io.github.sds100.keymapper.common.utils.minusFlag
import io.github.sds100.keymapper.common.utils.withFlag

/**
 * Move the action option "show performing toast when performing" to a trigger option.
 */
object FingerprintMapMigration0To1 {
    private const val NAME_VERSION = "db_version"
    private const val NAME_ACTION_LIST = "action_list"
    private const val FLAG_ACTION_SHOW_PERFORMING_TOAST = 2
    private const val FLAG_SHOW_TOAST = 2
    private const val TRIGGER_NAME_FLAGS = "flags"
    private const val ACTION_NAME_FLAGS = "flags"

    fun migrate(fingerprintMap: JsonObject): JsonObject {
        val actionListJsonArray by fingerprintMap.byArray(NAME_ACTION_LIST)

        var showToast = false

        actionListJsonArray.forEach {
            val flags by it.byInt(ACTION_NAME_FLAGS)

            if (flags.hasFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)) showToast = true

            it[ACTION_NAME_FLAGS] = flags.minusFlag(FLAG_ACTION_SHOW_PERFORMING_TOAST)
        }

        fingerprintMap[NAME_ACTION_LIST] = actionListJsonArray

        if (showToast) {
            val flags by fingerprintMap.byInt(TRIGGER_NAME_FLAGS)

            fingerprintMap[TRIGGER_NAME_FLAGS] = flags.withFlag(FLAG_SHOW_TOAST)
        }

        fingerprintMap[NAME_VERSION] = 1
        return fingerprintMap
    }
}
