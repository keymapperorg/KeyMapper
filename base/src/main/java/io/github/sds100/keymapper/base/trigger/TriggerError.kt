package io.github.sds100.keymapper.base.trigger

enum class TriggerError(val isFixable: Boolean) {
    DND_ACCESS_DENIED(isFixable = true),
    SCREEN_OFF_ROOT_DENIED(isFixable = true),
    CANT_DETECT_IN_PHONE_CALL(isFixable = true),

    // This error appears when a key map has an assistant trigger but the user hasn't purchased
    // the product.
    ASSISTANT_TRIGGER_NOT_PURCHASED(isFixable = true),

    /**
     * A Key Mapper IME must be used for DPAD triggers to work.
     */
    DPAD_IME_NOT_SELECTED(isFixable = true),

    FLOATING_BUTTON_DELETED(isFixable = false),

    FLOATING_BUTTONS_NOT_PURCHASED(isFixable = true),

    PURCHASE_VERIFICATION_FAILED(isFixable = true),
}
