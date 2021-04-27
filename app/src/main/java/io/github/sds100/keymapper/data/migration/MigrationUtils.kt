package io.github.sds100.keymapper.data.migration

import com.google.gson.Gson

/**
 * Created by sds100 on 24/01/21.
 */
object MigrationUtils {
    fun migrate(
        gson: Gson,
        migrations: List<JsonMigration>,
        inputVersion: Int,
        inputJson: String,
        outputVersion: Int
    ): String {
        var version = inputVersion
        var outputJson = inputJson

        while (version < outputVersion) {
            migrations
                .find { it.versionBefore == version }
                ?.let {
                    outputJson = it.migrate(gson, outputJson)
                    version = it.versionAfter
                }
                ?: throw Exception("No migration for version $version to $outputVersion")
        }

        return outputJson
    }
}