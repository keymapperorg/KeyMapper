package io.github.sds100.keymapper.base.system.accessibility

import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintData
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.detection.DetectKeyMapModel
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerDevice
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.common.utils.NodeInteractionType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceFeatureUtilsTest {

    @Test
    fun `When there are no key maps then request nothing`() {
        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(emptyList())

        assertThat(features, `is`(emptySet()))
    }

    @Test
    fun `When a key map is disabled then ignore it`() {
        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(
                model(
                    KeyMap(
                        isEnabled = false,
                        trigger = Trigger(keys = listOf(keyEventKey())),
                    ),
                ),
            ),
        )

        assertThat(features, `is`(emptySet()))
    }

    @Test
    fun `When a key map has a key event trigger then filter key events`() {
        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(model(KeyMap(trigger = Trigger(keys = listOf(keyEventKey()))))),
        )

        assertThat(features, hasItem(AccessibilityServiceFeature.FILTER_KEY_EVENTS))
    }

    @Test
    fun `When a key event trigger requires an input method then do not filter key events`() {
        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(
                model(
                    KeyMap(trigger = Trigger(keys = listOf(keyEventKey(requiresIme = true)))),
                ),
            ),
        )

        assertThat(features, not(hasItem(AccessibilityServiceFeature.FILTER_KEY_EVENTS)))
    }

    @Test
    fun `When a key map has a fingerprint trigger then request fingerprint gestures`() {
        val key = FingerprintTriggerKey(
            type = FingerprintGestureType.SWIPE_DOWN,
            clickType = ClickType.SHORT_PRESS,
        )

        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(model(KeyMap(trigger = Trigger(keys = listOf(key))))),
        )

        assertThat(features, hasItem(AccessibilityServiceFeature.FINGERPRINT_GESTURES))
    }

    @Test
    fun `When a key map has a sound action then request the accessibility volume`() {
        val action = ActionData.Sound.SoundFile(soundUid = "uid", soundDescription = "beep")

        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(model(KeyMap(actionList = listOf(Action(data = action))))),
        )

        assertThat(features, hasItem(AccessibilityServiceFeature.ACCESSIBILITY_VOLUME))
    }

    @Test
    fun `When a key map interacts with a UI element then request the window state and nodes`() {
        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(
                model(KeyMap(actionList = listOf(Action(data = interactUiElementAction())))),
            ),
        )

        assertThat(features, hasItem(AccessibilityServiceFeature.WINDOW_STATE))
        assertThat(features, hasItem(AccessibilityServiceFeature.NODE_INFO))
    }

    @Test
    fun `When a key map is constrained to an app then request the window state`() {
        val constraint = Constraint(
            data = ConstraintData.AppInForeground(packageName = "com.example"),
        )

        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(
                model(
                    KeyMap(constraintState = ConstraintState(constraints = setOf(constraint))),
                ),
            ),
        )

        assertThat(features, hasItem(AccessibilityServiceFeature.WINDOW_STATE))
    }

    @Test
    fun `When a group is constrained to the notification panel then request the window nodes`() {
        val constraint = Constraint(data = ConstraintData.NotificationPanelShowing)

        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(
                DetectKeyMapModel(
                    keyMap = KeyMap(),
                    groupConstraintStates = listOf(
                        ConstraintState(constraints = setOf(constraint)),
                    ),
                ),
            ),
        )

        assertThat(features, hasItem(AccessibilityServiceFeature.WINDOW_STATE))
        // The notification panel is detected by looking for the brightness slider by its view id.
        assertThat(features, hasItem(AccessibilityServiceFeature.NODE_INFO))
    }

    @Test
    fun `When a key map only opens an app then request nothing`() {
        val features = AccessibilityServiceFeatureUtils.getRequiredFeatures(
            listOf(
                model(
                    KeyMap(
                        actionList = listOf(
                            Action(data = ActionData.App(packageName = "com.example")),
                        ),
                    ),
                ),
            ),
        )

        assertThat(features, `is`(emptySet()))
    }

    private fun model(keyMap: KeyMap): DetectKeyMapModel = DetectKeyMapModel(keyMap = keyMap)

    private fun keyEventKey(requiresIme: Boolean = false): KeyEventTriggerKey = KeyEventTriggerKey(
        keyCode = 25,
        device = KeyEventTriggerDevice.Internal,
        clickType = ClickType.SHORT_PRESS,
        requiresIme = requiresIme,
    )

    private fun interactUiElementAction(): ActionData.InteractUiElement =
        ActionData.InteractUiElement(
            description = "Button",
            nodeAction = NodeInteractionType.CLICK,
            packageName = "com.example",
            text = null,
            tooltip = null,
            hint = null,
            contentDescription = null,
            className = null,
            viewResourceId = null,
            uniqueId = null,
            nodeActions = setOf(NodeInteractionType.CLICK),
        )
}
