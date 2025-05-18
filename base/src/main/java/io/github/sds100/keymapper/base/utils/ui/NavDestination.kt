package io.github.sds100.keymapper.base.utils.ui

import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.pinchscreen.PinchPickCoordinateResult
import io.github.sds100.keymapper.base.actions.swipescreen.SwipePickCoordinateResult
import io.github.sds100.keymapper.base.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.base.system.apps.ChooseAppShortcutResult
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import io.github.sds100.keymapper.base.system.intents.ConfigIntentResult

sealed class NavDestination<R> {
    abstract val id: String

    companion object {
        const val ID_CHOOSE_APP = "choose_app"
        const val ID_CHOOSE_APP_SHORTCUT = "choose_app_shortcut"
        const val ID_KEY_CODE = "key_code"
        const val ID_KEY_EVENT = "key_event"
        const val ID_PICK_COORDINATE = "pick_coordinate"
        const val ID_PICK_SWIPE_COORDINATE = "pick_swipe_coordinate"
        const val ID_PICK_PINCH_COORDINATE = "pick_pinch_coordinate"
        const val ID_CONFIG_INTENT = "config_intent"
        const val ID_CHOOSE_ACTIVITY = "choose_activity"
        const val ID_CHOOSE_SOUND = "choose_sound"
        const val ID_CHOOSE_ACTION = "choose_action"
        const val ID_CHOOSE_CONSTRAINT = "choose_constraint"
        const val ID_CHOOSE_BLUETOOTH_DEVICE = "choose_bluetooth_device"
        const val ID_SETTINGS = "settings"
        const val ID_ABOUT = "about"
        const val ID_CONFIG_KEY_MAP = "config_key_map"
        const val ID_SHIZUKU_SETTINGS = "shizuku_settings"
        const val ID_CONFIG_FLOATING_BUTTON = "config_floating_button"
        const val ID_INTERACT_UI_ELEMENT_ACTION = "interact_ui_element_action"
    }

    data class ChooseApp(
        /**
         * Allow the list to show hidden apps that can't be launched.
         */
        val allowHiddenApps: Boolean,
    ) : NavDestination<String>() {
        override val id: String = ID_CHOOSE_APP
    }

    data object ChooseAppShortcut : NavDestination<ChooseAppShortcutResult>() {
        override val id: String = ID_CHOOSE_APP_SHORTCUT
    }

    data object ChooseKeyCode : NavDestination<Int>() {
        override val id: String = ID_KEY_CODE
    }

    data class ConfigKeyEventAction(val action: ActionData.InputKeyEvent? = null) : NavDestination<ActionData.InputKeyEvent>() {
        override val id: String = ID_KEY_EVENT
    }

    data class PickCoordinate(val result: PickCoordinateResult? = null) : NavDestination<PickCoordinateResult>() {
        override val id: String = ID_PICK_COORDINATE
    }

    data class PickSwipeCoordinate(val result: SwipePickCoordinateResult? = null) : NavDestination<SwipePickCoordinateResult>() {
        override val id: String = ID_PICK_SWIPE_COORDINATE
    }

    data class PickPinchCoordinate(val result: PinchPickCoordinateResult? = null) : NavDestination<PinchPickCoordinateResult>() {
        override val id: String = ID_PICK_PINCH_COORDINATE
    }

    data class ConfigIntent(val result: ConfigIntentResult? = null) : NavDestination<ConfigIntentResult>() {
        override val id: String = ID_CONFIG_INTENT
    }

    data object ChooseActivity : NavDestination<ActivityInfo>() {
        override val id: String = ID_CHOOSE_ACTIVITY
    }

    data object ChooseSound : NavDestination<ActionData.Sound>() {
        override val id: String = ID_CHOOSE_SOUND
    }

    data object ChooseAction : NavDestination<ActionData>() {
        override val id: String = ID_CHOOSE_ACTION
    }

    data object ChooseConstraint : NavDestination<Constraint>() {
        override val id: String = ID_CHOOSE_CONSTRAINT
    }

    data object ChooseBluetoothDevice : NavDestination<BluetoothDeviceInfo>() {
        override val id: String = ID_CHOOSE_BLUETOOTH_DEVICE
    }

    data object Settings : NavDestination<Unit>() {
        override val id: String = ID_SETTINGS
    }

    data object About : NavDestination<Unit>() {
        override val id: String = ID_ABOUT
    }

    sealed class ConfigKeyMap : NavDestination<Unit>() {
        override val id: String = ID_CONFIG_KEY_MAP
        abstract val showAdvancedTriggers: Boolean

        data class Open(val keyMapUid: String, override val showAdvancedTriggers: Boolean = false) : ConfigKeyMap()

        data class New(val groupUid: String?, override val showAdvancedTriggers: Boolean = false) : ConfigKeyMap()
    }

    data object ChooseFloatingLayout : NavDestination<Unit>() {
        override val id: String = "choose_floating_layout"
    }

    data object ShizukuSettings : NavDestination<Unit>() {
        override val id: String = ID_SHIZUKU_SETTINGS
    }

    data class ConfigFloatingButton(val buttonUid: String?) : NavDestination<Unit>() {
        override val id: String = ID_CONFIG_FLOATING_BUTTON
    }

    data class InteractUiElement(val action: ActionData.InteractUiElement?) : NavDestination<ActionData.InteractUiElement>() {
        override val id: String = ID_INTERACT_UI_ELEMENT_ACTION
    }
}
