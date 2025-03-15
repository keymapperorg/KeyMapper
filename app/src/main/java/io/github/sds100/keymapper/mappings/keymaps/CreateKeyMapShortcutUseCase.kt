package io.github.sds100.keymapper.mappings.keymaps

import android.content.Intent
import android.graphics.Color
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.os.bundleOf
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.api.Api
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.TintType

/**
 * Created by sds100 on 23/03/2021.
 */

class CreateKeyMapShortcutUseCaseImpl(
    private val adapter: AppShortcutAdapter,
    displayKeyMap: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider,
) : CreateKeyMapShortcutUseCase,
    ResourceProvider by resourceProvider {
    private val actionUiHelper by lazy { KeyMapActionUiHelperOld(displayKeyMap, resourceProvider) }

    override val isSupported: Boolean
        get() = adapter.areLauncherShortcutsSupported

    override fun pinShortcutForSingleAction(keyMapUid: String, action: Action): Result<*> {
        return adapter.pinShortcut(createShortcutForSingleAction(keyMapUid, action))
    }

    override fun pinShortcutForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String,
    ): Result<*> {
        return adapter.pinShortcut(createShortcutForMultipleActions(keyMapUid, shortcutLabel))
    }

    override fun createIntentForSingleAction(
        keyMapUid: String,
        action: Action,
    ): Intent {
        return adapter.createShortcutResultIntent(createShortcutForSingleAction(keyMapUid, action))
    }

    override fun createIntentForMultipleActions(keyMapUid: String, shortcutLabel: String): Intent {
        return adapter.createShortcutResultIntent(
            createShortcutForMultipleActions(
                keyMapUid,
                shortcutLabel,
            ),
        )
    }

    private fun createShortcutForSingleAction(
        keyMapUid: String,
        action: Action,
    ): ShortcutInfoCompat {
        val iconInfo = actionUiHelper.getIcon(action.data)

        // If the action doesn't have an icon then use the key mapper icon
        // for the launcher shortcut.
        if (iconInfo == null) {
            return adapter.createLauncherShortcut(
                iconResId = R.mipmap.ic_launcher_round,
                label = actionUiHelper.getTitle(action.data, showDeviceDescriptors = false),
                intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
                bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid),
            )
        }

        val icon = iconInfo.drawable

        when (iconInfo.tintType) {
            // Always set the icon as black if it needs to be on surface because the
            // background is white. Also, getting the colorOnSurface attribute
            // from the application context doesn't seem to work correctly.
            TintType.OnSurface -> icon.setTint(Color.BLACK)
            is TintType.Color -> icon.setTint(iconInfo.tintType.color)
            else -> {}
        }

        return adapter.createLauncherShortcut(
            icon = icon,
            label = actionUiHelper.getTitle(action.data, showDeviceDescriptors = false),
            intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
            bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid),
        )
    }

    private fun createShortcutForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String,
    ): ShortcutInfoCompat = adapter.createLauncherShortcut(
        iconResId = R.mipmap.ic_launcher_round,
        label = shortcutLabel,
        intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
        bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid),
    )
}

interface CreateKeyMapShortcutUseCase {
    val isSupported: Boolean

    fun pinShortcutForSingleAction(
        keyMapUid: String,
        action: Action,
    ): Result<*>

    fun pinShortcutForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String,
    ): Result<*>

    fun createIntentForSingleAction(
        keyMapUid: String,
        action: Action,
    ): Intent

    fun createIntentForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String,
    ): Intent
}
