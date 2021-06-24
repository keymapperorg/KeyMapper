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

    fun getPrivateFile(name: String): Result<File>
    fun getPrivateDirectory(name: String): Result<File>

    fun createZipFile(destinationFile: File, files: List<File>): Result<*>
}