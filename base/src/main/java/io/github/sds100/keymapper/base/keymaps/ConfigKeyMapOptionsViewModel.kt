package io.github.sds100.keymapper.base.keymaps

import android.graphics.Color
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ActionUiHelper
import io.github.sds100.keymapper.base.shortcuts.CreateKeyMapShortcutUseCase
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.trigger.EvdevTriggerKey
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.TintType
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigKeyMapOptionsViewModel(
    private val coroutineScope: CoroutineScope,
    private val config: ConfigTriggerUseCase,
    private val displayUseCase: DisplayKeyMapUseCase,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider,
    KeyMapOptionsCallback {

    private val actionUiHelper = ActionUiHelper(displayUseCase, resourceProvider)

    val state: StateFlow<State<KeyMapOptionsState>> = combine(
        config.keyMap,
        systemBridgeConnectionManager.connectionState,
    ) { keyMapState, systemBridgeConnectionState ->
        keyMapState.mapData { keyMap -> buildState(keyMap, systemBridgeConnectionState) }
    }.stateIn(coroutineScope, SharingStarted.Eagerly, State.Loading)

    override fun onLongPressDelayChanged(delay: Int) {
        config.setLongPressDelay(delay)
    }

    override fun onDoublePressDelayChanged(delay: Int) {
        config.setDoublePressDelay(delay)
    }

    override fun onSequenceTriggerTimeoutChanged(timeout: Int) {
        config.setSequenceTriggerTimeout(timeout)
    }

    override fun onVibrateDurationChanged(duration: Int) {
        config.setVibrationDuration(duration)
    }

    override fun onVibrateChanged(checked: Boolean) {
        config.setVibrateEnabled(checked)
    }

    override fun onLongPressDoubleVibrationChanged(checked: Boolean) {
        config.setLongPressDoubleVibrationEnabled(checked)
    }

    override fun onShowToastChanged(checked: Boolean) {
        config.setShowToastEnabled(checked)
    }

    override fun onTriggerFromOtherAppsChanged(checked: Boolean) {
        config.setTriggerFromOtherAppsEnabled(checked)
    }

    override fun onOpenExpertModeSettings() {
        coroutineScope.launch {
            navigate("screen_off_trigger_tip", NavDestination.ExpertMode)
        }
    }

    override fun onCreateShortcutClick() {
        coroutineScope.launch {
            val mapping = config.keyMap.firstOrNull()?.dataOrNull() ?: return@launch
            val keyMapUid = mapping.uid

            val key = "create_launcher_shortcut"
            val defaultShortcutName: String
            val icon: Drawable?

            if (mapping.actionList.size == 1) {
                val action = mapping.actionList.first().data
                defaultShortcutName = actionUiHelper.getTitle(
                    action,
                    showDeviceDescriptors = false,
                )

                val iconInfo = actionUiHelper.getDrawableIcon(action)

                if (iconInfo == null) {
                    icon = null
                } else {
                    when (iconInfo.tintType) {
                        // Always set the icon as black if it needs to be on surface because the
                        // background is white. Also, getting the colorOnSurface attribute
                        // from the application context doesn't seem to work correctly.
                        TintType.OnSurface -> iconInfo.drawable.setTint(Color.BLACK)

                        is TintType.Color -> iconInfo.drawable.setTint(iconInfo.tintType.color)

                        else -> {}
                    }

                    icon = iconInfo.drawable
                }
            } else {
                defaultShortcutName = ""
                icon = null
            }

            val shortcutName = showDialog(
                key,
                DialogModel.Text(
                    getString(R.string.hint_shortcut_name),
                    allowEmpty = false,
                    text = defaultShortcutName,
                ),
            ) ?: return@launch

            val result = createKeyMapShortcut.pinShortcut(keyMapUid, shortcutName, icon)

            result.onFailure { error ->
                val snackBar = DialogModel.SnackBar(
                    message = error.getFullMessage(this@ConfigKeyMapOptionsViewModel),
                )

                showDialog("create_shortcut_result", snackBar)
            }
        }
    }

    private suspend fun buildState(
        keyMap: KeyMap,
        systemBridgeConnectionState: SystemBridgeConnectionState,
    ): KeyMapOptionsState {
        val defaultLongPressDelay = config.defaultLongPressDelay.first()
        val defaultDoublePressDelay = config.defaultDoublePressDelay.first()
        val defaultSequenceTriggerTimeout = config.defaultSequenceTriggerTimeout.first()
        val defaultVibrateDuration = config.defaultVibrateDuration.first()

        return KeyMapOptionsState(
            showLongPressDelay = keyMap.trigger.isChangingLongPressDelayAllowed(),
            longPressDelay = keyMap.trigger.longPressDelay ?: defaultLongPressDelay,
            defaultLongPressDelay = defaultLongPressDelay,

            showDoublePressDelay = keyMap.trigger.isChangingDoublePressDelayAllowed(),
            doublePressDelay = keyMap.trigger.doublePressDelay ?: defaultDoublePressDelay,
            defaultDoublePressDelay = defaultDoublePressDelay,

            showSequenceTriggerTimeout = keyMap.trigger.isChangingSequenceTriggerTimeoutAllowed(),
            sequenceTriggerTimeout = keyMap.trigger.sequenceTriggerTimeout
                ?: defaultSequenceTriggerTimeout,
            defaultSequenceTriggerTimeout = defaultSequenceTriggerTimeout,

            showVibrateDuration = keyMap.trigger.isChangingVibrationDurationAllowed(),
            vibrateDuration = keyMap.trigger.vibrateDuration ?: defaultVibrateDuration,
            defaultVibrateDuration = defaultVibrateDuration,

            showVibrate = keyMap.trigger.isVibrateAllowed(),
            vibrate = keyMap.trigger.vibrate,

            showLongPressDoubleVibration = keyMap.trigger.isLongPressDoubleVibrationAllowed(),
            longPressDoubleVibration = keyMap.trigger.longPressDoubleVibration,

            triggerFromOtherApps = keyMap.trigger.triggerFromOtherApps,
            keyMapUid = keyMap.uid,
            isLauncherShortcutButtonEnabled = createKeyMapShortcut.isSupported,

            showToast = keyMap.trigger.showToast,
            showScreenOffTip = keyMap.trigger.keys.none { it is EvdevTriggerKey },
            isExpertModeStarted =
            systemBridgeConnectionState is SystemBridgeConnectionState.Connected,
        )
    }
}

data class KeyMapOptionsState(
    val showLongPressDelay: Boolean,
    val longPressDelay: Int,
    val defaultLongPressDelay: Int,

    val showDoublePressDelay: Boolean,
    val doublePressDelay: Int,
    val defaultDoublePressDelay: Int,

    val showSequenceTriggerTimeout: Boolean,
    val sequenceTriggerTimeout: Int,
    val defaultSequenceTriggerTimeout: Int,

    val showVibrateDuration: Boolean,
    val vibrateDuration: Int,
    val defaultVibrateDuration: Int,

    val showVibrate: Boolean,
    val vibrate: Boolean,

    val showLongPressDoubleVibration: Boolean,
    val longPressDoubleVibration: Boolean,

    val triggerFromOtherApps: Boolean,
    val keyMapUid: String,
    val isLauncherShortcutButtonEnabled: Boolean,

    val showToast: Boolean,

    val showScreenOffTip: Boolean,
    val isExpertModeStarted: Boolean,
)
