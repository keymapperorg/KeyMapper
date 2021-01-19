package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.MyApplication
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.viewmodel.*
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel

/**
 * Created by sds100 on 26/01/2020.
 */
object InjectorUtils {

    fun provideAppListViewModel(context: Context): AppListViewModel.Factory {
        return AppListViewModel.Factory(ServiceLocator.packageRepository(context))
    }

    fun provideAppShortcutListViewModel(context: Context): AppShortcutListViewModel.Factory {
        return AppShortcutListViewModel.Factory(ServiceLocator.packageRepository(context))
    }

    fun provideKeymapListViewModel(context: Context): KeymapListViewModel.Factory {
        return KeymapListViewModel.Factory(
            ServiceLocator.keymapRepository(context),
            ServiceLocator.deviceInfoRepository(context)
        )
    }

    fun provideBackupRestoreViewModel(context: Context): BackupRestoreViewModel.Factory {
        return BackupRestoreViewModel.Factory(
            ServiceLocator.keymapRepository(context),
            ServiceLocator.deviceInfoRepository(context),
            ServiceLocator.fingerprintMapRepository(context)
        )
    }

    fun provideChooseConstraintListViewModel(): ChooseConstraintListViewModel.Factory {
        return ChooseConstraintListViewModel.Factory()
    }

    fun provideKeyActionTypeViewModel(): KeyActionTypeViewModel.Factory {
        return KeyActionTypeViewModel.Factory()
    }

    fun provideKeyEventActionTypeViewModel(context: Context
    ): KeyEventActionTypeViewModel.Factory {
        return KeyEventActionTypeViewModel.Factory(ServiceLocator.deviceInfoRepository(context))
    }

    fun provideKeycodeListViewModel(): KeycodeListViewModel.Factory {
        return KeycodeListViewModel.Factory()
    }

    fun provideIntentActionTypeViewModel(): IntentActionTypeViewModel.Factory {
        return IntentActionTypeViewModel.Factory()
    }

    fun provideTextBlockActionTypeViewModel(): TextBlockActionTypeViewModel.Factory {
        return TextBlockActionTypeViewModel.Factory()
    }

    fun provideUrlActionTypeViewModel(): UrlActionTypeViewModel.Factory {
        return UrlActionTypeViewModel.Factory()
    }

    fun provideTapCoordinateActionTypeViewModel(): TapCoordinateActionTypeViewModel.Factory {
        return TapCoordinateActionTypeViewModel.Factory()
    }

    fun provideSystemActionListViewModel(context: Context): SystemActionListViewModel.Factory {
        return SystemActionListViewModel.Factory(ServiceLocator.systemActionRepository(context))
    }

    fun provideUnsupportedActionListViewModel(context: Context
    ): UnsupportedActionListViewModel.Factory {
        return UnsupportedActionListViewModel.Factory(ServiceLocator.systemActionRepository(context))
    }

    fun provideKeymapActionOptionsViewModel(): KeymapActionOptionsViewModel.Factory {
        return KeymapActionOptionsViewModel.Factory()
    }

    fun provideFingerprintActionOptionsViewModel(): FingerprintActionOptionsViewModel.Factory {
        return FingerprintActionOptionsViewModel.Factory()
    }

    fun provideTriggerKeyOptionsViewModel(): TriggerKeyOptionsViewModel.Factory {
        return TriggerKeyOptionsViewModel.Factory()
    }

    fun provideOnlineViewModel(context: Context,
                               fileUrl: String,
                               alternateUrl: String? = null,
                               header: String): OnlineFileViewModel.Factory {
        return OnlineFileViewModel.Factory(
            ServiceLocator.fileRepository(context),
            fileUrl,
            alternateUrl,
            header
        )
    }

    fun provideFingerprintMapListViewModel(context: Context): FingerprintMapListViewModel.Factory {
        return FingerprintMapListViewModel.Factory(
            ServiceLocator.fingerprintMapRepository(context),
            ServiceLocator.deviceInfoRepository(context))
    }

    fun provideMenuFragmentViewModel(context: Context): MenuFragmentViewModel.Factory {
        return MenuFragmentViewModel.Factory(
            ServiceLocator.keymapRepository(context),
            ServiceLocator.fingerprintMapRepository(context))
    }

    fun provideConfigKeymapViewModel(context: Context
    ): ConfigKeymapViewModel.Factory {
        (context.applicationContext as MyApplication).apply {
            return ConfigKeymapViewModel.Factory(
                ServiceLocator.keymapRepository(context),
                ServiceLocator.deviceInfoRepository(context),
                ServiceLocator.preferenceDataStore(context)
            )
        }
    }

    fun provideConfigFingerprintMapViewModel(context: Context
    ): ConfigFingerprintMapViewModel.Factory {
        (context.applicationContext as MyApplication).apply {
            return ConfigFingerprintMapViewModel.Factory(
                ServiceLocator.fingerprintMapRepository(context),
                ServiceLocator.deviceInfoRepository(context),
                ServiceLocator.preferenceDataStore(context)
            )
        }
    }

    fun provideCreateActionShortcutViewModel(context: Context
    ): CreateActionShortcutViewModel.Factory {
        return CreateActionShortcutViewModel.Factory(ServiceLocator.deviceInfoRepository(context))
    }

    fun provideHomeViewModel(context: Context): HomeViewModel.Factory {
        return HomeViewModel.Factory(ServiceLocator.globalPreferences(context))
    }

    fun provideSettingsViewModel(context: Context): SettingsViewModel.Factory {
        return SettingsViewModel.Factory(ServiceLocator.preferenceDataStore(context))
    }
}