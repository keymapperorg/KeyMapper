package io.github.sds100.keymapper.data.db

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.sds100.keymapper.data.db.AppDatabase.Companion.DATABASE_VERSION
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao
import io.github.sds100.keymapper.data.db.dao.FingerprintMapDao
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao
import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao
import io.github.sds100.keymapper.data.db.dao.GroupDao
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.db.dao.LogEntryDao
import io.github.sds100.keymapper.data.db.typeconverter.ActionListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.ConstraintListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.ExtraListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.NodeInteractionTypeSetTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.TriggerTypeConverter
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.data.entities.FingerprintMapEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntity
import io.github.sds100.keymapper.data.entities.GroupEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.entities.LogEntryEntity
import io.github.sds100.keymapper.data.migration.AutoMigration14To15
import io.github.sds100.keymapper.data.migration.AutoMigration15To16
import io.github.sds100.keymapper.data.migration.AutoMigration16To17
import io.github.sds100.keymapper.data.migration.AutoMigration18To19
import io.github.sds100.keymapper.data.migration.AutoMigration19To20
import io.github.sds100.keymapper.data.migration.AutoMigration20To21
import io.github.sds100.keymapper.data.migration.Migration10To11
import io.github.sds100.keymapper.data.migration.Migration11To12
import io.github.sds100.keymapper.data.migration.Migration13To14
import io.github.sds100.keymapper.data.migration.Migration1To2
import io.github.sds100.keymapper.data.migration.Migration2To3
import io.github.sds100.keymapper.data.migration.Migration3To4
import io.github.sds100.keymapper.data.migration.Migration4To5
import io.github.sds100.keymapper.data.migration.Migration5To6
import io.github.sds100.keymapper.data.migration.Migration6To7
import io.github.sds100.keymapper.data.migration.Migration8To9
import io.github.sds100.keymapper.data.migration.Migration9To10

@Database(
    entities = [
        KeyMapEntity::class, FingerprintMapEntity::class,
        LogEntryEntity::class, FloatingLayoutEntity::class,
        FloatingButtonEntity::class, GroupEntity::class, AccessibilityNodeEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        // This adds the button and background opacity columns to the floating button entity
        AutoMigration(from = 14, to = 15, spec = AutoMigration14To15::class),
        // This deletes the folder name column from key maps
        AutoMigration(from = 15, to = 16, spec = AutoMigration15To16::class),
        // This adds last opened timestamp to groups
        AutoMigration(from = 16, to = 17, spec = AutoMigration16To17::class),
        // Adds accessibility node table
        AutoMigration(from = 18, to = 19, spec = AutoMigration18To19::class),
        // Adds interacted, tooltip, and hint fields to accessibility node entity
        AutoMigration(from = 19, to = 20, spec = AutoMigration19To20::class),
        // Adds floating button settings to show over status bar, and show over input method
        AutoMigration(from = 20, to = 21, spec = AutoMigration20To21::class),
    ],
)
@TypeConverters(
    ActionListTypeConverter::class,
    ExtraListTypeConverter::class,
    TriggerTypeConverter::class,
    ConstraintListTypeConverter::class,
    NodeInteractionTypeSetTypeConverter::class,
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "key_map_database"
        const val DATABASE_VERSION = 21

        val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(database: SupportSQLiteDatabase) {
                Migration1To2.migrate(database)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration2To3.migrate(database)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration3To4.migrate(database)
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration4To5.migrate(database)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration5To6.migrate(database)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration6To7.migrate(database)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // DO NOTHING
                // I added a change and then removed it in a later commit. this will only affect testers so not a big
                // deal
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration8To9.migrate(database)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration9To10.migrateDatabase(database)
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration10To11.migrateDatabase(database)
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE `log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `time` INTEGER NOT NULL, `severity` INTEGER NOT NULL, `message` TEXT NOT NULL)",
                )
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration13To14.migrateDatabase(database)
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP INDEX IF EXISTS `index_groups_name`")
            }
        }
    }

    class RoomMigration11To12(private val fingerprintMapDataStore: DataStore<Preferences>) :
        Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Migration11To12.migrateDatabase(database, fingerprintMapDataStore)
        }
    }

    abstract fun keyMapDao(): KeyMapDao
    abstract fun fingerprintMapDao(): FingerprintMapDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun floatingLayoutDao(): FloatingLayoutDao
    abstract fun floatingButtonDao(): FloatingButtonDao
    abstract fun groupDao(): GroupDao
    abstract fun accessibilityNodeDao(): AccessibilityNodeDao
}
