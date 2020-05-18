package io.github.sds100.keymapper.util

import android.content.Context
import com.example.architecturetest.data.DefaultDeviceInfoRepository
import com.example.architecturetest.data.DefaultKeymapRepository
import io.github.sds100.keymapper.data.DefaultSystemActionRepository
import io.github.sds100.keymapper.data.FileRepository
import io.github.sds100.keymapper.data.SystemRepository
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.viewmodel.*
import io.github.sds100.keymapper.ui.callback.StringResourceProvider

/**
 * Created by sds100 on 26/01/2020.
 */
object InjectorUtils {
    fun getDefaultKeymapRepository(context: Context): DefaultKeymapRepository {
        return DefaultKeymapRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).keymapDao()
        )
    }

    private fun getDefaultSystemActionRepository(context: Context): DefaultSystemActionRepository {
        return DefaultSystemActionRepository.getInstance(context)
    }

    fun getDeviceInfoRepository(context: Context): DefaultDeviceInfoRepository {
        return DefaultDeviceInfoRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).deviceInfoDao()
        )
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
        val keymapRepository = getDefaultKeymapRepository(context)
        val deviceInfoRepository = getDeviceInfoRepository(context)

        return KeymapListViewModel.Factory(keymapRepository, deviceInfoRepository)
    }

    fun provideChooseConstraintListViewModel(): ChooseConstraintListViewModel.Factory {
        return ChooseConstraintListViewModel.Factory()
    }

    fun provideKeyActionTypeViewModel(): KeyActionTypeViewModel.Factory {
        return KeyActionTypeViewModel.Factory()
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

    fun provideSystemActionListViewModel(context: Context): SystemActionListViewModel.Factory {
        return SystemActionListViewModel.Factory(getDefaultSystemActionRepository(context))
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
        val keymapRepository = getDefaultKeymapRepository(context)
        val deviceInfoRepository = getDeviceInfoRepository(context)
        return ConfigKeymapViewModel.Factory(keymapRepository, deviceInfoRepository, id)
    }
}