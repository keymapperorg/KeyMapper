package io.github.sds100.keymapper.base.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.system.apps.AppShortcutInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DisplayAppShortcutsUseCaseImpl @Inject constructor(
    private val appShortcutAdapter: AppShortcutAdapter,
) : DisplayAppShortcutsUseCase {
    override val shortcuts: Flow<State<List<AppShortcutInfo>>> =
        appShortcutAdapter.installedAppShortcuts

    override fun getShortcutName(appShortcutInfo: AppShortcutInfo): KMResult<String> =
        appShortcutAdapter.getShortcutName(appShortcutInfo)

    override fun getShortcutIcon(appShortcutInfo: AppShortcutInfo): KMResult<Drawable> =
        appShortcutAdapter.getShortcutIcon(appShortcutInfo)
}

interface DisplayAppShortcutsUseCase {
    val shortcuts: Flow<State<List<AppShortcutInfo>>>

    fun getShortcutName(appShortcutInfo: AppShortcutInfo): KMResult<String>
    fun getShortcutIcon(appShortcutInfo: AppShortcutInfo): KMResult<Drawable>
}
