package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayAppShortcutsUseCaseImpl @Inject constructor(
    private val appShortcutAdapter: AppShortcutAdapter,
) : DisplayAppShortcutsUseCase {
    override val shortcuts: Flow<State<List<AppShortcutInfo>>> = appShortcutAdapter.installedAppShortcuts

    override fun getShortcutName(appShortcutInfo: AppShortcutInfo): Result<String> {
        return appShortcutAdapter.getShortcutName(appShortcutInfo)
    }

    override fun getShortcutIcon(appShortcutInfo: AppShortcutInfo): Result<Drawable> {
        return appShortcutAdapter.getShortcutIcon(appShortcutInfo)
    }
}

interface DisplayAppShortcutsUseCase {
    val shortcuts: Flow<State<List<AppShortcutInfo>>>

    fun getShortcutName(appShortcutInfo: AppShortcutInfo): Result<String>
    fun getShortcutIcon(appShortcutInfo: AppShortcutInfo): Result<Drawable>
}