package io.github.sds100.keymapper.actions.sound

import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileInfo
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.*

/**
 * Created by sds100 on 24/06/2021.
 */

class SoundsManagerImpl(
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

    override suspend fun saveSound(uri: String): Result<String> {
        return fileAdapter.openInputStream(uri) //open chosen sound file
            .then { inputStream ->

                fileAdapter.getFileInfo(uri) // get file extension for the sound file
                    .then { fileInfo -> // create a new file for the sound file to be copied to

                        val newFileName = createSoundCopyFileName(fileInfo)
                        fileAdapter.createPrivateFile("$SOUNDS_DIR_NAME/$newFileName")
                            .then { outputStream -> //copy the sound file to Key Mapper's sound file copy
                                try {
                                    inputStream.copyTo(outputStream)
                                    outputStream.close()

                                    Success(newFileName)

                                } catch (e: Exception) {
                                    Error.Exception(e)
                                } finally {
                                    inputStream.close()
                                    outputStream.close()
                                }
                            }
                    }
            }
            .onSuccess { updateSoundFilesFlow() }
    }

    override fun getSoundUri(uid: String): Result<String> {
        return getSoundFilePath(uid).then { path ->
            fileAdapter.getUriForPrivateFile(path)
        }
    }

    private fun deleteSound(fileName: String): Result<*> {
        updateSoundFilesFlow()
        TODO("Not yet implemented")
    }

    override fun getSound(uid: String): Result<InputStream> {
        return getSoundFilePath(uid).then { path ->
            fileAdapter.readPrivateFile(path)
        }
    }

    private fun getSoundFileInfo(fileName: String): SoundFileInfo {
        val name = fileName.substringBeforeLast('_')
        val extension = fileName.substringAfterLast('.')

        val uid = fileName
            .substringBeforeLast('.')// e.g "bla_32a3b1289"
            .substringAfterLast('_')

        return SoundFileInfo(uid, "$name.$extension")
    }

    private fun updateSoundFilesFlow() {
        fileAdapter.createPrivateDirectory(SOUNDS_DIR_NAME)

        soundFiles.value = fileAdapter
            .getFilesInPrivateDirectory(SOUNDS_DIR_NAME)
            .valueOrNull()!!
            .map { it.substringAfterLast('/') } //file name
            .map { getSoundFileInfo(it) }

    }

    private fun getSoundFilePath(uid: String): Result<String> {
        return fileAdapter.getFilesInPrivateDirectory(SOUNDS_DIR_NAME)
            .then { files ->
                val matchingFile = files.find { it.contains(uid) }

                if (matchingFile == null) {
                    return Error.CantFindSoundFile
                } else {
                    return Success(matchingFile)
                }
            }
    }

    private fun createSoundCopyFileName(originalSoundFileInfo: FileInfo): String {
        val uid = UUID.randomUUID().toString()
        return buildString {
            append(originalSoundFileInfo.name)
            append("_$uid")
            append(".${originalSoundFileInfo.extension}")
        }
    }
}

interface SoundsManager {
    val soundFiles: StateFlow<List<SoundFileInfo>>

    /**
     * @return the sound file uid
     */
    suspend fun saveSound(uri: String): Result<String>
    fun getSoundUri(uid: String): Result<String>
    fun getSound(uid: String): Result<InputStream>
}