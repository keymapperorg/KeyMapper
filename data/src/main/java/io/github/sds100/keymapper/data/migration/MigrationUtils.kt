package io.github.sds100.keymapper.data.migration

import com.google.gson.JsonObject


object MigrationUtils {
    fun migrate(
        migrations: List<JsonMigration>,
        inputVersion: Int,
        inputJson: JsonObject,
        outputVersion: Int,
    ): JsonObject {
        var version = inputVersion
        var outputJson = inputJson

        while (version < outputVersion) {
            migrations
                .find { it.versionBefore == version }
                ?.let {
                    outputJson = it.migrate(outputJson)
                    version = it.versionAfter
                }
                ?: throw Exception("No migration for version $version to $outputVersion")
        }

        return outputJson
    }
}
