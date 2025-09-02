package io.github.sds100.keymapper.system.files

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.anggrayudi.storage.file.toDocumentFile
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.InputStream
import java.util.UUID
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidFileAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildConfigProvider: BuildConfigProvider
) : FileAdapter {
    private val ctx = context.applicationContext
    private val contentResolver: ContentResolver = ctx.contentResolver

    override fun getPrivateFile(path: String): IFile {
        val file = File(ctx.filesDir, path)

        val documentFile = DocumentFile.fromFile(file)

        return DocumentFileWrapper(documentFile, ctx)
    }

    override fun getFile(parent: IFile, path: String): IFile {
        val documentFile = DocumentFile.fromFile(File(parent.toJavaFile(), path))

        return DocumentFileWrapper(documentFile, ctx)
    }

    override fun getFileFromUri(uri: String): IFile {
        val documentFile = DocumentFile.fromSingleUri(ctx, uri.toUri())!!

        return DocumentFileWrapper(documentFile, ctx)
    }

    override fun openAsset(fileName: String): InputStream = ctx.assets.open(fileName)

    override fun openDownloadsFile(fileName: String, mimeType: String): KMResult<IFile> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.IS_DOWNLOAD, 1)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri =
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return KMError.UnknownIOError

            val file = DocumentFile.fromSingleUri(ctx, uri) ?: return KMError.UnknownIOError

            return Success(DocumentFileWrapper(file, ctx))
        } else {
            val downloadsFile =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val documentFile = downloadsFile.toDocumentFile(ctx) ?: return KMError.UnknownIOError

            val file = DocumentFileWrapper(documentFile, ctx)
            return Success(file)
        }
    }

    override fun getPicturesFolder(): String = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path

    override fun createZipFile(destination: IFile, files: Set<IFile>): KMResult<*> {
        val zipUid = UUID.randomUUID().toString()

        val tempZipFile = File(ctx.filesDir, "$zipUid.zip")

        try {
            ZipFile(tempZipFile).apply {
                files.forEach { file ->
                    if (file.isDirectory) {
                        addFolder(file.toJavaFile())
                    } else {
                        val javaFile = file.toJavaFile()
                        addFile(javaFile)
                    }
                }
            }

            tempZipFile.inputStream().use { input ->
                destination.outputStream()!!.use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            tempZipFile.delete()
        }

        return Success(Unit)
    }

    override suspend fun extractZipFile(zipFile: IFile, destination: IFile): KMResult<*> {
        withContext(Dispatchers.IO) {
            val zipUid = UUID.randomUUID().toString()
            val tempZipFileCopy = File(ctx.filesDir, "$zipUid.zip")

            try {
                zipFile.inputStream().use { input ->
                    tempZipFileCopy.outputStream().use { output ->
                        input?.copyTo(output)
                    }
                }

                ZipFile(tempZipFileCopy).extractAll(destination.path)
            } finally {
                tempZipFileCopy.delete()
            }
        }

        return Success(Unit)
    }

    override fun getPublicUriForPrivateFile(privateFile: IFile): String {
        return FileProvider.getUriForFile(
            ctx,
            "${buildConfigProvider.packageName}.provider",
            privateFile.toJavaFile(),
        ).toString()
    }
}
