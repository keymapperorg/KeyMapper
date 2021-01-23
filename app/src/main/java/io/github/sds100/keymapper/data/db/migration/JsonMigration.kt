package io.github.sds100.keymapper.data.db.migration

import com.google.gson.Gson

/**
 * Created by sds100 on 22/01/21.
 */
abstract class JsonMigration(val versionBefore: Int, val versionAfter: Int) {
    abstract fun migrate(gson: Gson, json: String): String
}