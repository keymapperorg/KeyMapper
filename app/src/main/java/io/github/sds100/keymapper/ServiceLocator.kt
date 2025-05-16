package io.github.sds100.keymapper

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.actions.sound.SoundsManagerImpl
import io.github.sds100.keymapper.backup.BackupManager
import io.github.sds100.keymapper.backup.BackupManagerImpl
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepositoryImpl
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.FloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.repositories.RoomFloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.RoomFloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.RoomGroupRepository
import io.github.sds100.keymapper.data.repositories.RoomKeyMapRepository
import io.github.sds100.keymapper.data.repositories.RoomLogRepository
import io.github.sds100.keymapper.data.repositories.SettingsPreferenceRepository
import io.github.sds100.keymapper.keymaps.ConfigKeyMapUseCaseController
import io.github.sds100.keymapper.logging.LogRepository
import io.github.sds100.keymapper.purchasing.PurchasingManagerImpl
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.airplanemode.AirplaneModeAdapter
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.bluetooth.BluetoothAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.AndroidDisplayAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.intents.IntentAdapter
import io.github.sds100.keymapper.system.leanback.LeanbackAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.AndroidMediaAdapter
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.nfc.NfcAdapter
import io.github.sds100.keymapper.system.notifications.AndroidNotificationAdapter
import io.github.sds100.keymapper.system.notifications.NotificationController
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapter
import io.github.sds100.keymapper.system.permissions.AndroidPermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.system.power.PowerAdapter
import io.github.sds100.keymapper.system.ringtones.RingtoneAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.url.OpenUrlAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.system.volume.VolumeAdapter
import io.github.sds100.keymapper.util.ui.ResourceProviderImpl
import kotlinx.coroutines.CoroutineScope


object ServiceLocator {

    private var database: AppDatabase? = null

    private fun database(context: Context): AppDatabase {
        synchronized(this) {
            return database ?: createDatabase(context.applicationContext).also {
                this.database = it
            }
        }
    }

    @Volatile
    private var roomKeymapRepository: RoomKeyMapRepository? = null

    fun roomKeyMapRepository(context: Context): RoomKeyMapRepository {
        synchronized(this) {
            return roomKeymapRepository ?: RoomKeyMapRepository(
                database(context).keyMapDao(),
                database(context).fingerprintMapDao(),
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
            ).also {
                this.roomKeymapRepository = it
            }
        }
    }

    @Volatile
    private var settingsRepository: PreferenceRepository? = null

    fun settingsRepository(context: Context): PreferenceRepository {
        synchronized(this) {
            return settingsRepository ?: SettingsPreferenceRepository(
                context.applicationContext,
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
            ).also {
                this.settingsRepository = it
            }
        }
    }

    @Volatile
    private var logRepository: LogRepository? = null

    fun logRepository(context: Context): LogRepository {
        synchronized(this) {
            return logRepository ?: RoomLogRepository(
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
                database(context).logEntryDao(),
            ).also {
                this.logRepository = it
            }
        }
    }

    @Volatile
    private var floatingLayoutRepository: FloatingLayoutRepository? = null

    fun floatingLayoutRepository(context: Context): FloatingLayoutRepository {
        synchronized(this) {
            return floatingLayoutRepository ?: RoomFloatingLayoutRepository(
                database(context).floatingLayoutDao(),
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
            ).also {
                this.floatingLayoutRepository = it
            }
        }
    }

    @Volatile
    private var floatingButtonRepository: FloatingButtonRepository? = null

    fun floatingButtonRepository(context: Context): FloatingButtonRepository {
        synchronized(this) {
            return floatingButtonRepository ?: RoomFloatingButtonRepository(
                database(context).floatingButtonDao(),
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
            ).also {
                this.floatingButtonRepository = it
            }
        }
    }

    @Volatile
    private var groupRepository: GroupRepository? = null

    fun groupRepository(context: Context): GroupRepository {
        synchronized(this) {
            return groupRepository ?: RoomGroupRepository(
                database(context).groupDao(),
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
            ).also {
                this.groupRepository = it
            }
        }
    }

    @Volatile
    private var backupManager: BackupManager? = null

    fun backupManager(context: Context): BackupManager {
        synchronized(this) {
            return backupManager ?: createBackupManager(context).also {
                this.backupManager = it
            }
        }
    }

    private fun createBackupManager(context: Context): BackupManager = backupManager ?: BackupManagerImpl(
        (context.applicationContext as KeyMapperApp).appCoroutineScope,
        fileAdapter(context),
        roomKeyMapRepository(context),
        settingsRepository(context),
        floatingLayoutRepository(context),
        floatingButtonRepository(context),
        groupRepository(context),
        soundsManager(context),
    )

    @Volatile
    private var soundsManager: SoundsManager? = null

    fun soundsManager(context: Context): SoundsManager {
        synchronized(this) {
            return soundsManager ?: SoundsManagerImpl(
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
                fileAdapter(context),
            ).also {
                this.soundsManager = it
            }
        }
    }

    @Volatile
    private var configKeyMapsController: ConfigKeyMapUseCaseController? = null

    fun configKeyMapsController(ctx: Context): ConfigKeyMapUseCaseController {
        synchronized(this) {
            return configKeyMapsController
                ?: createConfigKeyMapsController(ctx).also {
                    configKeyMapsController = it
                }
        }
    }

