package io.github.sds100.keymapper.util.ui

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.pinchscreen.PinchPickCoordinateResult
import io.github.sds100.keymapper.actions.sound.ChooseSoundFileResult
import io.github.sds100.keymapper.actions.swipescreen.SwipePickCoordinateResult
import io.github.sds100.keymapper.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.actions.uielementinteraction.InteractWithScreenElementResult
import io.github.sds100.keymapper.constraints.ChooseConstraintType
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.mappings.fingerprintmaps.FingerprintMapId
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.system.apps.ChooseAppShortcutResult
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.system.intents.ConfigIntentResult
import io.github.sds100.keymapper.system.ui.UiElementInfo
import timber.log.Timber

/**
 * Created by sds100 on 25/07/2021.
 */
sealed class NavDestination<R> {
    companion object {
        const val ID_CHOOSE_APP = "choose_app"
        const val ID_CHOOSE_APP_SHORTCUT = "choose_app_shortcut"
        const val ID_KEY_CODE = "key_code"
        const val ID_KEY_EVENT = "key_event"
        const val ID_PICK_COORDINATE = "pick_coordinate"
        const val ID_PICK_SWIPE_COORDINATE = "pick_swipe_coordinate"
        const val ID_PICK_PINCH_COORDINATE = "pick_pinch_coordinate"
        const val ID_INTERACT_WITH_SCREEN_ELEMENT = "interact_with_screen_element"
        const val ID_CHOOSE_UI_ELEMENT = "choose_ui_element"
        const val ID_CONFIG_INTENT = "config_intent"
        const val ID_CHOOSE_ACTIVITY = "choose_activity"
        const val ID_CHOOSE_SOUND = "choose_sound"
        const val ID_CHOOSE_ACTION = "choose_action"
        const val ID_CHOOSE_CONSTRAINT = "choose_constraint"
        const val ID_CHOOSE_BLUETOOTH_DEVICE = "choose_bluetooth_device"
        const val ID_REPORT_BUG = "report_bug"
        const val ID_FIX_APP_KILLING = "fix_app_killing"
        const val ID_SETTINGS = "settings"
        const val ID_ABOUT = "about"
        const val ID_CONFIG_KEY_MAP = "config_key_map"
        const val ID_CONFIG_FINGERPRINT_MAP = "config_fingerprint_map"

        fun NavDestination<*>.getId(): String {
            Timber.d("NavDestination: %s", this.toString())
            return when (this) {
                is ChooseApp -> ID_CHOOSE_APP
                is ChooseAppShortcut -> ID_CHOOSE_APP_SHORTCUT
                is ChooseKeyCode -> ID_KEY_CODE
                is ConfigKeyEventAction -> ID_KEY_EVENT
                is PickCoordinate -> ID_PICK_COORDINATE
                is PickSwipeCoordinate -> ID_PICK_SWIPE_COORDINATE
                is PickPinchCoordinate -> ID_PICK_PINCH_COORDINATE
                is InteractWithScreenElement -> ID_INTERACT_WITH_SCREEN_ELEMENT
                is ChooseUiElement -> ID_CHOOSE_UI_ELEMENT
                is ConfigIntent -> ID_CONFIG_INTENT
                is ChooseActivity -> ID_CHOOSE_ACTIVITY
                is ChooseSound -> ID_CHOOSE_SOUND
                is ChooseAction -> ID_CHOOSE_ACTION
                is ChooseConstraint -> ID_CHOOSE_CONSTRAINT
                is ChooseBluetoothDevice -> ID_CHOOSE_BLUETOOTH_DEVICE
                is FixAppKilling -> ID_FIX_APP_KILLING
                is ReportBug -> ID_REPORT_BUG
                is Settings -> ID_SETTINGS
                is About -> ID_ABOUT
                is ConfigKeyMap -> ID_CONFIG_KEY_MAP
                is ConfigFingerprintMap -> ID_CONFIG_FINGERPRINT_MAP
            }
        }
    }

    data class ChooseApp(
        /**
         * Allow the list to show hidden apps that can't be launched.
         */
        val allowHiddenApps: Boolean
    ) : NavDestination<String>()

    object ChooseAppShortcut : NavDestination<ChooseAppShortcutResult>()
    object ChooseKeyCode : NavDestination<Int>()
    data class ConfigKeyEventAction(val action: ActionData.InputKeyEvent? = null) :
        NavDestination<ActionData.InputKeyEvent>()

    data class PickCoordinate(val result: PickCoordinateResult? = null) :
        NavDestination<PickCoordinateResult>()

    data class PickSwipeCoordinate(val result: SwipePickCoordinateResult? = null) :
        NavDestination<SwipePickCoordinateResult>()

    data class PickPinchCoordinate(val result: PinchPickCoordinateResult? = null) :
        NavDestination<PinchPickCoordinateResult>()

    data class InteractWithScreenElement(val result: InteractWithScreenElementResult? = null) :
        NavDestination<InteractWithScreenElementResult>()

    object ChooseUiElement: NavDestination<UiElementInfo>()

    data class ConfigIntent(val result: ConfigIntentResult? = null) :
        NavDestination<ConfigIntentResult>()

    object ChooseActivity : NavDestination<ActivityInfo>()
    object ChooseSound : NavDestination<ChooseSoundFileResult>()
    object ChooseAction : NavDestination<ActionData>()
    data class ChooseConstraint(val supportedConstraints: List<ChooseConstraintType>) :
        NavDestination<Constraint>()

    object ChooseBluetoothDevice : NavDestination<BluetoothDeviceInfo>()
    object ReportBug : NavDestination<Unit>()
    object FixAppKilling : NavDestination<Unit>()
    object Settings : NavDestination<Unit>()
    object About : NavDestination<Unit>()
    data class ConfigKeyMap(val keyMapUid: String?) : NavDestination<Unit>()
    data class ConfigFingerprintMap(val id: FingerprintMapId) : NavDestination<Unit>()
}