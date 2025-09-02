package io.github.sds100.keymapper.base.keymaps

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.os.bundleOf
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import javax.inject.Inject

class CreateKeyMapShortcutUseCaseImpl @Inject constructor(
    private val appShortcutAdapter: AppShortcutAdapter,
    private val resourceProvider: ResourceProvider,
) : CreateKeyMapShortcutUseCase {

    companion object {
        private const val ACTION_TRIGGER_KEYMAP_BY_UID =
            "io.github.sds100.keymapper.ACTION_TRIGGER_KEYMAP_BY_UID"
        private const val EXTRA_KEYMAP_UID = "io.github.sds100.keymapper.EXTRA_KEYMAP_UID"
    }

    override val isSupported: Boolean
        get() = appShortcutAdapter.areLauncherShortcutsSupported

    override fun pinShortcut(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): KMResult<*> {
        val shortcut = if (icon == null) {
            appShortcutAdapter.createLauncherShortcut(
                iconResId = R.mipmap.ic_launcher_round,
                label = shortcutLabel,
                intentAction = ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(EXTRA_KEYMAP_UID to keyMapUid),
            )
        } else {
            appShortcutAdapter.createLauncherShortcut(
                icon = icon,
                label = shortcutLabel,
                intentAction = ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(EXTRA_KEYMAP_UID to keyMapUid),
            )
        }
        return appShortcutAdapter.pinShortcut(shortcut)
    }

    override fun createIntent(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): Intent {
        val shortcut = if (icon == null) {
            appShortcutAdapter.createLauncherShortcut(
                iconResId = R.mipmap.ic_launcher_round,
                label = shortcutLabel,
                intentAction = ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(EXTRA_KEYMAP_UID to keyMapUid),
            )
        } else {
            appShortcutAdapter.createLauncherShortcut(
                icon = icon,
                label = shortcutLabel,
                intentAction = ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(EXTRA_KEYMAP_UID to keyMapUid),
            )
        }
        return appShortcutAdapter.createShortcutResultIntent(shortcut)
    }
}

interface CreateKeyMapShortcutUseCase {
    val isSupported: Boolean

    fun pinShortcut(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): KMResult<*>

    fun createIntent(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): Intent
}
