package io.github.sds100.keymapper.base.reroutekeyevents

import android.os.Build
import io.github.sds100.keymapper.base.input.InjectKeyEventModel
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.base.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is used for the feature created in issue #618 to fix the device IDs of key events
 * on Android 11. There was a bug in the system where enabling an accessibility service
 * would reset the device ID of key events to -1.
 */
@Singleton
class RerouteKeyEventsUseCaseImpl @Inject constructor(
    // MUST use the input event injector instead of the InputManager to bypass the buggy code in Android.
    private val imeInputEventInjector: ImeInputEventInjector,
    private val inputMethodAdapter: InputMethodAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val buildConfigProvider: BuildConfigProvider,
) : RerouteKeyEventsUseCase {

    override val isReroutingEnabled: Flow<Boolean> =
        preferenceRepository.get(Keys.rerouteKeyEvents).map { it ?: false }

    private val devicesToRerouteKeyEvents =
        preferenceRepository.get(Keys.devicesToRerouteKeyEvents).map { it ?: emptyList() }

    private val imeHelper by lazy {
        KeyMapperImeHelper(
            inputMethodAdapter,
            buildConfigProvider.packageName,
        )
    }

    override fun shouldRerouteKeyEvent(descriptor: String?): Boolean {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.R) {
            return false
        }

        return isReroutingEnabled.firstBlocking() &&
            imeHelper.isCompatibleImeChosen() &&
            (descriptor != null && devicesToRerouteKeyEvents.firstBlocking().contains(descriptor))
    }

    override fun inputKeyEvent(keyEvent: InjectKeyEventModel) {
        imeInputEventInjector.inputKeyEvent(keyEvent)
    }
}

interface RerouteKeyEventsUseCase {
    val isReroutingEnabled: Flow<Boolean>
    fun shouldRerouteKeyEvent(descriptor: String?): Boolean
    fun inputKeyEvent(keyEvent: InjectKeyEventModel)
}
