package io.github.sds100.keymapper.system.phone

import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface PhoneAdapter {
    val callStateFlow: Flow<CallState>

    /**
     * Must check if Key Mapper has READ_PHONE_STATE permission before calling this. Otherwise
     * a security exception will be thrown.
     */
    fun getCallState(): CallState
    fun startCall(number: String): KMResult<*>
    fun answerCall()
    fun endCall()
    suspend fun sendSms(number: String, message: String): KMResult<Unit>
    fun composeSms(number: String, message: String): KMResult<Unit>
}
