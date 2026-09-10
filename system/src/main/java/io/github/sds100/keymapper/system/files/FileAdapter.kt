package io.github.sds100.keymapper.system.files

import io.github.sds100.keymapper.common.utils.KMResult
import java.io.InputStream

interface FileAdapter {
    fun openAsset(fileName: String): InputStream

    fun getPicturesFolder(): String
    fun openDownloadsFile(fileName: String, mimeType: String): KMResult<IFile>

    /**
     * Lists the files currently in the Downloads folder. If MANAGE_EXTERNAL_STORAGE
     * is granted, this lists everything in the public Downloads directory. Otherwise it
     * only lists files this app itself previously wrote there via [openDownloadsFile],
     * which needs no permission.
     */
    fun getDownloads(): KMResult<List<IFile>>

    fun getPrivateFile(path: String): IFile
    fun getFile(parent: IFile, path: String): IFile
    fun getFileFromUri(uri: String): IFile
    fun getPublicUriForPrivateFile(privateFile: IFile): String

    fun createZipFile(destination: IFile, files: Set<IFile>): KMResult<*>
    suspend fun extractZipFile(zipFile: IFile, destination: IFile): KMResult<*>
}
