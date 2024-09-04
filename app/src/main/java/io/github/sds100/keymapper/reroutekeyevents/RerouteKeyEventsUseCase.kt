package io.github.sds100.keymapper.reroutekeyevents

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeMessenger
import io.github.sds100.keymapper.util.firstBlocking
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 27/04/2021.
 */

/**
 * This is used for the feature created in issue #618 to fix the device IDs of key events
 * on Android 11. There was a bug in the system where enabling an accessibility service
 * would reset the device ID of key events to -1.
 */
class RerouteKeyEventsUseCaseImpl(
    private val inputMethodAdapter: InputMethodAdapter,
    private val keyMapperImeMessenger: KeyMapperImeMessenger,
    private val preferenceRepository: PreferenceRepository,
) : RerouteKeyEventsUseCase {

    private val rerouteKeyEvents =
        preferenceRepository.get(Keys.rerouteKeyEvents).map { it ?: false }

    private val devicesToRerouteKeyEvents =
        preferenceRepository.get(Keys.devicesToRerouteKeyEvents).map { it ?: emptyList() }

    private val imeHelper by lazy { KeyMapperImeHelper(inputMethodAdapter) }

    override fun shouldRerouteKeyEvent(descriptor: String): Boolean =
        imeHelper.isCompatibleImeChosen() &&
            devicesToRerouteKeyEvents.firstBlocking().contains(descriptor) &&
            rerouteKeyEvents.firstBlocking()

    override fun inputKeyEvent(keyModel: InputKeyModel) {
        keyMapperImeMessenger.inputKeyEvent(keyModel)
    }
}

interface RerouteKeyEventsUseCase {
    fun shouldRerouteKeyEvent(descriptor: String): Boolean
    fun inputKeyEvent(keyModel: InputKeyModel)
}
