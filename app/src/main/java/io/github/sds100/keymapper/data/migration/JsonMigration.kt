package io.github.sds100.keymapper.data.migration

import com.google.gson.Gson

/**
 * Created by sds100 on 22/01/21.
 */
class JsonMigration(val versionBefore: Int,
                    val versionAfter: Int,
                    val migrate: (gson: Gson, json: String) -> String)