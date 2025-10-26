package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class ShowHideInputMethodUseCaseImpl
    @Inject
    constructor(
        private val serviceAdapter: AccessibilityServiceAdapter,
    ) : ShowHideInputMethodUseCase {
        override val onHiddenChange: Flow<Boolean> =
            serviceAdapter.eventReceiver.mapNotNull {
                when (it) {
                    AccessibilityServiceEvent.OnHideKeyboardEvent -> true
                    AccessibilityServiceEvent.OnShowKeyboardEvent -> false
                    else -> null
                }
            }

        override fun show() {
            runBlocking { serviceAdapter.send(AccessibilityServiceEvent.ShowKeyboard) }
        }

        override fun hide() {
            runBlocking { serviceAdapter.send(AccessibilityServiceEvent.HideKeyboard) }
        }
    }

interface ShowHideInputMethodUseCase {
    val onHiddenChange: Flow<Boolean>

    fun show()

    fun hide()
}
