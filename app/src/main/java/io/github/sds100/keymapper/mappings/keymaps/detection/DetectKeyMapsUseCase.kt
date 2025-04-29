package io.github.sds100.keymapper.mappings.keymaps.detection

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.KeyEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.FloatingButtonRepository
import io.github.sds100.keymapper.data.repositories.GroupRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.groups.Group
import io.github.sds100.keymapper.groups.GroupEntityMapper
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.KeyMapEntityMapper
import io.github.sds100.keymapper.mappings.keymaps.KeyMapRepository
import io.github.sds100.keymapper.mappings.keymaps.trigger.FingerprintTriggerKey
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.inputevents.InputEventInjector
import io.github.sds100.keymapper.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.navigation.OpenMenuHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.vibrator.VibratorAdapter
import io.github.sds100.keymapper.system.volume.VolumeAdapter
import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Created by sds100 on 17/04/2021.
 */

class DetectKeyMapsUseCaseImpl(
    private val keyMapRepository: KeyMapRepository,
    private val floatingButtonRepository: FloatingButtonRepository,
    private val groupRepository: GroupRepository,
    private val preferenceRepository: PreferenceRepository,
    private val suAdapter: SuAdapter,
    private val displayAdapter: DisplayAdapter,
    private val volumeAdapter: VolumeAdapter,
    private val imeInputEventInjector: ImeInputEventInjector,
    private val accessibilityService: IAccessibilityService,
    private val shizukuInputEventInjector: InputEventInjector,
    private val popupMessageAdapter: PopupMessageAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val resourceProvider: ResourceProvider,
    private val vibrator: VibratorAdapter,
    private val coroutineScope: CoroutineScope,
) : DetectKeyMapsUseCase {

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
            suAdapter.isGranted,
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
        suAdapter,
        accessibilityService,
        shizukuInputEventInjector,
        permissionAdapter,
        coroutineScope,
    )

    override val forceVibrate: Flow<Boolean> =
        preferenceRepository.get(Keys.forceVibrate).map { it == true }

    override val defaultVibrateDuration: Flow<Long> =
        preferenceRepository.get(Keys.defaultVibrateDuration)
            .map { it ?: PreferenceDefaults.VIBRATION_DURATION }
            .map { it.toLong() }

    override fun showTriggeredToast() {
        popupMessageAdapter.showPopupMessage(resourceProvider.getString(R.string.toast_triggered_keymap))
    }

    override fun vibrate(duration: Long) {
        vibrator.vibrate(duration)
    }

    override fun imitateButtonPress(
        keyCode: Int,
        metaState: Int,
        deviceId: Int,
        inputEventType: InputEventType,
        scanCode: Int,
    ) {
        val model = InputKeyModel(
            keyCode,
            inputEventType,
            metaState,
            deviceId,
            scanCode,
        )

        if (permissionAdapter.isGranted(Permission.SHIZUKU)) {
            Timber.d("Imitate button press ${KeyEvent.keyCodeToString(keyCode)} with Shizuku, key code: $keyCode, device id: $deviceId, meta state: $metaState, scan code: $scanCode")

            coroutineScope.launch {
                shizukuInputEventInjector.inputKeyEvent(model)
            }
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

                else -> runBlocking {
                    imeInputEventInjector.inputKeyEvent(model)
                }
            }
        }
    }

    override val isScreenOn: Flow<Boolean> = displayAdapter.isScreenOn
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

    fun imitateButtonPress(
        keyCode: Int,
        metaState: Int = 0,
        deviceId: Int = 0,
        inputEventType: InputEventType = InputEventType.DOWN_UP,
        scanCode: Int = 0,
    )

    val isScreenOn: Flow<Boolean>
}
