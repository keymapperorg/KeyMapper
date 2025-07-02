package io.github.sds100.keymapper.system.files

import io.github.sds100.keymapper.common.utils.KMResult
import java.io.InputStream

interface FileAdapter {
    fun openAsset(fileName: String): InputStream

    fun getPicturesFolder(): String
    fun openDownloadsFile(fileName: String, mimeType: String): KMResult<IFile>

    fun getPrivateFile(path: String): IFile
    fun getFile(parent: IFile, path: String): IFile
    fun getFileFromUri(uri: String): IFile
    fun getPublicUriForPrivateFile(privateFile: IFile): String

    fun createZipFile(destination: IFile, files: Set<IFile>): KMResult<*>
    suspend fun extractZipFile(zipFile: IFile, destination: IFile): KMResult<*>
}
