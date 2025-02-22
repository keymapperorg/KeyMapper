package io.github.sds100.keymapper.data.migration

import androidx.sqlite.db.SupportSQLiteDatabase

object Migration13To14 {
    fun migrateDatabase(database: SupportSQLiteDatabase) {
        // Create floating layout table
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS `floating_layouts` (`uid` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`uid`))""",
        )

        // Create floating button table
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS `floating_buttons` (`uid` TEXT NOT NULL, `layout_uid` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `button_size` INTEGER NOT NULL, `x` INTEGER NOT NULL, `y` INTEGER NOT NULL, `orientation` TEXT NOT NULL, `display_width` INTEGER NOT NULL, `display_height` INTEGER NOT NULL, PRIMARY KEY(`uid`), FOREIGN KEY(`layout_uid`) REFERENCES `floating_layouts`(`uid`) ON UPDATE NO ACTION ON DELETE CASCADE )""",
        )
    }
}
