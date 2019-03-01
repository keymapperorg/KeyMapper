package io.github.sds100.keymapper.selection

/**
 * Created by sds100 on 12/09/2018.
 */

enum class SelectionEvent {
    /**
     * When the user starts selection mode
     */
    START,

    /**
     * When the user stops selection mode
     */
    STOP,

    /**
     * When an item is selected
     */
    SELECTED,

    /**
     * When an item is unselected
     */
    UNSELECTED,

    /**
     * When all item are selected
     */
    SELECT_ALL
}