    private fun createConfigKeyMapsController(ctx: Context): ConfigKeyMapUseCaseController {
        return ConfigKeyMapUseCaseController(
            appCoroutineScope(ctx),
            roomKeyMapRepository(ctx),
            devicesAdapter(ctx),
            settingsRepository(ctx),
            floatingLayoutRepository(ctx),
            floatingButtonRepository(ctx),
            accessibilityServiceAdapter(ctx),
        )
    }

    @Volatile
    private var accessibilityNodeRepository: AccessibilityNodeRepository? = null

    fun accessibilityNodeRepository(context: Context): AccessibilityNodeRepository {
        synchronized(this) {
            return accessibilityNodeRepository ?: AccessibilityNodeRepositoryImpl(
                (context.applicationContext as KeyMapperApp).appCoroutineScope,
                database(context).accessibilityNodeDao(),
            ).also {
                this.accessibilityNodeRepository = it
            }
        }
    }

    fun fileAdapter(context: Context): FileAdapter = (context.applicationContext as KeyMapperApp).fileAdapter

    fun inputMethodAdapter(context: Context): InputMethodAdapter = (context.applicationContext as KeyMapperApp).inputMethodAdapter

    fun devicesAdapter(context: Context): DevicesAdapter = (context.applicationContext as KeyMapperApp).devicesAdapter

    fun bluetoothAdapter(context: Context): BluetoothAdapter = (context.applicationContext as KeyMapperApp).bluetoothMonitor

    fun notificationController(context: Context): NotificationController = (context.applicationContext as KeyMapperApp).notificationController

    fun resourceProvider(context: Context): ResourceProviderImpl = (context.applicationContext as KeyMapperApp).resourceProvider

    fun packageManagerAdapter(context: Context): PackageManagerAdapter = (context.applicationContext as KeyMapperApp).packageManagerAdapter

    fun cameraAdapter(context: Context): CameraAdapter = (context.applicationContext as KeyMapperApp).cameraAdapter

    fun permissionAdapter(context: Context): AndroidPermissionAdapter = (context.applicationContext as KeyMapperApp).permissionAdapter

    fun systemFeatureAdapter(context: Context): SystemFeatureAdapter = (context.applicationContext as KeyMapperApp).systemFeatureAdapter

    fun accessibilityServiceAdapter(context: Context): AccessibilityServiceAdapter = (context.applicationContext as KeyMapperApp).accessibilityServiceAdapter

    fun notificationReceiverAdapter(context: Context): NotificationReceiverAdapter = (context.applicationContext as KeyMapperApp).notificationReceiverAdapter

    fun appShortcutAdapter(context: Context): AppShortcutAdapter = (context.applicationContext as KeyMapperApp).appShortcutAdapter

    fun notificationAdapter(context: Context): AndroidNotificationAdapter = (context.applicationContext as KeyMapperApp).notificationAdapter

    fun popupMessageAdapter(context: Context): PopupMessageAdapter = (context.applicationContext as KeyMapperApp).popupMessageAdapter

    fun vibratorAdapter(context: Context): VibratorAdapter = (context.applicationContext as KeyMapperApp).vibratorAdapter

    fun displayAdapter(context: Context): AndroidDisplayAdapter = (context.applicationContext as KeyMapperApp).displayAdapter

    fun audioAdapter(context: Context): VolumeAdapter = (context.applicationContext as KeyMapperApp).audioAdapter

    fun suAdapter(context: Context): SuAdapter = (context.applicationContext as KeyMapperApp).suAdapter

    fun intentAdapter(context: Context): IntentAdapter = (context.applicationContext as KeyMapperApp).intentAdapter

    fun phoneAdapter(context: Context): PhoneAdapter = (context.applicationContext as KeyMapperApp).phoneAdapter

    fun mediaAdapter(context: Context): AndroidMediaAdapter = (context.applicationContext as KeyMapperApp).mediaAdapter

    fun lockScreenAdapter(context: Context): LockScreenAdapter = (context.applicationContext as KeyMapperApp).lockScreenAdapter

    fun airplaneModeAdapter(context: Context): AirplaneModeAdapter = (context.applicationContext as KeyMapperApp).airplaneModeAdapter

    fun networkAdapter(context: Context): NetworkAdapter = (context.applicationContext as KeyMapperApp).networkAdapter

    fun nfcAdapter(context: Context): NfcAdapter = (context.applicationContext as KeyMapperApp).nfcAdapter

    fun openUrlAdapter(context: Context): OpenUrlAdapter = (context.applicationContext as KeyMapperApp).openUrlAdapter

    fun clipboardAdapter(context: Context): ClipboardAdapter = (context.applicationContext as KeyMapperApp).clipboardAdapter

    fun shizukuAdapter(context: Context): ShizukuAdapter = (context.applicationContext as KeyMapperApp).shizukuAdapter

    fun leanbackAdapter(context: Context): LeanbackAdapter = (context.applicationContext as KeyMapperApp).leanbackAdapter

    fun powerAdapter(context: Context): PowerAdapter = (context.applicationContext as KeyMapperApp).powerAdapter

    fun appCoroutineScope(context: Context): CoroutineScope = (context.applicationContext as KeyMapperApp).appCoroutineScope

    fun purchasingManager(context: Context): PurchasingManagerImpl = (context.applicationContext as KeyMapperApp).purchasingManager

    fun ringtoneAdapter(context: Context): RingtoneAdapter = (context.applicationContext as KeyMapperApp).ringtoneManagerAdapter

    private fun createDatabase(context: Context): AppDatabase = Room.databaseBuilder(
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
