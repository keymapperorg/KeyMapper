package io.github.sds100.keymapper.actions.sound

import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileInfo
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.then
import java.io.InputStream
import java.util.*

/**
 * Created by sds100 on 24/06/2021.
 */

class SoundsManagerImpl(
    private val fileAdapter: FileAdapter,
) : SoundsManager {
    companion object {
        private const val SOUNDS_DIR_NAME = "sounds"
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
    }

    override fun getSoundUri(fileName: String): Result<String> {
        return fileAdapter.getUriForPrivateFile("$SOUNDS_DIR_NAME/$fileName")
    }

    private fun deleteSound(fileName: String): Result<*> {
        TODO("Not yet implemented")
    }

    override fun getSoundName(fileName: String): String {
        val nameWithUid = fileName.substringBeforeLast('.') // e.g "bla_32a3b1289"

        return nameWithUid.substringBeforeLast('_')
    }

    override fun getSound(fileName: String): Result<InputStream> {
        return fileAdapter.readPrivateFile("$SOUNDS_DIR_NAME/$fileName")
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
    /**
     * @return the file name of the file
     */
    suspend fun saveSound(uri: String): Result<String>
    fun getSoundUri(fileName: String): Result<String>
    fun getSound(fileName: String): Result<InputStream>
    fun getSoundName(fileName: String): String
}