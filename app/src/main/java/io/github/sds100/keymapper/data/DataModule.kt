package io.github.sds100.keymapper.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.repositories.RoomFingerprintMapRepository
import io.github.sds100.keymapper.data.repositories.RoomKeyMapRepository
import io.github.sds100.keymapper.data.repositories.RoomLogRepository
import io.github.sds100.keymapper.logging.LogRepository
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.util.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * Created by sds100 on 28/06/2022.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    private val Context.legacyFingerprintMapDataStore by preferencesDataStore("fingerprint_gestures")

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return createDatabase(ctx)
    }

    private fun createDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.RoomMigration_11_12(context.applicationContext.legacyFingerprintMapDataStore),
            AppDatabase.MIGRATION_12_13
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideLogRepository(
        database: AppDatabase,
        scope: CoroutineScope
    ): LogRepository {
        return RoomLogRepository(scope, database.logEntryDao())
    }

    @Provides
    @Singleton
    fun provideFingerprintMapRepository(
        database: AppDatabase,
        devicesAdapter: DevicesAdapter,
        scope: CoroutineScope
    ): FingerprintMapRepository {
        return RoomFingerprintMapRepository(database.fingerprintMapDao(), scope, devicesAdapter)
    }

    @Provides
    @Singleton
    fun provideKeyMapRepository(
        database: AppDatabase,
        devicesAdapter: DevicesAdapter,
        scope: CoroutineScope,
        dispatchers: DispatcherProvider
    ): KeyMapRepository {
        return RoomKeyMapRepository(database.keymapDao(), devicesAdapter, scope, dispatchers)
    }
}