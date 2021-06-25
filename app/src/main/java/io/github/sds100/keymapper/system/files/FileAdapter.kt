package io.github.sds100.keymapper.system.files

import io.github.sds100.keymapper.util.Result
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 13/04/2021.
 */
interface FileAdapter {
    fun openOutputStream(uriString: String): Result<OutputStream>
    fun openInputStream(uriString: String): Result<InputStream>
    fun openAsset(fileName: String): InputStream

    fun getPicturesFolder(): File
    fun getFileInfo(uri: String): Result<FileInfo>

    fun getUriForPrivateFile(path: String): Result<String>

    fun createPrivateFile(path: String): Result<OutputStream>
    fun readPrivateFile(path: String): Result<InputStream>
    fun writePrivateFile(path: String): Result<OutputStream>
    fun deletePrivateFile(path: String): Result<*>
    fun createPrivateDirectory(path: String): Result<*>
    fun deletePrivateDirectory(path: String): Result<*>

    /**
     * @return a list of paths to all the files and directories.
     */
    fun getFilesInPrivateDirectory(path: String): Result<Set<String>>

    fun createZipFile(destination: OutputStream, files: Set<String>): Result<*>
}