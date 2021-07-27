package io.github.sds100.keymapper.actions.sound

import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 25/06/2021.
 */

class ChooseSoundFileUseCaseImpl(
    private val fileAdapter: FileAdapter,
    private val soundsManager: SoundsManager
) : ChooseSoundFileUseCase {
    override val soundFiles = soundsManager.soundFiles

    override suspend fun saveSound(uri: String): Result<String> {
        return soundsManager.saveNewSound(uri)
    }

    override fun getSoundFileName(uri: String): Result<String> {
        return Success(fileAdapter.getFileFromUri(uri).name!!)
    }
}

interface ChooseSoundFileUseCase {

    /**
     * @return the sound file uid
     */
    suspend fun saveSound(uri: String): Result<String>
    val soundFiles: StateFlow<List<SoundFileInfo>>
    fun getSoundFileName(uri: String): Result<String>
}