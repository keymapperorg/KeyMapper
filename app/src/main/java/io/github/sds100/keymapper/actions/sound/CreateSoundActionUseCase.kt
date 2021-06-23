package io.github.sds100.keymapper.actions.sound

import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.then
import java.util.*

/**
 * Created by sds100 on 23/06/2021.
 */

class CreateSoundActionUseCaseImpl(
    private val fileAdapter: FileAdapter
) : CreateSoundActionUseCase {

    override suspend fun storeSoundFile(uri: String): Result<String> {
        val uid = UUID.randomUUID().toString()

        return fileAdapter.openInputStream(uri).then { inputStream ->
            fileAdapter
                .getFileInfo(uri)
                .then { fileAdapter.createPrivateFile("sounds/$uid.${it.extension}") }
                .then { outputStream ->
                    try {
                        inputStream.copyTo(outputStream)
                        Success(uid)

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

interface CreateSoundActionUseCase {
    /**
     * Store the sound file somewhere safe so that it won't be deleted by the user.
     *
     * @return the UID of the stored sound file.
     */
    suspend fun storeSoundFile(uri: String): Result<String>
}