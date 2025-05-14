package io.github.sds100.keymapper.keymaps

import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.core.os.bundleOf
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.api.Api
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 23/03/2021.
 */

class CreateKeyMapShortcutUseCaseImpl(
    private val adapter: AppShortcutAdapter,
    resourceProvider: ResourceProvider,
) : CreateKeyMapShortcutUseCase,
    ResourceProvider by resourceProvider {

    override val isSupported: Boolean
        get() = adapter.areLauncherShortcutsSupported

    override fun pinShortcut(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): Result<*> {
        val shortcut = if (icon == null) {
            adapter.createLauncherShortcut(
                iconResId = R.mipmap.ic_launcher_round,
                label = shortcutLabel,
                intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid),
            )
        } else {
            adapter.createLauncherShortcut(
                icon = icon,
                label = shortcutLabel,
                intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid),
            )
        }
        return adapter.pinShortcut(shortcut)
    }

    override fun createIntent(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): Intent {
        val shortcut = if (icon == null) {
            adapter.createLauncherShortcut(
                iconResId = R.mipmap.ic_launcher_round,
                label = shortcutLabel,
                intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid),
            )
        } else {
            adapter.createLauncherShortcut(
                icon = icon,
                label = shortcutLabel,
                intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid),
            )
        }
        return adapter.createShortcutResultIntent(shortcut)
    }
}

interface CreateKeyMapShortcutUseCase {
    val isSupported: Boolean

    fun pinShortcut(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): Result<*>

    fun createIntent(
        keyMapUid: String,
        shortcutLabel: String,
        icon: Drawable?,
    ): Intent
}
