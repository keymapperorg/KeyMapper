package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetDefaultKeyMapOptionsUseCaseImpl
    @Inject
    constructor(
        coroutineScope: CoroutineScope,
        preferenceRepository: PreferenceRepository,
    ) : GetDefaultKeyMapOptionsUseCase {
        override val defaultRepeatDelay: StateFlow<Int> =
            preferenceRepository
                .get(Keys.defaultRepeatDelay)
                .map { it ?: PreferenceDefaults.REPEAT_DELAY }
                .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.REPEAT_DELAY)

        override val defaultRepeatRate: StateFlow<Int> =
            preferenceRepository
                .get(Keys.defaultRepeatRate)
                .map { it ?: PreferenceDefaults.REPEAT_RATE }
                .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.REPEAT_RATE)

        override val defaultHoldDownDuration: StateFlow<Int> =
            preferenceRepository
                .get(Keys.defaultHoldDownDuration)
                .map { it ?: PreferenceDefaults.HOLD_DOWN_DURATION }
                .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.HOLD_DOWN_DURATION)

        override val defaultLongPressDelay: StateFlow<Int> =
            preferenceRepository
                .get(Keys.defaultLongPressDelay)
                .map { it ?: PreferenceDefaults.LONG_PRESS_DELAY }
                .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.LONG_PRESS_DELAY)

        override val defaultDoublePressDelay: StateFlow<Int> =
            preferenceRepository
                .get(Keys.defaultDoublePressDelay)
                .map { it ?: PreferenceDefaults.DOUBLE_PRESS_DELAY }
                .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.DOUBLE_PRESS_DELAY)

        override val defaultSequenceTriggerTimeout: StateFlow<Int> =
            preferenceRepository
                .get(Keys.defaultSequenceTriggerTimeout)
                .map { it ?: PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT }
                .stateIn(
                    coroutineScope,
                    SharingStarted.Lazily,
                    PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT,
                )

        override val defaultVibrateDuration: StateFlow<Int> =
            preferenceRepository
                .get(Keys.defaultVibrateDuration)
                .map { it ?: PreferenceDefaults.VIBRATION_DURATION }
                .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.VIBRATION_DURATION)
    }

interface GetDefaultKeyMapOptionsUseCase {
    val defaultHoldDownDuration: StateFlow<Int>
    val defaultRepeatDelay: StateFlow<Int>
    val defaultRepeatRate: StateFlow<Int>
    val defaultLongPressDelay: StateFlow<Int>
    val defaultDoublePressDelay: StateFlow<Int>
    val defaultSequenceTriggerTimeout: StateFlow<Int>
    val defaultVibrateDuration: StateFlow<Int>
}
