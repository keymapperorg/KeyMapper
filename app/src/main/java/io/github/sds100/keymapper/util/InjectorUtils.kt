package io.github.sds100.keymapper.util

import android.content.Context
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.SystemRepository
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.viewmodel.*

/**
 * Created by sds100 on 26/01/2020.
 */
object InjectorUtils {
    private fun getKeymapRepository(context: Context): KeymapRepository {
        return KeymapRepository.getInstance(
            AppDatabase.getInstance(context.applicationContext).keymapDao()
        )
    }

    private fun getSystemRepository(context: Context): SystemRepository {
        return SystemRepository.getInstance(context)
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
        val repository = getKeymapRepository(context)
        return KeymapListViewModel.Factory(repository)
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

    fun provideConfigKeymapViewModel(
        context: Context,
        id: Long
    ): ConfigKeymapViewModel.Factory {
        val repository = getKeymapRepository(context)
        return ConfigKeymapViewModel.Factory(repository, id)
    }
}