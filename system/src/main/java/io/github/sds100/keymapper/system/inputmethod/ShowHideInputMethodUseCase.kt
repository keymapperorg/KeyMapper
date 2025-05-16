package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.util.ServiceEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking



class ShowHideInputMethodUseCaseImpl(
    private val serviceAdapter: ServiceAdapter,
) : ShowHideInputMethodUseCase {
    override val onHiddenChange: Flow<Boolean> = serviceAdapter.eventReceiver.mapNotNull {
        when (it) {
            ServiceEvent.OnHideKeyboardEvent -> true
            ServiceEvent.OnShowKeyboardEvent -> false
            else -> null
        }
    }

    override fun show() {
        runBlocking { serviceAdapter.send(ServiceEvent.ShowKeyboard) }
    }

    override fun hide() {
        runBlocking { serviceAdapter.send(ServiceEvent.HideKeyboard) }
    }
}

interface ShowHideInputMethodUseCase {
    val onHiddenChange: Flow<Boolean>
    fun show()
    fun hide()
}
