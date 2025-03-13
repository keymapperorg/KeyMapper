package io.github.sds100.keymapper.mappings.keymaps.trigger

/**
 * Created by sds100 on 04/04/2021.
 */
enum class TriggerError {
    DND_ACCESS_DENIED,
    SCREEN_OFF_ROOT_DENIED,
    CANT_DETECT_IN_PHONE_CALL,

    // This error appears when a key map has an assistant trigger but the user hasn't purchased
    // the product.
    ASSISTANT_TRIGGER_NOT_PURCHASED,

    /**
     * A Key Mapper IME must be used for DPAD triggers to work.
     */
    DPAD_IME_NOT_SELECTED,
}
