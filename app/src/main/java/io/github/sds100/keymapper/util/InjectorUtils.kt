package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.MyApplication
import io.github.sds100.keymapper.data.repository.DefaultSystemActionRepository
import io.github.sds100.keymapper.data.repository.FileRepository
import io.github.sds100.keymapper.data.repository.SystemRepository
import io.github.sds100.keymapper.data.viewmodel.*

/**
 * Created by sds100 on 26/01/2020.
 */
object InjectorUtils {
    private fun getDefaultSystemActionRepository(context: Context): DefaultSystemActionRepository {
        return DefaultSystemActionRepository.getInstance(context)
    }

    private fun getSystemRepository(context: Context): SystemRepository {
        return SystemRepository.getInstance(context)
    }

    private fun getFileRepository(context: Context): FileRepository {
        return FileRepository.getInstance(context)
    }

    fun provideAppListViewModel(context: Context): AppListViewModel.Factory {
        val repository = getSystemRepository(context)
        return AppListViewModel.Factory(repository)
    }

    fun provideAppShortcutListViewModel(context: Context): AppShortcutListViewModel.Factory {
        val repository = getSystemRepository(context)
        return AppShortcutListViewModel.Factory(repository)
    }

    fun provideKeymapListViewModel(context: Context): KeymapListViewModel.Factory {
        val keymapRepository = (context.applicationContext as MyApplication).keymapRepository
        val deviceInfoRepository = (context.applicationContext as MyApplication).deviceInfoRepository

        return KeymapListViewModel.Factory(keymapRepository, deviceInfoRepository)
    }

    fun provideBackupRestoreViewModel(context: Context): BackupRestoreViewModel.Factory {
        val keymapRepository = (context.applicationContext as MyApplication).keymapRepository
        val deviceInfoRepository = (context.applicationContext as MyApplication).deviceInfoRepository

        return BackupRestoreViewModel.Factory(keymapRepository, deviceInfoRepository)
    }

    fun provideChooseConstraintListViewModel(): ChooseConstraintListViewModel.Factory {
        return ChooseConstraintListViewModel.Factory()
    }

    fun provideKeyActionTypeViewModel(): KeyActionTypeViewModel.Factory {
        return KeyActionTypeViewModel.Factory()
    }

    fun provideKeyEventActionTypeViewModel(): KeyEventActionTypeViewModel.Factory {
        return KeyEventActionTypeViewModel.Factory()
    }

    fun provideKeycodeListViewModel(): KeycodeListViewModel.Factory {
        return KeycodeListViewModel.Factory()
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
        return SystemActionListViewModel.Factory(getDefaultSystemActionRepository(context))
    }

    fun provideUnsupportedActionListViewModel(context: Context): UnsupportedActionListViewModel.Factory {
        return UnsupportedActionListViewModel.Factory(getDefaultSystemActionRepository(context))
    }

    fun provideActionBehaviorViewModel(): ActionBehaviorViewModel.Factory {
        return ActionBehaviorViewModel.Factory()
    }

    fun provideTriggerKeyBehaviorViewModel(): TriggerKeyBehaviorViewModel.Factory {
        return TriggerKeyBehaviorViewModel.Factory()
    }

    fun provideOnlineViewModel(context: Context,
                               fileUrl: String,
                               alternateUrl: String? = null,
                               header: String): OnlineFileViewModel.Factory {
        val repository = getFileRepository(context)
        return OnlineFileViewModel.Factory(repository, fileUrl, alternateUrl, header)
    }

    fun provideConfigKeymapViewModel(
        context: Context,
        id: Long
    ): ConfigKeymapViewModel.Factory {
        (context.applicationContext as MyApplication).apply {
            return ConfigKeymapViewModel.Factory(keymapRepository, deviceInfoRepository, onboardingState, id)
        }
    }

    fun provideCreateActionShortcutViewModel(): CreateActionShortcutViewModel.Factory {
        return CreateActionShortcutViewModel.Factory()
    }
}