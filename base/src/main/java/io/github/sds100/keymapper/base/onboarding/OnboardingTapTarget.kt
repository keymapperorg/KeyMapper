package io.github.sds100.keymapper.base.onboarding

import androidx.annotation.StringRes
import io.github.sds100.keymapper.base.R

enum class OnboardingTapTarget(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
) {
    CHOOSE_ACTION(
        titleRes = R.string.tap_target_choose_action_title,
        messageRes = R.string.tap_target_choose_action_message,
    ),
}
