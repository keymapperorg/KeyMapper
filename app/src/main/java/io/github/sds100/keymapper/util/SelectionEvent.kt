package io.github.sds100.keymapper.util

/**
 * Created by sds100 on 12/01/21.
 */
sealed class SelectionEvent
class SelectAll : SelectionEvent()
class Selected(id: Long) : SelectionEvent()
class Unselected(id: Long) : SelectionEvent()