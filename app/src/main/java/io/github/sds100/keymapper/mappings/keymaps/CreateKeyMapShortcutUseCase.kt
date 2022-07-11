package io.github.sds100.keymapper.mappings.keymaps

import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.os.bundleOf
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.api.Api
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.system.accessibility.MyAccessibilityService
import io.github.sds100.keymapper.util.Result
import javax.inject.Inject

/**
 * Created by sds100 on 23/03/2021.
 */

class CreateKeyMapShortcutUseCaseImpl @Inject constructor(
    private val adapter: AppShortcutAdapter,
    displayKeyMap: DisplayKeyMapUseCase,
    resourceProvider: ResourceProvider
) : CreateKeyMapShortcutUseCase, ResourceProvider by resourceProvider {
    private val actionUiHelper by lazy { KeyMapActionUiHelper(displayKeyMap, resourceProvider) }

    override val isSupported: Boolean
        get() = adapter.areLauncherShortcutsSupported

    override fun pinShortcutForSingleAction(keyMapUid: String, action: KeyMapAction): Result<*> {
        return adapter.pinShortcut(createShortcutForSingleAction(keyMapUid, action))
    }

    override fun pinShortcutForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String
    ): Result<*> {
        return adapter.pinShortcut(createShortcutForMultipleActions(keyMapUid, shortcutLabel))
    }

    override fun createIntentForSingleAction(
        keyMapUid: String,
        action: KeyMapAction
    ): Intent {
        return adapter.createShortcutResultIntent(createShortcutForSingleAction(keyMapUid, action))
    }

    override fun createIntentForMultipleActions(keyMapUid: String, shortcutLabel: String): Intent {
        return adapter.createShortcutResultIntent(createShortcutForMultipleActions(keyMapUid, shortcutLabel))
    }

    private fun createShortcutForSingleAction(
        keyMapUid: String,
        action: KeyMapAction
    ): ShortcutInfoCompat {
        val icon = actionUiHelper.getIcon(action.data)?.drawable
            ?: getDrawable(R.mipmap.ic_launcher_round)

        return adapter.createLauncherShortcut(
            icon = icon,
            label = actionUiHelper.getTitle(action.data, showDeviceDescriptors = false),
            intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
            bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid)
        )
    }

    private fun createShortcutForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String
    ): ShortcutInfoCompat {
        return adapter.createLauncherShortcut(
            icon = getDrawable(R.mipmap.ic_launcher_round),
            label = shortcutLabel,
            intentAction = Api.ACTION_TRIGGER_KEYMAP_BY_UID,
            bundleOf(Api.EXTRA_KEYMAP_UID to keyMapUid)
        )
    }
}

interface CreateKeyMapShortcutUseCase {
    val isSupported: Boolean

    fun pinShortcutForSingleAction(
        keyMapUid: String,
        action: KeyMapAction
    ): Result<*>

    fun pinShortcutForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String
    ): Result<*>

    fun createIntentForSingleAction(
        keyMapUid: String,
        action: KeyMapAction
    ): Intent

    fun createIntentForMultipleActions(
        keyMapUid: String,
        shortcutLabel: String
    ): Intent
}