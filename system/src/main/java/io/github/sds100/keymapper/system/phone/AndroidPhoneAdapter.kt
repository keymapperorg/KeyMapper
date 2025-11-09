package io.github.sds100.keymapper.system.phone

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

@Singleton
class AndroidPhoneAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
) : PhoneAdapter {
    companion object {
        private const val ACTION_SMS_SENT_RESULT = "io.github.sds100.keymapper.SMS_SENT_RESULT"

        /**
         * The minimum frequency that SMS messages can be sent.
         */
        private const val SMS_MIN_RATE_MILLIS = 1000L
    }

    private val ctx: Context = context.applicationContext
    private val telecomManager: TelecomManager? = ctx.getSystemService()
    private val telephonyManager: TelephonyManager? = ctx.getSystemService()

    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)

            coroutineScope.launch {
                updateCallState(state)
            }
        }
    }

    override val callStateFlow: MutableSharedFlow<CallState> = MutableSharedFlow()

    /**
     * Emits the result code in SmsManager
     */
    private val smsSentResultFlow = Channel<Int>()

    /**
     * The time the last SMS was sent. This is used to prevent someone accidentally incurring
     * significant charges.
     */
    private var lastSmsTime: Long = -1

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context ?: return
            intent ?: return

            when (intent.action) {
                ACTION_SMS_SENT_RESULT -> {
                    smsSentResultFlow.trySend(resultCode)
                }
            }
        }
    }

    init {
        if (telephonyManager != null) {
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

        IntentFilter().apply {
            addAction(ACTION_SMS_SENT_RESULT)

            ContextCompat.registerReceiver(
                ctx,
                broadcastReceiver,
                this,
                ContextCompat.RECEIVER_EXPORTED,
            )
        }
    }

    override fun getCallState(): CallState {
        if (telephonyManager == null) {
            throw Exception("TelephonyManager is null. Does this device support telephony?")
        }

        return callStateConverter(telephonyManager.callState)
    }

    override fun startCall(number: String): KMResult<*> {
        try {
            Intent(Intent.ACTION_CALL).apply {
                data = "tel:$number".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return KMError.NoAppToPhoneCall
        }
    }

    @SuppressLint("MissingPermission")
    override fun answerCall() {
        if (!hasAnswerPhoneCallsPermission()) {
            return
        }

        telecomManager?.acceptRingingCall()
    }

    private fun hasAnswerPhoneCallsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ANSWER_PHONE_CALLS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    override fun endCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (!hasAnswerPhoneCallsPermission()) {
                return
            }

            telecomManager?.endCall()
        }
    }

    override suspend fun sendSms(number: String, message: String): KMResult<Unit> {
        val smsManager: SmsManager? = ctx.getSystemService()

        if (smsManager == null) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                KMError.SystemFeatureNotSupported(PackageManager.FEATURE_TELEPHONY_MESSAGING)
            } else {
                KMError.SystemFeatureNotSupported(PackageManager.FEATURE_TELEPHONY)
            }
        }

        val sentPendingIntent =
            PendingIntent.getBroadcast(
                ctx,
                0,
                Intent(ACTION_SMS_SENT_RESULT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        try {
            if (SystemClock.uptimeMillis() - lastSmsTime < SMS_MIN_RATE_MILLIS) {
                Timber.d("SMS rate limit exceeded to protect against significant costs")
                return KMError.KeyMapperSmsRateLimit
            }

            smsManager.sendTextMessage(number, null, message, sentPendingIntent, null)
            lastSmsTime = SystemClock.uptimeMillis()
        } catch (e: IllegalArgumentException) {
            return KMError.Exception(e)
        }

        try {
            return withTimeout(10000L) {
                val resultCode = smsSentResultFlow.receive()

                when (resultCode) {
                    Activity.RESULT_OK -> Success(Unit)
                    else -> KMError.SendSmsError(resultCode)
                }
            }
        } catch (e: TimeoutCancellationException) {
            return KMError.Exception(e)
        }
    }

    override fun composeSms(number: String, message: String): KMResult<Unit> {
        try {
            Intent(Intent.ACTION_VIEW).apply {
                data = "smsto:$number".toUri()
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                ctx.startActivity(this)
            }

            return Success(Unit)
        } catch (e: ActivityNotFoundException) {
            return KMError.NoAppToSendSms
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
            else -> throw IllegalArgumentException(
                "Don't know how to convert that call state $sdkCallState",
            )
        }
    }
}
