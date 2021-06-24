package io.github.sds100.keymapper.system.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Created by sds100 on 13/04/2021.
 */
class AndroidFileAdapter(context: Context) : FileAdapter {
    private val ctx = context.applicationContext

    override fun openOutputStream(uriString: String): Result<OutputStream> {
        val uri = Uri.parse(uriString)

        return try {
            val outputStream = ctx.contentResolver.openOutputStream(uri)!!

            Success(outputStream)
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> Error.FileAccessDenied
                else -> Error.Exception(e)
            }
        }
    }

    override fun openInputStream(uriString: String): Result<InputStream> {
        val uri = Uri.parse(uriString)

        return try {
            val inputStream = ctx.contentResolver.openInputStream(uri)!!

            Success(inputStream)
        } catch (e: Exception) {
            when (e) {
                is SecurityException -> Error.FileAccessDenied
                else -> Error.Exception(e)
            }
        }
    }

    override fun getFileInfo(uri: String): Result<FileInfo> {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        ctx.contentResolver.query(Uri.parse(uri), projection, null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()

                val fileInfo = FileInfo(
                    nameWithExtension = cursor.getString(nameIndex)
                )

                return Success(fileInfo)
            }

        return Error.FileNotFound(uri)
    }

    /**
     * @param [name] The path with file name and extension.
     */
    override fun getPrivateFile(name: String): Result<File> {
        try {
            val filesDir = ctx.filesDir

            val directory = name.substringBeforeLast('/')
            val fileName = name.substringAfterLast('/')

            val directoryFile = File(filesDir, directory)
            directoryFile.mkdirs()

            val file = File(directoryFile, fileName)
            return Success(file)

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    /**
     * @param [name] The path with file name and extension.
     */
    override fun getPrivateDirectory(name: String): Result<File> {
        try {
            val directory = File(ctx.filesDir, name)

            if (directory.exists() && !directory.isDirectory) {
                return Error.NotADirectory
            }

            directory.mkdirs()

            return Success(directory)

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun openAsset(fileName: String): InputStream {
        return ctx.assets.open(fileName)
    }

    override fun getPicturesFolder(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    }

    override fun createZipFile(destinationFile: File, files: List<File>): Result<*> {
        try {
            ZipFile(destinationFile).apply {
                files.forEach { file ->
                    if (file.isDirectory) {
                        addFolder(file)
                    } else {
                        addFile(file)
                    }
                }
            }

            return Success(Unit)
        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }
}