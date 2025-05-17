package io.github.sds100.keymapper.system.files

import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.Success
import kotlinx.coroutines.runBlocking
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.InputStream

class FakeFileAdapter(
    private val tempFolder: TemporaryFolder,
) : FileAdapter {

    val privateFolder = tempFolder.newFolder("private")

    override fun openAsset(fileName: String): InputStream {
        throw Exception()
    }

    override fun getPicturesFolder(): String {
        throw Exception()
    }

    override fun openDownloadsFile(fileName: String, mimeType: String): Result<IFile> {
        throw Exception()
    }

    override fun getPrivateFile(path: String): IFile {
        val file = File(privateFolder, path)

        return JavaFile(file)
    }

    override fun getFile(parent: IFile, path: String): IFile {
        return JavaFile(File((parent as JavaFile).file, path))
    }

    override fun getFileFromUri(uri: String): IFile {
        return JavaFile(File(uri))
    }

    override fun getPublicUriForPrivateFile(privateFile: IFile): String {
        return ""
    }

    override fun createZipFile(destination: IFile, files: Set<IFile>): Result<*> {
        runBlocking {
            files.forEach { file ->
                file.copyTo(destination)
            }
        }

        return Success(Unit)
    }

    override suspend fun extractZipFile(zipFile: IFile, destination: IFile): Result<*> {
        zipFile.listFiles()!!.forEach { file ->
            file.copyTo(destination)
        }

        return Success(Unit)
    }
}
