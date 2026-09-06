package io.github.sds100.keymapper.base.system.accessibility

import android.os.Build
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.constraints.Constraint
import io.github.sds100.keymapper.base.constraints.ConstraintDependency
import io.github.sds100.keymapper.base.constraints.ConstraintUtils
import io.github.sds100.keymapper.base.detection.DetectKeyMapModel
import io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey

/**
 * Works out which [AccessibilityServiceFeature]s the user's key maps need so that the
 * accessibility service only requests those. See [AccessibilityServiceInfoController].
 */
object AccessibilityServiceFeatureUtils {

    /**
     * The features that the enabled key maps need. This does not include the features that are
     * only needed while the user is doing something in the app, such as recording a trigger.
     */
    fun getRequiredFeatures(keyMaps: List<DetectKeyMapModel>): Set<AccessibilityServiceFeature> {
        val features = mutableSetOf<AccessibilityServiceFeature>()

        for (model in keyMaps) {
            if (!model.keyMap.isEnabled) {
                continue
            }

            for (key in model.keyMap.trigger.keys) {
                when (key) {
                    // Keys that can only be detected by an input method never reach the
                    // accessibility service.
                    is KeyEventTriggerKey -> if (!key.requiresIme) {
                        features.add(AccessibilityServiceFeature.FILTER_KEY_EVENTS)
                    }

                    is FingerprintTriggerKey ->
                        features.add(AccessibilityServiceFeature.FINGERPRINT_GESTURES)

                    else -> Unit
                }
            }

            for (action in model.keyMap.actionList) {
                features.addAll(getRequiredFeatures(action.data))
            }

            for (constraint in model.keyMap.constraintState.constraints) {
                features.addAll(getRequiredFeatures(constraint))
            }

            for (constraintState in model.groupConstraintStates) {
                for (constraint in constraintState.constraints) {
                    features.addAll(getRequiredFeatures(constraint))
                }
            }
        }

        return features
    }

    /**
     * The features that an action needs to be performed.
     */
    fun getRequiredFeatures(action: ActionData): Set<AccessibilityServiceFeature> {
        return when (action) {
            is ActionData.InteractUiElement -> setOf(
                // The action checks which app is in the foreground before looking for the node.
                AccessibilityServiceFeature.WINDOW_STATE,
                AccessibilityServiceFeature.NODE_INFO,
            )

            // These read the packages of the windows that are currently on the screen.
            ActionData.ForceStopApp,
            ActionData.ClearRecentApp,
                -> setOf(AccessibilityServiceFeature.WINDOW_STATE)

            // These find and act on the node that the user is typing in.
            is ActionData.MoveCursor,
            ActionData.CutText,
            ActionData.CopyText,
            ActionData.PasteText,
            ActionData.SelectWordAtCursor,
            ActionData.SelectAllText,
                -> setOf(AccessibilityServiceFeature.NODE_INFO)

            is ActionData.Sound -> setOf(AccessibilityServiceFeature.ACCESSIBILITY_VOLUME)

            // Uses the accessibility service's own input method, which is only available on
            // Android 13+.
            ActionData.PerformImeAction ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setOf(AccessibilityServiceFeature.INPUT_METHOD_EDITOR)
                } else {
                    emptySet()
                }

            // Text is inserted through the accessibility service's own input method on
            // Android 13+. Older versions use the Key Mapper input method instead.
            is ActionData.Text ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    setOf(AccessibilityServiceFeature.INPUT_METHOD_EDITOR)
                } else {
                    emptySet()
                }

            else -> emptySet()
        }
    }

    private fun getRequiredFeatures(constraint: Constraint): Set<AccessibilityServiceFeature> {
        val features = mutableSetOf<AccessibilityServiceFeature>()

        for (dependency in with(ConstraintUtils) { constraint.getDependency() }) {
            when (dependency) {
                ConstraintDependency.FOREGROUND_APP,
                ConstraintDependency.LOCK_SCREEN_SHOWING,
                ConstraintDependency.KEYBOARD_VISIBLE,
                    -> features.add(AccessibilityServiceFeature.WINDOW_STATE)

                // The notification shade is detected on the lock screen by looking for the
                // brightness slider by its view id.
                ConstraintDependency.NOTIFICATION_PANEL_STATE -> {
                    features.add(AccessibilityServiceFeature.WINDOW_STATE)
                    features.add(AccessibilityServiceFeature.NODE_INFO)
                }

                else -> Unit
            }
        }

        return features
    }
}
