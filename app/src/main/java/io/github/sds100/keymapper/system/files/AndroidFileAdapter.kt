package io.github.sds100.keymapper.system.files

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Created by sds100 on 13/04/2021.
 */
class AndroidFileAdapter(context: Context) : FileAdapter {
    private val ctx = context.applicationContext

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
        val documentFile = DocumentFile.fromSingleUri(ctx, Uri.parse(uri))!!

        return DocumentFileWrapper(documentFile, ctx)
    }

    override fun openAsset(fileName: String): InputStream {
        return ctx.assets.open(fileName)
    }

    override fun getPicturesFolder(): String {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
    }

    override fun createZipFile(destination: IFile, files: Set<IFile>): Result<*> {
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

    override suspend fun extractZipFile(zipFile: IFile, destination: IFile): Result<*> {
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
}