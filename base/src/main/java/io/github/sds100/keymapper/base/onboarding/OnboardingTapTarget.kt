package io.github.sds100.keymapper.base.onboarding

import androidx.annotation.StringRes
import io.github.sds100.keymapper.base.R

enum class OnboardingTapTarget(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
) {
    CREATE_KEY_MAP(
        titleRes = R.string.tap_target_create_key_map_title,
        messageRes = R.string.tap_target_create_key_map_message,
    ),

    RECORD_TRIGGER(
        titleRes = R.string.tap_target_record_trigger_title,
        messageRes = R.string.tap_target_record_trigger_message,
    ),

    ADVANCED_TRIGGERS(
        titleRes = R.string.tap_target_advanced_triggers_title,
        messageRes = R.string.tap_target_advanced_triggers_message,
    ),

    CHOOSE_ACTION(
        titleRes = R.string.tap_target_choose_action_title,
        messageRes = R.string.tap_target_choose_action_message,
    ),

    CHOOSE_CONSTRAINT(
        titleRes = R.string.tap_target_choose_constraint_title,
        messageRes = R.string.tap_target_choose_constraint_message,
    ),
}
