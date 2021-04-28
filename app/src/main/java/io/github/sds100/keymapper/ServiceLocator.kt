package io.github.sds100.keymapper

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.repositories.*
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapRepository
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.airplanemode.AirplaneModeAdapter
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.bluetooth.BluetoothAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.devices.DeviceInfoCache
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.intents.IntentAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.nfc.NfcAdapter
import io.github.sds100.keymapper.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.url.OpenUrlAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.system.volume.VolumeAdapter
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 17/05/2020.
 */
object ServiceLocator {

    private val lock = Any()
    private var database: AppDatabase? = null

    @Volatile
    private var deviceInfoRepository: DeviceInfoCache? = null

    fun deviceInfoRepository(context: Context): DeviceInfoCache {
        synchronized(this) {
            return deviceInfoRepository ?: createDeviceInfoRepository(context)
        }
    }

    @Volatile
    private var roomKeymapRepository: RoomKeyMapRepository? = null

    fun roomKeymapRepository(context: Context): RoomKeyMapRepository {
        synchronized(this) {
            val dataBase = database ?: createDatabase(context.applicationContext)

            return roomKeymapRepository ?: RoomKeyMapRepository(
                dataBase.keymapDao(),
                (context.applicationContext as KeyMapperApp).appCoroutineScope
            ).also {
                this.roomKeymapRepository = it
            }
        }
    }

    private fun createDeviceInfoRepository(context: Context): DeviceInfoCache {
        val database = database ?: createDatabase(context.applicationContext)
        deviceInfoRepository = RoomDeviceInfoCache(
            database.deviceInfoDao(),
            (context.applicationContext as KeyMapperApp).appCoroutineScope
        )
        return deviceInfoRepository!!
    }

    @Volatile
    private var fingerprintMapRepository: FingerprintMapRepository? = null

    fun fingerprintMapRepository(context: Context): FingerprintMapRepository {
        synchronized(this) {
            return fingerprintMapRepository ?: createFingerprintMapRepository(context)
        }
    }

    private val Context.fingerprintMapDataStore by preferencesDataStore("fingerprint_gestures")
    private fun createFingerprintMapRepository(context: Context): FingerprintMapRepository {
        val scope = (context.applicationContext as KeyMapperApp).appCoroutineScope

        return fingerprintMapRepository
            ?: DataStoreFingerprintMapRepository(context.fingerprintMapDataStore, scope).also {
                this.fingerprintMapRepository = it
            }
    }

    @Volatile
    private var preferenceRepository: PreferenceRepository? = null

    fun preferenceRepository(context: Context): PreferenceRepository {
        synchronized(this) {
            return preferenceRepository ?: createPreferenceRepository(context)
        }
    }

    private fun createPreferenceRepository(context: Context): PreferenceRepository {

        return preferenceRepository
            ?: DataStorePreferenceRepository(
                context.applicationContext,
                (context.applicationContext as KeyMapperApp).appCoroutineScope
            ).also {
                this.preferenceRepository = it
            }
    }

    @Volatile
    private var backupManager: BackupManager? = null

    fun backupManager(context: Context): BackupManager {
        synchronized(this) {
            return backupManager ?: createBackupManager(context)
        }
    }

    private fun createBackupManager(context: Context): BackupManager {
        return backupManager ?: BackupManagerImpl(
            (context.applicationContext as KeyMapperApp).appCoroutineScope,
            fileAdapter(context),
            roomKeymapRepository(context),
            deviceInfoRepository(context),
            preferenceRepository(context),
            fingerprintMapRepository(context)
        ).also {
            this.backupManager = it
        }
    }

    fun fileAdapter(context: Context): FileAdapter {
        return (context.applicationContext as KeyMapperApp).fileAdapter
    }

    fun inputMethodAdapter(context: Context): InputMethodAdapter {
        return (context.applicationContext as KeyMapperApp).inputMethodAdapter
    }

