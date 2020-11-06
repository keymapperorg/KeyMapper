package io.github.sds100.keymapper

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import io.github.sds100.keymapper.data.IOnboardingState
import io.github.sds100.keymapper.data.OnboardingState
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.repository.DefaultDeviceInfoRepository
import io.github.sds100.keymapper.data.repository.DefaultKeymapRepository
import io.github.sds100.keymapper.data.repository.DeviceInfoRepository
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 17/05/2020.
 */
object ServiceLocator {

    private val lock = Any()
    private var database: AppDatabase? = null

    @Volatile
    var keymapRepository: DefaultKeymapRepository? = null
        @VisibleForTesting set

    @Volatile
    var deviceInfoRepository: DeviceInfoRepository? = null
        @VisibleForTesting set

    @Volatile
    var onboardingState: IOnboardingState? = null
        @VisibleForTesting set

    fun provideKeymapRepository(context: Context): DefaultKeymapRepository {
        synchronized(this) {
            return keymapRepository ?: createKeymapRepository(context)
        }
    }

    fun provideDeviceInfoRepository(context: Context): DeviceInfoRepository {
        synchronized(this) {
            return deviceInfoRepository ?: createDeviceInfoRepository(context)
        }
    }

    fun provideOnboardingState(context: Context): IOnboardingState {
        synchronized(this) {
            return onboardingState ?: createOnboardingState(context)
        }
    }

    @VisibleForTesting
    fun resetRepository() {
        synchronized(lock) {
            runBlocking {
                keymapRepository?.deleteAll()
                deviceInfoRepository?.deleteAll()
            }

            database?.apply {
                clearAllTables()
                close()
            }

            database = null
            keymapRepository = null
            deviceInfoRepository = null
        }
    }

    private fun createKeymapRepository(context: Context): DefaultKeymapRepository {
        val database = database ?: createDatabase(context)
        keymapRepository = DefaultKeymapRepository(database.keymapDao())
        return keymapRepository!!
    }

    private fun createDeviceInfoRepository(context: Context): DeviceInfoRepository {
        val database = database ?: createDatabase(context)
        deviceInfoRepository = DefaultDeviceInfoRepository(database.deviceInfoDao())
        return deviceInfoRepository!!
    }

    private fun createOnboardingState(context: Context): IOnboardingState {
        val onboardingState = onboardingState ?: OnboardingState(context)
        this.onboardingState = onboardingState

        return onboardingState
    }

    private fun createDatabase(context: Context): AppDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java, AppDatabase.DATABASE_NAME
        ).addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8).build()
        database = result
        return result
    }
}