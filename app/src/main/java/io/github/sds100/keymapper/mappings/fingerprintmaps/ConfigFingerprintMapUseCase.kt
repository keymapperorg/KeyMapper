package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.constraints.ConstraintState
import io.github.sds100.keymapper.mappings.BaseConfigMappingUseCase
import io.github.sds100.keymapper.mappings.ConfigMappingUseCase
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData

/**
 * Created by sds100 on 16/02/2021.
 */
class ConfigFingerprintMapUseCaseImpl(
    private val repository: FingerprintMapRepository,
) : BaseConfigMappingUseCase<FingerprintMapAction, FingerprintMap>(),
    ConfigFingerprintMapUseCase {

    override fun setEnabled(enabled: Boolean) = editFingerprintMap { it.copy(isEnabled = enabled) }

    override fun createAction(data: ActionData): FingerprintMapAction =
        FingerprintMapAction(data = data)

    override fun setActionList(actionList: List<FingerprintMapAction>) {
        editFingerprintMap { it.copy(actionList = actionList) }
    }

    override fun setConstraintState(constraintState: ConstraintState) {
        editFingerprintMap { it.copy(constraintState = constraintState) }
    }

    override fun setVibrateEnabled(enabled: Boolean) =
        editFingerprintMap { it.copy(vibrate = enabled) }

    override fun setVibrationDuration(duration: Defaultable<Int>) =
        editFingerprintMap { it.copy(vibrateDuration = duration.nullIfDefault()) }

    override fun setShowToastEnabled(enabled: Boolean) {
        editFingerprintMap { it.copy(showToast = enabled) }
    }

    override fun setActionData(uid: String, data: ActionData) {
        editFingerprintMap { keyMap ->
            val newActionList = keyMap.actionList.map { action ->
                if (action.uid == uid) {
                    action.copy(data = data)
                } else {
                    action
                }
            }

            keyMap.copy(
                actionList = newActionList,
            )
        }
    }

    override fun setActionMultiplier(uid: String, multiplier: Int?) {
        setActionOption(uid) { it.copy(multiplier = multiplier) }
    }

    override fun setDelayBeforeNextAction(uid: String, delay: Int?) {
        setActionOption(uid) { it.copy(delayBeforeNextAction = delay) }
    }

    override fun setActionRepeatEnabled(uid: String, repeat: Boolean) =
        setActionOption(uid) { it.copy(repeat = repeat) }

    override fun setActionRepeatRate(uid: String, repeatRate: Int?) =
        setActionOption(uid) { it.copy(repeatRate = repeatRate) }

    override fun setActionRepeatLimit(uid: String, repeatLimit: Int?) =
        setActionOption(uid) { it.copy(repeatLimit = repeatLimit) }

    override fun setActionStopRepeatingWhenTriggerPressedAgain(uid: String) =
        setActionOption(uid) { it.copy(repeatMode = RepeatMode.TRIGGER_PRESSED_AGAIN) }

    override fun setActionStopRepeatingWhenLimitReached(uid: String) =
        setActionOption(uid) { it.copy(repeatMode = RepeatMode.LIMIT_REACHED) }

    override fun setActionHoldDownEnabled(uid: String, holdDown: Boolean) =
        setActionOption(uid) { it.copy(holdDownUntilSwipedAgain = holdDown) }

    override fun setActionHoldDownDuration(uid: String, holdDownDuration: Int?) =
        setActionOption(uid) { it.copy(holdDownDuration = holdDownDuration) }

    override suspend fun loadFingerprintMap(id: FingerprintGestureType) {
        val entity = repository.get(FingerprintMapIdEntityMapper.toEntity(id))
        val fingerprintMap = FingerprintMapEntityMapper.fromEntity(entity)

        mapping.value = State.Data(fingerprintMap)
    }

    override fun restoreState(fingerprintMap: FingerprintMap) {
        mapping.value = State.Data(fingerprintMap)
    }

    override fun save() {
        mapping.value.ifIsData { fingerprintMap ->
            val entity =
                FingerprintMapEntityMapper.toEntity(fingerprintMap)

            repository.update(entity)
        }
    }

    override fun getState(): State<FingerprintMap> = mapping.value

    private fun editFingerprintMap(block: (fingerprintMap: FingerprintMap) -> FingerprintMap) {
        mapping.value.ifIsData { mapping.value = State.Data(block.invoke(it)) }
    }

    private fun setActionOption(
        uid: String,
        block: (action: FingerprintMapAction) -> FingerprintMapAction,
    ) {
        editFingerprintMap { fingerprintMap ->
            val newActionList = fingerprintMap.actionList.map { action ->
                if (action.uid == uid) {
                    block.invoke(action)
                } else {
                    action
                }
            }

            fingerprintMap.copy(
                actionList = newActionList,
            )
        }
    }
}

interface ConfigFingerprintMapUseCase : ConfigMappingUseCase<FingerprintMapAction, FingerprintMap> {
    fun setVibrateEnabled(enabled: Boolean)
    fun setVibrationDuration(duration: Defaultable<Int>)
    fun setShowToastEnabled(enabled: Boolean)

    fun getState(): State<FingerprintMap>
    fun restoreState(fingerprintMap: FingerprintMap)
    suspend fun loadFingerprintMap(id: FingerprintGestureType)

    fun setActionRepeatEnabled(uid: String, repeat: Boolean)
    fun setActionHoldDownEnabled(uid: String, holdDown: Boolean)
    fun setActionHoldDownDuration(uid: String, holdDownDuration: Int?)
}
