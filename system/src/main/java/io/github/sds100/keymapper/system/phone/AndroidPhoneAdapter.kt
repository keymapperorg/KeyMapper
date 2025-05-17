package io.github.sds100.keymapper.system.phone

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.Success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPhoneAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) : PhoneAdapter {
    private val ctx: Context = context.applicationContext
    private val telecomManager: TelecomManager = ctx.getSystemService()!!
    private val telephonyManager: TelephonyManager = ctx.getSystemService()!!

    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)

            coroutineScope.launch {
                updateCallState(state)
            }
        }
    }

    override val callStateFlow: MutableSharedFlow<CallState> = MutableSharedFlow()

    init {
        coroutineScope.launch {
            callStateFlow.subscriptionCount.collect { subscriptionCount ->
                if (subscriptionCount == 0) {
                    telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
                } else {
                    telephonyManager.listen(
                        phoneStateListener,
                        PhoneStateListener.LISTEN_CALL_STATE,
                    )
                }
            }
        }
    }

    override fun getCallState(): CallState = callStateConverter(telephonyManager.callState)

    override fun startCall(number: String): Result<*> {
        try {
            Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$number")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return Error.NoAppToPhoneCall
        }
    }

    override fun answerCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            telecomManager.acceptRingingCall()
        }
    }

    override fun endCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            telecomManager.endCall()
        }
    }

    private suspend fun updateCallState(sdkCallState: Int) {
        callStateFlow.emit(callStateConverter(sdkCallState))
    }

    private fun callStateConverter(sdkCallState: Int): CallState {
        when (sdkCallState) {
            TelephonyManager.CALL_STATE_IDLE -> return CallState.NONE
            TelephonyManager.CALL_STATE_OFFHOOK -> return CallState.IN_PHONE_CALL
            TelephonyManager.CALL_STATE_RINGING -> return CallState.RINGING
            else -> throw IllegalArgumentException("Don't know how to convert that call state $sdkCallState")
        }
    }
}
