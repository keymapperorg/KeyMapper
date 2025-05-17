package io.github.sds100.keymapper.system.files

import io.github.sds100.keymapper.common.util.result.Result
import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface IFile {
    val uri: String
    val path: String
    val name: String?
    val extension: String?
    val baseName: String?
    val isFile: Boolean
    val isDirectory: Boolean
    fun inputStream(): InputStream?
    fun outputStream(): OutputStream?
    fun listFiles(): List<IFile>?
    fun clear()
    fun delete()
    fun exists(): Boolean

    fun createFile()
    fun createDirectory()

    suspend fun copyTo(directory: IFile, fileName: String? = null): Result<*>
}

fun IFile.toJavaFile(): File = File(path)
