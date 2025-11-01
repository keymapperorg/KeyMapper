package io.github.sds100.keymapper.base.onboarding

data class OnboardingTipModel(
    val id: String,
    val title: String,
    val message: String,
    val isDismissable: Boolean,
    val buttonText: String? = null,
)
