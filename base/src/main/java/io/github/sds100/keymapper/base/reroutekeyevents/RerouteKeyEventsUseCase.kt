package io.github.sds100.keymapper.base.reroutekeyevents

import android.os.Build
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.base.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * This is used for the feature created in issue #618 to fix the device IDs of key events
 * on Android 11. There was a bug in the system where enabling an accessibility service
 * would reset the device ID of key events to -1.
 */
class RerouteKeyEventsUseCaseImpl @AssistedInject constructor(
    @Assisted
    private val keyMapperImeMessenger: ImeInputEventInjector,
    private val inputMethodAdapter: InputMethodAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val buildConfigProvider: BuildConfigProvider,
) : RerouteKeyEventsUseCase {

    @AssistedFactory
    interface Factory {
        fun create(
            keyMapperImeMessenger: ImeInputEventInjector,
        ): RerouteKeyEventsUseCaseImpl
    }

    private val rerouteKeyEvents =
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

        return rerouteKeyEvents.firstBlocking() &&
            imeHelper.isCompatibleImeChosen() &&
            (
                descriptor != null &&
                    devicesToRerouteKeyEvents.firstBlocking()
                        .contains(descriptor)
                )
    }

    override fun inputKeyEvent(keyModel: InputKeyModel) {
        // It is safe to run the ime injector on the main thread.
        runBlocking {
            keyMapperImeMessenger.inputKeyEvent(keyModel)
        }
    }
}

interface RerouteKeyEventsUseCase {
    fun shouldRerouteKeyEvent(descriptor: String?): Boolean
    fun inputKeyEvent(keyModel: InputKeyModel)
}
