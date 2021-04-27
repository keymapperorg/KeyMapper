package io.github.sds100.keymapper.util.ui

import io.github.sds100.keymapper.util.ui.PopupUi

/**
 * Created by sds100 on 23/03/2021.
 */
data class ShowPopupEvent(val key: String, val ui: PopupUi<*>)