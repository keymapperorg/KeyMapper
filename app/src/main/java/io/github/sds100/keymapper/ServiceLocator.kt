package io.github.sds100.keymapper

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import io.github.sds100.keymapper.data.DefaultPreferenceDataStore
import io.github.sds100.keymapper.data.IPreferenceDataStore
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.repository.*
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 17/05/2020.
 */
object ServiceLocator {

    private val lock = Any()
    private var database: AppDatabase? = null

    @Volatile
    private var keymapRepository: DefaultKeymapRepository? = null

    @Volatile
    private var deviceInfoRepository: DeviceInfoRepository? = null

    @Volatile
    private var preferenceDataStore: IPreferenceDataStore? = null

    @Volatile
    private var fingerprintMapRepository: FingerprintMapRepository? = null

    @Volatile
    private var fileRepository: FileRepository? = null

    @Volatile
    private var systemActionRepository: SystemActionRepository? = null

    @Volatile
    private var packageRepository: DefaultPackageRepository? = null

    fun keymapRepository(context: Context): DefaultKeymapRepository {
        synchronized(this) {
            return keymapRepository ?: createKeymapRepository(context)
        }
    }

    fun deviceInfoRepository(context: Context): DeviceInfoRepository {
        synchronized(this) {
            return deviceInfoRepository ?: createDeviceInfoRepository(context)
        }
    }

    fun preferenceDataStore(context: Context): IPreferenceDataStore {
        synchronized(this) {
            return preferenceDataStore ?: createPreferenceDataStore(context)
        }
    }

    fun fingerprintMapRepository(context: Context): FingerprintMapRepository {
        synchronized(this) {
            return fingerprintMapRepository
                ?: createFingerprintMapRepository(context)
        }
    }

    fun fileRepository(context: Context): FileRepository {
        synchronized(this) {
            return fileRepository ?: createFileRepository(context)
        }
    }

    fun systemActionRepository(context: Context): SystemActionRepository {
        synchronized(this) {
            return systemActionRepository ?: createSystemActionRepository(context)
        }
    }

    fun packageRepository(context: Context): PackageRepository {
        synchronized(this) {
            return packageRepository ?: createPackageRepository(context)
        }
    }

    @VisibleForTesting
    fun resetKeymapRepository() {
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
        val database = database ?: createDatabase(context.applicationContext)
        keymapRepository = DefaultKeymapRepository(database.keymapDao())
        return keymapRepository!!
    }

    private fun createDeviceInfoRepository(context: Context): DeviceInfoRepository {
        val database = database ?: createDatabase(context.applicationContext)
        deviceInfoRepository = DefaultDeviceInfoRepository(database.deviceInfoDao())
        return deviceInfoRepository!!
    }

    private fun createPreferenceDataStore(context: Context): IPreferenceDataStore {
        return preferenceDataStore
            ?: DefaultPreferenceDataStore(context.applicationContext).also {
                this.preferenceDataStore = it
            }
    }

    private fun createFingerprintMapRepository(context: Context): FingerprintMapRepository {
        val dataStore = preferenceDataStore(context).fingerprintGestureDataStore

        return fingerprintMapRepository
            ?: FingerprintMapRepository(dataStore).also {
                this.fingerprintMapRepository = it
            }
    }

    private fun createSystemActionRepository(context: Context): SystemActionRepository {
        return systemActionRepository
            ?: DefaultSystemActionRepository(context.applicationContext).also {
                this.systemActionRepository = it
            }
    }

    private fun createPackageRepository(context: Context): PackageRepository {
        return packageRepository
            ?: DefaultPackageRepository(context.packageManager).also {
                this.packageRepository = it
            }
    }

    private fun createFileRepository(context: Context): FileRepository {
        return fileRepository
            ?: FileRepository(context.applicationContext).also {
                this.fileRepository = it
            }
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
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9).build()
        database = result
        return result
    }

    fun release() {
        synchronized(this) {
            packageRepository = null
            systemActionRepository = null
        }
    }
}