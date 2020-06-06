package io.github.sds100.keymapper.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.sds100.keymapper.data.db.dao.DeviceInfoDao
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.db.typeconverter.ActionListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.ConstraintListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.ExtraListTypeConverter
import io.github.sds100.keymapper.data.db.typeconverter.TriggerTypeConverter
import io.github.sds100.keymapper.data.model.DeviceInfo
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 24/01/2020.
 */
@Database(entities = [KeyMap::class, DeviceInfo::class], version = 2, exportSchema = true)
@TypeConverters(
    ActionListTypeConverter::class,
    ExtraListTypeConverter::class,
    TriggerTypeConverter::class,
    ConstraintListTypeConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        const val DATABASE_NAME = "key_map_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {

            }
        }
    }

    abstract fun keymapDao(): KeyMapDao
    abstract fun deviceInfoDao(): DeviceInfoDao
}