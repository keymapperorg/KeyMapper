package io.github.sds100.keymapper.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.typeconverter.ActionTypeTypeConverter
import io.github.sds100.keymapper.typeconverter.ExtraListTypeConverter
import io.github.sds100.keymapper.typeconverter.TriggerListTypeConverter

/**
 * Created by sds100 on 05/09/2018.
 */

@Database(version = 1, entities = [KeyMap::class], exportSchema = false)
@TypeConverters(
        TriggerListTypeConverter::class,
        ActionTypeTypeConverter::class,
        ExtraListTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private const val DATABASE_NAME = "key_map_database"

        private var INSTANCE: AppDatabase? = null

        fun getInstance(ctx: Context): AppDatabase {

            if (INSTANCE == null) {
                //must be application context to prevent memory leaking
                INSTANCE = Room.databaseBuilder(
                        ctx.applicationContext,
                        AppDatabase::class.java,
                        DATABASE_NAME
                ).build()
            }

            return INSTANCE!!
        }
    }

    abstract fun keyMapDao(): KeyMapDao
}