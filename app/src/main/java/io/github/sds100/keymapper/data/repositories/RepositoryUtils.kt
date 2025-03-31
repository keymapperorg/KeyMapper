package io.github.sds100.keymapper.data.repositories

import android.database.sqlite.SQLiteConstraintException

object RepositoryUtils {
    suspend fun <T> saveUniqueName(
        entity: T,
        saveBlock: suspend (entity: T) -> Unit,
        renameBlock: (entity: T, suffix: String) -> T,
    ): T {
        var group = entity
        var count = 0

        while (count < 1000) {
            // Insert must be suspending so we only update the layout uid once the layout
            // has been saved.
            try {
                saveBlock(group)
                break
            } catch (_: SQLiteConstraintException) {
                // If the name already exists try creating it with a new name.
                group = renameBlock(entity, "(${count + 1})")
                count++
            }
        }

        return group
    }
}
