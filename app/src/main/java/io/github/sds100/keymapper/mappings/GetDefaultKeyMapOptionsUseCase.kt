package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GetDefaultKeyMapOptionsUseCaseImpl(
    coroutineScope: CoroutineScope,
    preferenceRepository: PreferenceRepository,
) : GetDefaultKeyMapOptionsUseCase {

    override val defaultRepeatDelay: StateFlow<Int> =
        preferenceRepository.get(Keys.defaultRepeatDelay)
            .map { it ?: PreferenceDefaults.REPEAT_DELAY }
            .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.REPEAT_DELAY)

    override val defaultRepeatRate: StateFlow<Int> =
        preferenceRepository.get(Keys.defaultRepeatRate)
            .map { it ?: PreferenceDefaults.REPEAT_RATE }
            .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.REPEAT_DELAY)

    override val defaultHoldDownDuration: StateFlow<Int> =
        preferenceRepository.get(Keys.defaultHoldDownDuration)
            .map { it ?: PreferenceDefaults.HOLD_DOWN_DURATION }
            .stateIn(coroutineScope, SharingStarted.Lazily, PreferenceDefaults.REPEAT_DELAY)
}

interface GetDefaultKeyMapOptionsUseCase {

    val defaultHoldDownDuration: StateFlow<Int>
    val defaultRepeatDelay: StateFlow<Int>
    val defaultRepeatRate: StateFlow<Int>
}
