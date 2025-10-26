package io.github.sds100.keymapper.data.db

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.data.db.dao.AccessibilityNodeDao
import io.github.sds100.keymapper.data.db.dao.FingerprintMapDao
import io.github.sds100.keymapper.data.db.dao.FloatingButtonDao
import io.github.sds100.keymapper.data.db.dao.FloatingLayoutDao
import io.github.sds100.keymapper.data.db.dao.GroupDao
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.db.dao.LogEntryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal class AppDatabaseModule {
    @Singleton
    @Provides
    fun provideAppDatabase(
        @ApplicationContext ctx: Context,
    ): AppDatabase = createDatabase(ctx)

    @Provides
    fun provideKeyMapDao(database: AppDatabase): KeyMapDao = database.keyMapDao()

    @Provides
    fun provideFingerprintMapDao(database: AppDatabase): FingerprintMapDao = database.fingerprintMapDao()

    @Provides
    fun provideLogEntryDao(database: AppDatabase): LogEntryDao = database.logEntryDao()

    @Provides
    fun provideFloatingLayoutDao(database: AppDatabase): FloatingLayoutDao = database.floatingLayoutDao()

    @Provides
    fun provideFloatingButtonDao(database: AppDatabase): FloatingButtonDao = database.floatingButtonDao()

    @Provides
    fun provideGroupDao(database: AppDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideAccessibilityNodeDao(database: AppDatabase): AccessibilityNodeDao = database.accessibilityNodeDao()

    private fun createDatabase(context: Context): AppDatabase =
        Room
            .databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DATABASE_NAME,
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
                AppDatabase.RoomMigration11To12(context.applicationContext.legacyFingerprintMapDataStore),
                AppDatabase.MIGRATION_12_13,
                AppDatabase.MIGRATION_13_14,
                AppDatabase.MIGRATION_17_18,
            ).build()

    private val Context.legacyFingerprintMapDataStore by preferencesDataStore("fingerprint_gestures")
}
