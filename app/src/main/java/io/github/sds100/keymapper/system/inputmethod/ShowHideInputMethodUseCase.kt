package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.util.HideKeyboardEvent
import io.github.sds100.keymapper.util.OnHideKeyboardEvent
import io.github.sds100.keymapper.util.OnShowKeyboardEvent
import io.github.sds100.keymapper.util.ShowKeyboardEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 16/04/2021.
 */

class ShowHideInputMethodUseCaseImpl(
    private val serviceAdapter: ServiceAdapter
) : ShowHideInputMethodUseCase {
    override val onHiddenChange: Flow<Boolean> = serviceAdapter.eventReceiver.mapNotNull {
        when (it) {
            OnHideKeyboardEvent -> true
            OnShowKeyboardEvent -> false
            else -> null
        }
    }

    override fun show() {
        runBlocking { serviceAdapter.send(ShowKeyboardEvent) }
    }

    override fun hide() {
        runBlocking { serviceAdapter.send(HideKeyboardEvent) }
    }
}

interface ShowHideInputMethodUseCase {
    val onHiddenChange: Flow<Boolean>
    fun show()
    fun hide()
}