package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.state.State
import kotlinx.coroutines.flow.Flow

class DisplayAppShortcutsUseCaseImpl(
    private val appShortcutAdapter: AppShortcutAdapter,
) : DisplayAppShortcutsUseCase {
    override val shortcuts: Flow<State<List<AppShortcutInfo>>> =
        appShortcutAdapter.installedAppShortcuts

    override fun getShortcutName(appShortcutInfo: AppShortcutInfo): Result<String> =
        appShortcutAdapter.getShortcutName(appShortcutInfo)

    override fun getShortcutIcon(appShortcutInfo: AppShortcutInfo): Result<Drawable> =
        appShortcutAdapter.getShortcutIcon(appShortcutInfo)
}

interface DisplayAppShortcutsUseCase {
    val shortcuts: Flow<State<List<AppShortcutInfo>>>

    fun getShortcutName(appShortcutInfo: AppShortcutInfo): Result<String>
    fun getShortcutIcon(appShortcutInfo: AppShortcutInfo): Result<Drawable>
}
