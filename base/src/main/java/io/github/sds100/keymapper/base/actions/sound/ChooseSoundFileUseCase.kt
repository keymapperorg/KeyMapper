package io.github.sds100.keymapper.base.actions.sound

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.files.FileAdapter
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ChooseSoundFileUseCaseImpl @Inject constructor(
    private val fileAdapter: FileAdapter,
    private val soundsManager: SoundsManager,
) : ChooseSoundFileUseCase {
    override val soundFiles = soundsManager.soundFiles

    override suspend fun saveSound(uri: String): KMResult<String> = soundsManager.saveNewSound(uri)

    override fun getSoundFileName(uri: String): KMResult<String> {
        val name = fileAdapter.getFileFromUri(uri).name

        return if (name == null) {
            KMError.NoFileName
        } else {
            Success(name)
        }
    }
}

interface ChooseSoundFileUseCase {

    /**
     * @return the sound file uid
     */
    suspend fun saveSound(uri: String): KMResult<String>
    val soundFiles: StateFlow<List<SoundFileInfo>>
    fun getSoundFileName(uri: String): KMResult<String>
}
