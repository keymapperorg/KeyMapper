package io.github.sds100.keymapper.util

import android.content.Context
import com.example.architecturetest.data.KeymapRepository
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import com.example.architecturetest.data.viewmodel.NewKeymapViewModel
import io.github.sds100.keymapper.data.db.AppDatabase

/**
 * Created by sds100 on 26/01/2020.
 */
object InjectorUtils {
    private fun getKeymapRepository(context: Context): KeymapRepository {
        return KeymapRepository.getInstance(
                AppDatabase.getInstance(context.applicationContext).keymapDao()
        )
    }

    fun provideConfigKeymapViewModel(
            context: Context,
            keymapId: Long
    ): ConfigKeymapViewModel.Factory {
        val repository = getKeymapRepository(context)
        return ConfigKeymapViewModel.Factory(repository, keymapId)
    }

    fun provideNewKeymapViewModel(context: Context): NewKeymapViewModel.Factory {
        val repository = getKeymapRepository(context)
        return NewKeymapViewModel.Factory(repository)
    }
}