    fun devicesAdapter(context: Context): DevicesAdapter {
        return (context.applicationContext as KeyMapperApp).devicesAdapter
    }

    fun bluetoothAdapter(context: Context): BluetoothAdapter {
        return (context.applicationContext as KeyMapperApp).bluetoothMonitor
    }

    fun notificationController(context: Context): NotificationController {
        return (context.applicationContext as KeyMapperApp).notificationController
    }

    fun resourceProvider(context: Context): ResourceProvider {
        return (context.applicationContext as KeyMapperApp).resourceProvider
    }

    fun packageManagerAdapter(context: Context): PackageManagerAdapter {
        return (context.applicationContext as KeyMapperApp).packageManagerAdapter
    }

    fun cameraAdapter(context: Context): CameraAdapter {
        return (context.applicationContext as KeyMapperApp).cameraAdapter
    }

    fun permissionAdapter(context: Context): AndroidPermissionAdapter {
        return (context.applicationContext as KeyMapperApp).permissionAdapter
    }

    fun systemFeatureAdapter(context: Context): SystemFeatureAdapter {
        return (context.applicationContext as KeyMapperApp).systemFeatureAdapter
    }

    fun serviceAdapter(context: Context): AccessibilityServiceAdapter {
        return (context.applicationContext as KeyMapperApp).serviceAdapter
    }

    fun appShortcutAdapter(context: Context): AppShortcutAdapter {
        return (context.applicationContext as KeyMapperApp).appShortcutAdapter
    }

    fun notificationAdapter(context: Context): AndroidNotificationAdapter {
        return (context.applicationContext as KeyMapperApp).notificationAdapter
    }

    fun popupMessageAdapter(context: Context): PopupMessageAdapter {
        return (context.applicationContext as KeyMapperApp).popupMessageAdapter
    }

    fun vibratorAdapter(context: Context): VibratorAdapter {
        return (context.applicationContext as KeyMapperApp).vibratorAdapter
    }

    fun displayAdapter(context: Context): DisplayAdapter {
        return (context.applicationContext as KeyMapperApp).displayAdapter
    }

    fun audioAdapter(context: Context): VolumeAdapter {
        return (context.applicationContext as KeyMapperApp).audioAdapter
    }

    fun suAdapter(context: Context): SuAdapter {
        return (context.applicationContext as KeyMapperApp).suAdapter
    }

    fun intentAdapter(context: Context): IntentAdapter {
        return (context.applicationContext as KeyMapperApp).intentAdapter
    }

    fun phoneAdapter(context: Context): PhoneAdapter {
        return (context.applicationContext as KeyMapperApp).phoneAdapter
    }

    fun mediaAdapter(context: Context): MediaAdapter {
        return (context.applicationContext as KeyMapperApp).mediaAdapter
    }

    fun lockScreenAdapter(context: Context): LockScreenAdapter {
        return (context.applicationContext as KeyMapperApp).lockScreenAdapter
    }

    fun airplaneModeAdapter(context: Context): AirplaneModeAdapter {
        return (context.applicationContext as KeyMapperApp).airplaneModeAdapter
    }

    fun networkAdapter(context: Context): NetworkAdapter {
        return (context.applicationContext as KeyMapperApp).networkAdapter
    }

    fun nfcAdapter(context: Context): NfcAdapter {
        return (context.applicationContext as KeyMapperApp).nfcAdapter
    }

    fun openUrlAdapter(context: Context): OpenUrlAdapter {
        return (context.applicationContext as KeyMapperApp).openUrlAdapter
    }

    private fun createDatabase(context: Context): AppDatabase {
        val result = Room.databaseBuilder(
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
            AppDatabase.MIGRATION_10_11
        ).build()
        /* REMINDER!!!! Need to migrate fingerprint maps and other stuff???
         * Keep this note at the bottom */
        database = result
        return result
    }
}