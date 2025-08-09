package io.github.sds100.keymapper.base.detection

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.constraints.ConstraintState
import io.github.sds100.keymapper.base.groups.Group
import io.github.sds100.keymapper.base.groups.GroupEntityMapper
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.base.input.InputEventHub
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.base.keymaps.KeyMapEntityMapper
import io.github.sds100.keymapper.base.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.base.system.navigation.OpenMenuHelper
import io.github.sds100.keymapper.base.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.popup.ToastAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.system.volume.VolumeAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import timber.log.Timber

class DetectKeyMapsUseCaseImpl @AssistedInject constructor(
    @Assisted
    private val accessibilityService: IAccessibilityService,
    private val keyMapRepository: KeyMapRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val groupRepository: GroupRepository,
    private val preferenceRepository: PreferenceRepository,
    private val suAdapter: SuAdapter,
    private val volumeAdapter: VolumeAdapter,
    private val toastAdapter: ToastAdapter,
    private val resourceProvider: ResourceProvider,
    private val vibrator: VibratorAdapter,
    @Assisted
    private val coroutineScope: CoroutineScope,
    private val inputEventHub: InputEventHub,
) : DetectKeyMapsUseCase {

    @AssistedFactory
    interface Factory {
        fun create(
            accessibilityService: IAccessibilityService,
            coroutineScope: CoroutineScope,
        ): DetectKeyMapsUseCaseImpl
    }

    companion object {
        fun processKeyMapsAndGroups(
            keyMaps: List<KeyMap>,
            groups: List<Group>,
        ): List<DetectKeyMapModel> = buildList {
            val groupMap = groups.associateBy { it.uid }

            keyMapLoop@ for (keyMap in keyMaps) {
                var depth = 0
                var groupUid: String? = keyMap.groupUid
                val constraintStates = mutableListOf<ConstraintState>()

                while (depth < 100) {
                    if (groupUid == null) {
                        add(
                            DetectKeyMapModel(
                                keyMap = keyMap,
                                groupConstraintStates = constraintStates,
                            ),
                        )
                        break
                    }

                    if (!groupMap.containsKey(groupUid)) {
                        continue@keyMapLoop
                    }

                    val group = groupMap[groupUid]!!
                    groupUid = group.parentUid

                    if (group.constraintState.constraints.isNotEmpty()) {
                        constraintStates.add(group.constraintState)
                    }

                    depth++
                }
            }
        }
    }

    override val allKeyMapList: Flow<List<DetectKeyMapModel>> = combine(
        keyMapRepository.keyMapList,
        floatingButtonRepository.buttonsList,
        groupRepository.groups,
    ) { keyMapListState, buttonListState, groupEntities ->
        if (keyMapListState is State.Loading || buttonListState is State.Loading) {
            return@combine emptyList()
        }

        val keyMapEntityList = keyMapListState.dataOrNull() ?: return@combine emptyList()
        val buttonEntityList = buttonListState.dataOrNull() ?: return@combine emptyList()

        val keyMapList = keyMapEntityList.map { keyMap ->
            KeyMapEntityMapper.fromEntity(keyMap, buttonEntityList)
        }

        val groupList = groupEntities.map { GroupEntityMapper.fromEntity(it) }

        processKeyMapsAndGroups(keyMapList, groupList)
    }.flowOn(Dispatchers.Default)

    override val requestFingerprintGestureDetection: Flow<Boolean> =
        allKeyMapList.map { models ->
            models.any { model ->
                model.keyMap.isEnabled && model.keyMap.trigger.keys.any { it is FingerprintTriggerKey }
            }
        }

    override val keyMapsToTriggerFromOtherApps: Flow<List<KeyMap>> =
        allKeyMapList.map { keyMapList ->
            keyMapList.filter { it.keyMap.trigger.triggerFromOtherApps }.map { it.keyMap }
        }.flowOn(Dispatchers.Default)

    override val detectScreenOffTriggers: Flow<Boolean> =
        combine(
            allKeyMapList,
            suAdapter.isRooted,
        ) { keyMapList, isRootPermissionGranted ->
            keyMapList.any { it.keyMap.trigger.screenOffTrigger } && isRootPermissionGranted
        }.flowOn(Dispatchers.Default)

    override val defaultLongPressDelay: Flow<Long> =
        preferenceRepository.get(Keys.defaultLongPressDelay)
            .map { it ?: PreferenceDefaults.LONG_PRESS_DELAY }
            .map { it.toLong() }

    override val defaultDoublePressDelay: Flow<Long> =
        preferenceRepository.get(Keys.defaultDoublePressDelay)
            .map { it ?: PreferenceDefaults.DOUBLE_PRESS_DELAY }
            .map { it.toLong() }

    override val defaultSequenceTriggerTimeout: Flow<Long> =
        preferenceRepository.get(Keys.defaultSequenceTriggerTimeout)
            .map { it ?: PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT }
            .map { it.toLong() }

    override val currentTime: Long
        get() = SystemClock.elapsedRealtime()

    private val openMenuHelper = OpenMenuHelper(
        accessibilityService,
        inputEventHub,
    )

    override val forceVibrate: Flow<Boolean> =
        preferenceRepository.get(Keys.forceVibrate).map { it == true }

    override val defaultVibrateDuration: Flow<Long> =
        preferenceRepository.get(Keys.defaultVibrateDuration)
            .map { it ?: PreferenceDefaults.VIBRATION_DURATION }
            .map { it.toLong() }

    override fun showTriggeredToast() {
        toastAdapter.show(resourceProvider.getString(R.string.toast_triggered_keymap))
    }

    override fun vibrate(duration: Long) {
        vibrator.vibrate(duration)
    }

    override fun imitateKeyEvent(
        keyCode: Int,
        metaState: Int,
        deviceId: Int,
        action: Int,
        scanCode: Int,
        source: Int,
    ) {
        val model = InjectKeyEventModel(
            keyCode = keyCode,
            action = action,
            metaState = metaState,
            deviceId = deviceId,
            scanCode = scanCode,
            source = source,
        )

        if (inputEventHub.isSystemBridgeConnected()) {
            Timber.d("Imitate button press ${KeyEvent.keyCodeToString(keyCode)} with system bridge, key code: $keyCode, device id: $deviceId, meta state: $metaState, scan code: $scanCode")
            inputEventHub.injectKeyEventAsync(model)
        } else {
            Timber.d("Imitate button press ${KeyEvent.keyCodeToString(keyCode)}, key code: $keyCode, device id: $deviceId, meta state: $metaState, scan code: $scanCode")

            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> volumeAdapter.raiseVolume(showVolumeUi = true)

                KeyEvent.KEYCODE_VOLUME_DOWN -> volumeAdapter.lowerVolume(showVolumeUi = true)

                KeyEvent.KEYCODE_BACK -> accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                KeyEvent.KEYCODE_HOME -> accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                KeyEvent.KEYCODE_APP_SWITCH -> accessibilityService.doGlobalAction(
                    AccessibilityService.GLOBAL_ACTION_POWER_DIALOG,
                )

                KeyEvent.KEYCODE_MENU -> openMenuHelper.openMenu()

                else -> inputEventHub.injectKeyEventAsync(model)
            }
        }
    }

    override fun imitateEvdevEvent(devicePath: String, type: Int, code: Int, value: Int) {
        if (inputEventHub.isSystemBridgeConnected()) {
            Timber.d("Imitate evdev event, device path: $devicePath, type: $type, code: $code, value: $value")
            inputEventHub.injectEvdevEvent(devicePath, type, code, value)
        } else {
            Timber.w("Cannot imitate evdev event without system bridge connected. Device path: $devicePath, type: $type, code: $code, value: $value")
        }
    }
}

interface DetectKeyMapsUseCase {
    val allKeyMapList: Flow<List<DetectKeyMapModel>>
    val requestFingerprintGestureDetection: Flow<Boolean>
    val keyMapsToTriggerFromOtherApps: Flow<List<KeyMap>>
    val detectScreenOffTriggers: Flow<Boolean>

    val defaultLongPressDelay: Flow<Long>
    val defaultDoublePressDelay: Flow<Long>
    val defaultSequenceTriggerTimeout: Flow<Long>

    val forceVibrate: Flow<Boolean>
    val defaultVibrateDuration: Flow<Long>

    fun showTriggeredToast()
    fun vibrate(duration: Long)

    val currentTime: Long

    fun imitateKeyEvent(
        keyCode: Int,
        metaState: Int = 0,
        deviceId: Int = 0,
        action: Int,
        scanCode: Int = 0,
        source: Int = InputDevice.SOURCE_UNKNOWN,
    )

    fun imitateEvdevEvent(
        devicePath: String,
        type: Int,
        code: Int,
        value: Int,
    )
}
