package io.github.sds100.keymapper.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.sds100.keymapper.data.db.AppDatabase.Companion.DATABASE_VERSION
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.db.migration.keymaps.*
import io.github.sds100.keymapper.data.db.typeconverter.ActionListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.ConstraintListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.ExtraListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.TriggerTypeConverter
import io.github.sds100.keymapper.data.model.DeviceInfo
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 24/01/2020.
 */
@Database(entities = [KeyMap::class, DeviceInfo::class], version = DATABASE_VERSION, exportSchema = true)
@TypeConverters(
    ActionListTypeConverter::class,
    ExtraListTypeConverter::class,
    TriggerTypeConverter::class,
    ConstraintListTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "key_map_database"
        const val DATABASE_VERSION = 10

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_1_2.migrate(database)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_2_3.migrate(database)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_3_4.migrate(database)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_4_5.migrate(database)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_5_6.migrate(database)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_6_7.migrate(database)
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                //DO NOTHING
                //I added a change and then removed it in a later commit. this will only affect testers so not a big
                //deal
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_8_9.migrate(database)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Migration_9_10.migrateDatabase(database)
            }
        }
    }

    abstract fun keymapDao(): KeyMapDao
    abstract fun deviceInfoDao(): DeviceInfoDao
}