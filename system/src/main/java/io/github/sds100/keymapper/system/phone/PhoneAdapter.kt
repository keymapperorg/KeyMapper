package io.github.sds100.keymapper.system.phone

import io.github.sds100.keymapper.common.util.result.Result
import kotlinx.coroutines.flow.Flow


interface PhoneAdapter {
    val callStateFlow: Flow<CallState>

    /**
     * Must check if Key Mapper has READ_PHONE_STATE permission before calling this. Otherwise
     * a security exception will be thrown.
     */
    fun getCallState(): CallState
    fun startCall(number: String): Result<*>
    fun answerCall()
    fun endCall()
}
