package io.github.sds100.keymapper.base.system.files

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.IFile
import kotlinx.coroutines.runBlocking
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.InputStream

class FakeFileAdapter(
    private val tempFolder: TemporaryFolder,
) : FileAdapter {
    val privateFolder = tempFolder.newFolder("private")

    override fun openAsset(fileName: String): InputStream = throw Exception()

    override fun getPicturesFolder(): String = throw Exception()

    override fun openDownloadsFile(
        fileName: String,
        mimeType: String,
    ): KMResult<IFile> = throw Exception()

    override fun getPrivateFile(path: String): IFile {
        val file = File(privateFolder, path)

        return JavaFile(file)
    }

    override fun getFile(
        parent: IFile,
        path: String,
    ): IFile = JavaFile(File((parent as JavaFile).file, path))

    override fun getFileFromUri(uri: String): IFile = JavaFile(File(uri))

    override fun getPublicUriForPrivateFile(privateFile: IFile): String = ""

    override fun createZipFile(
        destination: IFile,
        files: Set<IFile>,
    ): KMResult<*> {
        runBlocking {
            files.forEach { file ->
                file.copyTo(destination)
            }
        }

        return Success(Unit)
    }

    override suspend fun extractZipFile(
        zipFile: IFile,
        destination: IFile,
    ): KMResult<*> {
        zipFile.listFiles()!!.forEach { file ->
            file.copyTo(destination)
        }

        return Success(Unit)
    }
}
