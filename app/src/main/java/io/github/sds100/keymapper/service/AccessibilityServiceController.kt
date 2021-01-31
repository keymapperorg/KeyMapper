package io.github.sds100.keymapper.service

import android.os.Build
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.hadilq.liveevent.LiveEvent
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.*
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.repository.FingerprintMapRepository
import io.github.sds100.keymapper.data.usecase.GlobalKeymapUseCase
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.*
import kotlinx.coroutines.*
import splitties.bitflags.hasFlag
import splitties.toast.toast
import timber.log.Timber

/**
 * Created by sds100 on 18/01/21.
 */
class AccessibilityServiceController(
    lifecycleOwner: LifecycleOwner,
    iAccessibilityService: IAccessibilityService,
    constraintState: IConstraintState,
    clock: IClock,
    actionError: IActionError,
    private val appUpdateManager: AppUpdateManager,
    private val globalPreferences: IGlobalPreferences,
    private val fingerprintMapRepository: FingerprintMapRepository,
    private val keymapRepository: GlobalKeymapUseCase
) : IAccessibilityService by iAccessibilityService, LifecycleOwner by lifecycleOwner {

    companion object {
        /**
         * How long should the accessibility service record a trigger in seconds.
         */
        private const val RECORD_TRIGGER_TIMER_LENGTH = 5
    }

    private val constraintDelegate = ConstraintDelegate(constraintState)

    private val triggerKeymapByIntentController = TriggerKeymapByIntentController(
        lifecycleScope,
        globalPreferences,
        constraintDelegate,
        actionError
    )

    private val fingerprintGestureMapController = FingerprintGestureMapController(
        lifecycleScope,
        globalPreferences,
        constraintDelegate,
        actionError
    )

    private var recordingTriggerJob: Job? = null

    private val recordingTrigger: Boolean
        get() = recordingTriggerJob != null && recordingTriggerJob?.isActive == true

    private val getEventDelegate = GetEventDelegate { keyCode, action, deviceDescriptor, isExternal, deviceId ->

        if (!globalPreferences.keymapsPaused.firstBlocking()) {
            withContext(Dispatchers.Main.immediate) {
                keymapDetectionDelegate.onKeyEvent(
                    keyCode,
                    action,
                    deviceDescriptor,
                    isExternal,
                    0,
                    deviceId
                )
            }
        }
    }

    private var screenOffTriggersEnabled = false

    private val keymapDetectionDelegate = KeymapDetectionDelegate(
        lifecycleScope,
        getKeymapDetectionDelegatePreferences(),
        clock,
        actionError,
        constraintDelegate
    )

    private val _eventStream = LiveEvent<Event>().apply {
        //vibrate
        addSource(keymapDetectionDelegate.vibrate) {
            value = it
        }

        addSource(fingerprintGestureMapController.vibrateEvent) {
            value = it
        }

        addSource(triggerKeymapByIntentController.vibrateEvent) {
            value = it
        }

        //perform action
        addSource(keymapDetectionDelegate.performAction) {
            value = it
        }

        addSource(fingerprintGestureMapController.performAction) {
            value = it
        }

        addSource(triggerKeymapByIntentController.performAction) {
            value = it
        }

        //show triggered keymap toast
        addSource(keymapDetectionDelegate.showTriggeredKeymapToast) {
            value = it
        }

        addSource(triggerKeymapByIntentController.showTriggeredToastEvent) {
            value = it
        }

        addSource(fingerprintGestureMapController.showTriggeredToastEvent) {
            value = it
        }

    }

    val eventStream: LiveData<Event> = _eventStream

    init {
        lifecycleScope.launch {
            val oldVersion = appUpdateManager.getLastVersionCodeAccessibilityService()
            if (oldVersion == Constants.VERSION_CODE) return@launch

            requestFingerprintGestureDetection()

            val handledUpdateInHomeScreen =
                appUpdateManager.getLastVersionCodeHomeScreen() == Constants.VERSION_CODE

            if (oldVersion < FingerprintMapUtils.FINGERPRINT_GESTURES_MIN_VERSION
                && isGestureDetectionAvailable
                && !handledUpdateInHomeScreen) {
                _eventStream.postValue(ShowFingerprintFeatureNotification)
            }

            denyFingerprintGestureDetection()

            appUpdateManager.handledAppUpdateInAccessibilityService()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            checkFingerprintGesturesAvailability()

            observeFingerprintMaps()
        }

        lifecycleScope.launchWhenStarted {
            val keymapList = withContext(Dispatchers.Default) {
                keymapRepository.getKeymaps()
            }

            withContext(Dispatchers.Main) {
                updateKeymapListCache(keymapList)
            }
        }

        subscribeToPreferenceChanges()
    }

    fun onKeyEvent(keyCode: Int,
                   action: Int,
                   deviceName: String,
                   descriptor: String,
                   isExternal: Boolean,
                   metaState: Int,
                   deviceId: Int,
                   scanCode: Int = 0): Boolean {

        if (recordingTrigger) {
            if (action == KeyEvent.ACTION_DOWN) {
                _eventStream.value = RecordedTriggerKeyEvent(
                    keyCode,
                    deviceName,
                    descriptor,
                    isExternal
                )
            }

            return true
        }

        if (!globalPreferences.keymapsPaused.firstBlocking()) {
            try {
                val consume = keymapDetectionDelegate.onKeyEvent(
                    keyCode,
                    action,
                    descriptor,
                    isExternal,
                    metaState,
                    deviceId,
                    scanCode)

                return consume

            } catch (e: Exception) {
                Timber.e(e)
            }
        }

        return false
    }

    fun onFingerprintGesture(sdkGestureId: Int) {
        fingerprintGestureMapController.onGesture(sdkGestureId)
    }

    fun onScreenOn() {
        getEventDelegate.stopListening()
    }

    fun onScreenOff() {
        val hasRootPermission = globalPreferences.hasRootPermission.firstBlocking()

        if (hasRootPermission && screenOffTriggersEnabled) {
            if (!getEventDelegate.startListening(lifecycleScope)) {
                toast(R.string.error_failed_execute_getevent)
            }
        }
    }

    fun triggerKeymapByIntent(uid: String) {
        triggerKeymapByIntentController.onDetected(uid)
    }

    fun startRecordingTrigger() {
        //don't start recording if a trigger is being recorded
        if (!recordingTrigger) {
            recordingTriggerJob = recordTriggerJob()
        }
    }

    fun stopRecordingTrigger() {
        val wasRecordingTrigger = recordingTrigger

        recordingTriggerJob?.cancel()
        recordingTriggerJob = null

        if (wasRecordingTrigger) {
            _eventStream.value = OnStoppedRecordingTrigger
        }
    }

    fun updateKeymapListCache(keymapList: List<KeyMap>) {
        keymapDetectionDelegate.keymapListCache = keymapList

        screenOffTriggersEnabled = keymapList.any { keymap ->
            keymap.trigger.flags.hasFlag(Trigger.TRIGGER_FLAG_SCREEN_OFF_TRIGGERS)
        }

        triggerKeymapByIntentController.onKeymapListUpdate(keymapList)
    }

    private fun recordTriggerJob() = lifecycleScope.launchWhenStarted {
        repeat(RECORD_TRIGGER_TIMER_LENGTH) { iteration ->
            if (isActive) {
                val timeLeft = RECORD_TRIGGER_TIMER_LENGTH - iteration
                _eventStream.value = OnIncrementRecordTriggerTimer(timeLeft)

                delay(1000)
            }
        }

        _eventStream.value = OnStoppedRecordingTrigger
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeFingerprintMaps() {
        fingerprintMapRepository.fingerprintGestureMapsLiveData.observe(this, { maps ->
            fingerprintGestureMapController.fingerprintMaps = maps

            invalidateFingerprintGestureDetection()
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun invalidateFingerprintGestureDetection() {
        fingerprintGestureMapController.fingerprintMaps.let { maps ->
            if (maps.any { it.value.isEnabled && it.value.actionList.isNotEmpty() }
                && !globalPreferences.keymapsPaused.firstBlocking()) {
                requestFingerprintGestureDetection()
            } else {
                denyFingerprintGestureDetection()
            }
        }
    }

    private fun checkFingerprintGesturesAvailability() {
        requestFingerprintGestureDetection()

        //this is important
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            /* Don't update whether fingerprint gesture detection is supported if it has
            * been supported at some point. Just in case the fingerprint reader is being
            * used while this is called. */
            if (fingerprintMapRepository.fingerprintGesturesAvailable.firstBlocking() != true) {
                fingerprintMapRepository
                    .setFingerprintGesturesAvailable(isGestureDetectionAvailable)
            }
        }

        denyFingerprintGestureDetection()
    }

    private fun getKeymapDetectionDelegatePreferences() = KeymapDetectionPreferences(
        globalPreferences.longPressDelay.firstBlocking(),
        globalPreferences.doublePressDelay.firstBlocking(),
        globalPreferences.repeatDelay.firstBlocking(),
        globalPreferences.repeatRate.firstBlocking(),
        globalPreferences.sequenceTriggerTimeout.firstBlocking(),
        globalPreferences.vibrationDuration.firstBlocking(),
        globalPreferences.holdDownDuration.firstBlocking(),
        globalPreferences.getFlow(Keys.forceVibrate).firstBlocking() ?: false
    )


    private fun subscribeToPreferenceChanges() {
        globalPreferences.longPressDelay.collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.defaultLongPressDelay = it
        }

        globalPreferences.doublePressDelay.collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.defaultDoublePressDelay = it
        }

        globalPreferences.repeatDelay.collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.defaultRepeatDelay = it
        }

        globalPreferences.repeatRate.collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.defaultRepeatRate = it
        }

        globalPreferences.sequenceTriggerTimeout.collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.defaultSequenceTriggerTimeout = it
        }

        globalPreferences.vibrationDuration.collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.defaultVibrateDuration = it
        }

        globalPreferences.holdDownDuration.collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.defaultHoldDownDuration = it
        }

        globalPreferences.getFlow(Keys.forceVibrate).collectWhenStarted(this) {
            keymapDetectionDelegate.preferences.forceVibrate = it ?: false
        }

        globalPreferences.keymapsPaused.collectWhenStarted(this) { paused ->
            keymapDetectionDelegate.reset()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (paused) {
                    denyFingerprintGestureDetection()
                } else {
                    requestFingerprintGestureDetection()
                }
            }
        }
    }
}