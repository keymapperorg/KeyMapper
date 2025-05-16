package io.github.sds100.keymapper.actions.sound

import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.Success
import io.github.sds100.keymapper.system.files.FileAdapter
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by sds100 on 25/06/2021.
 */

class ChooseSoundFileUseCaseImpl(
    private val fileAdapter: FileAdapter,
    private val soundsManager: SoundsManager,
) : ChooseSoundFileUseCase {
    override val soundFiles = soundsManager.soundFiles

    override suspend fun saveSound(uri: String): Result<String> = soundsManager.saveNewSound(uri)

    override fun getSoundFileName(uri: String): Result<String> {
        val name = fileAdapter.getFileFromUri(uri).name

        return if (name == null) {
            Error.NoFileName
        } else {
            Success(name)
        }
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
