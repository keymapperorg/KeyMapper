package io.github.sds100.keymapper.base.actions.sound

import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.IFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundsManagerImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val fileAdapter: FileAdapter,
) : SoundsManager {
    companion object {
        private const val SOUNDS_DIR_NAME = "sounds"
    }

    override val soundFiles = MutableStateFlow<List<SoundFileInfo>>(emptyList())

    init {
        coroutineScope.launch {
            updateSoundFilesFlow()
        }
    }

    override suspend fun saveNewSound(uri: String): Result<String> {
        val uid = UUID.randomUUID().toString()

        val soundFile = fileAdapter.getFileFromUri(uri)
        val newFileName = createSoundCopyFileName(soundFile, uid)

        val soundsDir = fileAdapter.getPrivateFile(SOUNDS_DIR_NAME)
        soundsDir.createDirectory()

        return soundFile.copyTo(soundsDir, newFileName)
            .then { Success(uid) }
            .onSuccess { updateSoundFilesFlow() }
            .onFailure {
                if (it is Error.Exception) {
                    Timber.d(it.exception)
                }
            }
    }

    override suspend fun restoreSound(file: IFile): Result<*> {
        val soundsDir = fileAdapter.getPrivateFile(SOUNDS_DIR_NAME)
        soundsDir.createDirectory()

        // Don't copy it if the file already exists
        if (fileAdapter.getFile(soundsDir, file.name!!).exists()) {
            return Success(Unit)
        } else {
            val result = file.copyTo(soundsDir)
            updateSoundFilesFlow()

            return result
        }
    }

    override fun getSound(uid: String): Result<IFile> {
        val soundsDir = fileAdapter.getPrivateFile(SOUNDS_DIR_NAME)
        soundsDir.createDirectory()

        val matchingFile = soundsDir.listFiles()!!.find { it.name?.contains(uid) == true }

        if (matchingFile == null) {
            return Error.CantFindSoundFile
        } else {
            return Success(matchingFile)
        }
    }

    override fun deleteSound(uid: String): Result<*> = getSound(uid)
        .then { Success(it.delete()) }
        .onSuccess { updateSoundFilesFlow() }

    private fun getSoundFileInfo(fileName: String): SoundFileInfo {
        val name = fileName.substringBeforeLast('_')
        val extension = fileName.substringAfterLast('.')

        val uid = fileName
            .substringBeforeLast('.') // e.g "bla_32a3b1289"
            .substringAfterLast('_')

        return SoundFileInfo(uid, "$name.$extension")
    }

    private fun updateSoundFilesFlow() {
        val soundsDir = fileAdapter.getPrivateFile(SOUNDS_DIR_NAME)
        soundsDir.createDirectory()

        soundFiles.value = soundsDir
            .listFiles()!!
            .map { getSoundFileInfo(it.name!!) }
    }

    private fun createSoundCopyFileName(originalSoundFile: IFile, uid: String): String = buildString {
        append(originalSoundFile.baseName)
        append("_$uid")
        append(".${originalSoundFile.extension}")
    }
}

interface SoundsManager {
    val soundFiles: StateFlow<List<SoundFileInfo>>

    /**
     * @return the sound file uid
     */
    suspend fun saveNewSound(uri: String): Result<String>
    suspend fun restoreSound(file: IFile): Result<*>
    fun getSound(uid: String): Result<IFile>
    fun deleteSound(uid: String): Result<*>
}
