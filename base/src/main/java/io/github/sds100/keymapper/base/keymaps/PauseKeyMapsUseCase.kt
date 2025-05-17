package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.ringtone.RingtoneAdapter
import io.github.sds100.keymapper.base.utils.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class PauseKeyMapsUseCaseImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val preferenceRepository: PreferenceRepository,
    private val mediaAdapter: MediaAdapter,
    private val ringtoneAdapter: RingtoneAdapter
) : PauseKeyMapsUseCase {

    override val isPaused: Flow<Boolean> =
        preferenceRepository.get(Keys.mappingsPaused).map { it ?: false }

    override fun pause() {
        preferenceRepository.set(Keys.mappingsPaused, true)
        mediaAdapter.stopFileMedia()
        ringtoneAdapter.stopPlaying()
        Timber.d("Pause mappings")
    }

    override fun resume() {
        preferenceRepository.set(Keys.mappingsPaused, false)
        Timber.d("Resume mappings")
    }
}

interface PauseKeyMapsUseCase {
    val isPaused: Flow<Boolean>
    fun pause()
    fun resume()
}
