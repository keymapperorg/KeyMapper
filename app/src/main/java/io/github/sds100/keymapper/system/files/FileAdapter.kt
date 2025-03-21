package io.github.sds100.keymapper.system.files

import io.github.sds100.keymapper.util.Result
import java.io.InputStream

/**
 * Created by sds100 on 13/04/2021.
 */
interface FileAdapter {
    fun openAsset(fileName: String): InputStream

    fun getPicturesFolder(): String
    fun openDownloadsFile(fileName: String, mimeType: String): Result<IFile>

    fun getPrivateFile(path: String): IFile
    fun getFile(parent: IFile, path: String): IFile
    fun getFileFromUri(uri: String): IFile
    fun getPublicUriForPrivateFile(privateFile: IFile): String

    fun createZipFile(destination: IFile, files: Set<IFile>): Result<*>
    suspend fun extractZipFile(zipFile: IFile, destination: IFile): Result<*>
}
