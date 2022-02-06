package io.github.sds100.keymapper.system.phone

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 21/04/2021.
 */
interface PhoneAdapter {
    val callStateFlow: Flow<CallState>

    fun getCallState(): CallState
    fun startCall(number: String): Result<*>
    fun answerCall()
    fun endCall()
}