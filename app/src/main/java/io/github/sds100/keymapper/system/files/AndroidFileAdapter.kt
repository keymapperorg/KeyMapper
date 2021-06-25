package io.github.sds100.keymapper.system.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.net.toUri
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import net.lingala.zip4j.io.outputstream.ZipOutputStream
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionMethod
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

    override fun createPrivateFile(path: String): Result<OutputStream> {
        try {
            val filesDir = ctx.filesDir

            val directories = path.substringBeforeLast('/')

            if (directories.isNotBlank()) {
                File(filesDir, directories).apply {
                    mkdirs()
                }
            }

            val file = File(filesDir, path)
            file.createNewFile()
            return Success(file.outputStream())

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun readPrivateFile(path: String): Result<InputStream> {
        try {
            val file = File(ctx.filesDir, path)

            if (!file.exists()) {
                return Error.FileNotFound(path)
            }

            if (file.isDirectory) {
                return Error.NotAFile
            }

            return Success(file.inputStream())

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun deletePrivateFile(path: String): Result<*> {
        try {
            val filesDir = ctx.filesDir
            val file = File(filesDir, path)
            file.deleteRecursively()

            return Success(Unit)

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun writePrivateFile(path: String): Result<OutputStream> {
        try {
            val file = File(ctx.filesDir, path)

            if (!file.exists()) {
                return Error.FileNotFound(path)
            }

            return Success(file.outputStream())

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun createPrivateDirectory(path: String): Result<*> {
        try {
            val directory = File(ctx.filesDir, path)
            directory.mkdirs()
            return Success(directory)

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun deletePrivateDirectory(path: String): Result<*> {
        try {
            val directory = File(ctx.filesDir, path)
            directory.deleteRecursively()
            return Success(directory)

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    override fun getUriForPrivateFile(path: String): Result<String> {
        return Success(File(ctx.filesDir, path).toUri().toString())
    }

    override fun openAsset(fileName: String): InputStream {
        return ctx.assets.open(fileName)
    }

    override fun getPicturesFolder(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    }

    /**
     * @param [files] A list of paths to the files/directories that should be zipped. They must be in private storage.
     */
    override fun createZipFile(destination: OutputStream, files: Set<String>): Result<*> {
        try {

            val zipParameters = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
            }

            ZipOutputStream(destination).use { zipOutput ->
                files
                    .map { File(ctx.filesDir, it) }
                    .toTypedArray()
                    .let { recursivelyAddFilesToZip(it, zipParameters, zipOutput) }
            }

            return Success(Unit)

        } catch (e: Exception) {
            return Error.Exception(e)
        }
    }

    /**
     * @return the paths of the file relative to Key Mapper's private data folder.
     */
    override fun getFilesInPrivateDirectory(path: String): Result<Set<String>> {
        val files = File(ctx.filesDir, path).listFiles()
        val privateDirPath = ctx.filesDir.path

        if (files == null) {
            return Error.NotADirectory
        } else {
            return Success(files.map { it.path.substringAfter("$privateDirPath/") }.toSet())
        }
    }

    private fun recursivelyAddFilesToZip(files: Array<File>, zipParameters: ZipParameters, zipOutput: ZipOutputStream) {
        files.forEach { file ->
            if (file.isDirectory) {
                recursivelyAddFilesToZip(file.listFiles() ?: emptyArray(), zipParameters, zipOutput)
            } else {
                zipParameters.fileNameInZip = file.path
                zipOutput.putNextEntry(zipParameters)

                file.inputStream().use { it.copyTo(zipOutput) }
                zipOutput.closeEntry()
            }
        }
    }
}