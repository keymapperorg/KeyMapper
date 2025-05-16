package io.github.sds100.keymapper.system.files

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.callback.FileCallback
import com.anggrayudi.storage.file.MimeType
import com.anggrayudi.storage.file.copyFileTo
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.isRawFile
import com.anggrayudi.storage.file.openInputStream
import com.anggrayudi.storage.file.openOutputStream
import com.anggrayudi.storage.file.recreateFile
import com.anggrayudi.storage.media.FileDescription
import io.github.sds100.keymapper.common.result.Error
import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.common.result.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DocumentFileWrapper(val file: DocumentFile, context: Context) : IFile {

    private val ctx = context.applicationContext

    override val uri: String
        get() = file.uri.toString()

    override val path: String
        get() = file.getAbsolutePath(ctx)

    override val name: String?
        get() = file.name

    override val baseName: String?
        get() = file.name?.substringBeforeLast('.')

    override val extension: String?
        get() = file.name?.substringAfterLast('.')

    override val isDirectory: Boolean
        get() = file.isDirectory || toJavaFile().isDirectory

    override val isFile: Boolean
        get() = file.isFile || toJavaFile().isFile

    override fun listFiles(): List<IFile> = file.listFiles()
        .toList()
        .map { DocumentFileWrapper(it, ctx) }

    override fun inputStream(): InputStream? = file.openInputStream(ctx)

    override fun outputStream(): OutputStream? = file.openOutputStream(ctx, append = false)

    override fun clear() {
        file.recreateFile(ctx)
    }

    override fun delete() {
        file.delete()
    }

    override fun exists(): Boolean = file.exists()

    override fun createFile() {
        if (file.isRawFile) {
            val directories = this.path.substringBeforeLast('/')

            if (directories != this.path) {
                File(directories).mkdirs()
            }

            toJavaFile().createNewFile()
        } else {
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.extension)
                ?: MimeType.BINARY_FILE
            file.createFile(mimeType, baseName!!)
        }
    }

    override fun createDirectory() {
        if (file.isRawFile) {
            toJavaFile().mkdirs()
        } else {
            file.createDirectory(name!!)
        }
    }

    override suspend fun copyTo(directory: IFile, fileName: String?): Result<*> = withContext(Dispatchers.Default) {
        suspendCoroutine { continuation ->
            val callback = object : FileCallback() {
                override fun onCompleted(result: Any) {
                    super.onCompleted(result)

                    continuation.resume(Success(Unit))
                }

                override fun onFailed(errorCode: ErrorCode) {
                    super.onFailed(errorCode)

                    val error = when (errorCode) {
                        ErrorCode.STORAGE_PERMISSION_DENIED -> Error.StoragePermissionDenied
                        ErrorCode.CANNOT_CREATE_FILE_IN_TARGET -> Error.CannotCreateFileInTarget(
                            directory.uri,
                        )

                        ErrorCode.SOURCE_FILE_NOT_FOUND -> Error.SourceFileNotFound(this@DocumentFileWrapper.uri)
                        ErrorCode.TARGET_FILE_NOT_FOUND -> Error.TargetFileNotFound(directory.uri)
                        ErrorCode.TARGET_FOLDER_NOT_FOUND -> Error.TargetDirectoryNotFound(
                            directory.uri,
                        )

                        ErrorCode.UNKNOWN_IO_ERROR -> Error.UnknownIOError
                        ErrorCode.CANCELED -> Error.FileOperationCancelled
                        ErrorCode.TARGET_FOLDER_CANNOT_HAVE_SAME_PATH_WITH_SOURCE_FOLDER -> Error.TargetDirectoryMatchesSourceDirectory
                        ErrorCode.NO_SPACE_LEFT_ON_TARGET_PATH -> Error.NoSpaceLeftOnTarget(
                            directory.uri,
                        )
                    }

                    continuation.resume(error)
                }

                override fun onStart(file: Any, workerThread: Thread): Long = 0
            }

            val fileDescription = if (fileName == null) {
                null
            } else {
                FileDescription(fileName)
            }

            file.copyFileTo(ctx, directory.path, fileDescription, callback)
        }
    }
}
