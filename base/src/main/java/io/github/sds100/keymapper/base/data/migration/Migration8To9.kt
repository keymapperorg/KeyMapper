@file:Suppress("ClassName")

package io.github.sds100.keymapper.base.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import java.util.UUID

/**
 * Add UUIDs to all keymaps.
 */
object Migration8To9 {

    fun migrate(database: SupportSQLiteDatabase) = database.apply {
        database.execSQL("ALTER TABLE keymaps ADD COLUMN 'uid' TEXT NOT NULL DEFAULT ''")

        val query = SupportSQLiteQueryBuilder
            .builder("keymaps")
            .columns(arrayOf("id", "uid"))
            .create()

        query(query).apply {
            while (moveToNext()) {
                val idColumnIndex = getColumnIndex("id")
                val id = getInt(idColumnIndex)

                val uid = UUID.randomUUID().toString()

                execSQL("UPDATE keymaps SET uid='$uid', flags=0 WHERE id=$id")
            }

            close()
        }
    }
}
