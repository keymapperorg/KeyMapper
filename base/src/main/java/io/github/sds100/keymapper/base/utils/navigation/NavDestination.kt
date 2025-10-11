package io.github.sds100.keymapper.base.utils.navigation

import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.pinchscreen.PinchPickCoordinateResult
import io.github.sds100.keymapper.base.actions.swipescreen.SwipePickCoordinateResult
import io.github.sds100.keymapper.base.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.system.apps.ChooseAppShortcutResult
import io.github.sds100.keymapper.base.system.intents.ConfigIntentResult
import io.github.sds100.keymapper.base.trigger.TriggerSetupShortcut
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.system.bluetooth.BluetoothDeviceInfo
import kotlinx.serialization.Serializable

@Serializable
abstract class NavDestination<R>(val isCompose: Boolean = false) {
    abstract val id: String

    companion object {
        const val ID_HOME = "home"
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
        const val ID_DEFAULT_OPTIONS_SETTINGS = "default_options_settings"
        const val ID_AUTOMATIC_CHANGE_IME_SETTINGS = "automatic_change_ime_settings"
        const val ID_ABOUT = "about"
        const val ID_CONFIG_KEY_MAP = "config_key_map"
        const val ID_INTERACT_UI_ELEMENT_ACTION = "interact_ui_element_action"
        const val ID_PRO_MODE = "pro_mode"
        const val ID_LOG = "log"
        const val ID_ADVANCED_TRIGGERS = "advanced_triggers"
    }

    @Serializable
    data object Home : NavDestination<Unit>() {
        override val id: String = ID_HOME
    }

    @Serializable
    data class ChooseApp(
        /**
         * Allow the list to show hidden apps that can't be launched.
         */
        val allowHiddenApps: Boolean,
    ) : NavDestination<String>() {
        override val id: String = ID_CHOOSE_APP
    }

    @Serializable
    data object ChooseAppShortcut : NavDestination<ChooseAppShortcutResult>() {
        override val id: String = ID_CHOOSE_APP_SHORTCUT
    }

    @Serializable
    data object ChooseKeyCode : NavDestination<Int>() {
        override val id: String = ID_KEY_CODE
    }

    @Serializable
    data class ConfigKeyEventAction(val action: ActionData.InputKeyEvent? = null) :
        NavDestination<ActionData.InputKeyEvent>() {
        override val id: String = ID_KEY_EVENT
    }

    @Serializable
    data class PickCoordinate(val result: PickCoordinateResult? = null) :
        NavDestination<PickCoordinateResult>() {
        override val id: String = ID_PICK_COORDINATE
    }

    @Serializable
    data class PickSwipeCoordinate(val result: SwipePickCoordinateResult? = null) :
        NavDestination<SwipePickCoordinateResult>() {
        override val id: String = ID_PICK_SWIPE_COORDINATE
    }

    @Serializable
    data class PickPinchCoordinate(val result: PinchPickCoordinateResult? = null) :
        NavDestination<PinchPickCoordinateResult>() {
        override val id: String = ID_PICK_PINCH_COORDINATE
    }

    @Serializable
    data class ConfigIntent(val result: ConfigIntentResult? = null) :
        NavDestination<ConfigIntentResult>() {
        override val id: String = ID_CONFIG_INTENT
    }

    @Serializable
    data object ChooseActivity : NavDestination<ActivityInfo>() {
        override val id: String = ID_CHOOSE_ACTIVITY
    }

    @Serializable
    data object ChooseSound : NavDestination<ActionData.Sound>() {
        override val id: String = ID_CHOOSE_SOUND
    }

    @Serializable
    data object ChooseAction : NavDestination<ActionData>(isCompose = true) {
        override val id: String = ID_CHOOSE_ACTION
    }

    @Serializable
    data object ChooseConstraint : NavDestination<ConstraintData>(isCompose = true) {
        override val id: String = ID_CHOOSE_CONSTRAINT
    }

    @Serializable
    data object ChooseBluetoothDevice : NavDestination<BluetoothDeviceInfo>() {
        override val id: String = ID_CHOOSE_BLUETOOTH_DEVICE
    }

    @Serializable
    data object Settings : NavDestination<Unit>(isCompose = true) {
        override val id: String = ID_SETTINGS
    }

    @Serializable
    data object DefaultOptionsSettings : NavDestination<Unit>(isCompose = true) {
        override val id: String = ID_DEFAULT_OPTIONS_SETTINGS
    }

    @Serializable
    data object AutomaticChangeImeSettings : NavDestination<Unit>(isCompose = true) {
        override val id: String = ID_AUTOMATIC_CHANGE_IME_SETTINGS
    }

    @Serializable
    data object About : NavDestination<Unit>() {
        override val id: String = ID_ABOUT
    }

    @Serializable
    data class OpenKeyMap(val keyMapUid: String) :
        NavDestination<Unit>(isCompose = true) {
        override val id: String = ID_CONFIG_KEY_MAP
    }

    @Serializable
    data class NewKeyMap(
        val groupUid: String?,
        val floatingButtonToUse: String? = null,
        /**
         * The trigger shortcut to immediately launch
         * when navigating to the screen to create a key map.
         */
        val triggerSetupShortcut: TriggerSetupShortcut? = null,
    ) : NavDestination<Unit>(isCompose = true) {
        override val id: String = ID_CONFIG_KEY_MAP
    }

    @Serializable
    data class InteractUiElement(val actionJson: String?) :
        NavDestination<ActionData.InteractUiElement>(isCompose = true) {
        override val id: String = ID_INTERACT_UI_ELEMENT_ACTION
    }

    @Serializable
    data object ProMode : NavDestination<Unit>(isCompose = true) {
        override val id: String = ID_PRO_MODE
    }

    @Serializable
    data object ProModeSetup : NavDestination<Unit>(isCompose = true) {
        const val ID_PRO_MODE_SETUP = "pro_mode_setup_wizard"
        override val id: String = ID_PRO_MODE_SETUP
    }

    @Serializable
    data object Log : NavDestination<Unit>(isCompose = true) {
        override val id: String = ID_LOG
    }

    /**
     * This returns a trigger setup shortcut if an advanced trigger is used.
     */
    @Serializable
    data object AdvancedTriggers : NavDestination<TriggerSetupShortcut?>(isCompose = true) {
        override val id: String = ID_ADVANCED_TRIGGERS
    }
}
