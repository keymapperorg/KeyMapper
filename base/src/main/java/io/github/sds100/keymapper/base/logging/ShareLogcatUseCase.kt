package io.github.sds100.keymapper.base.logging

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.utils.ShareUtils
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.files.IFile
import io.github.sds100.keymapper.system.shell.ShellAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@ViewModelScoped
class ShareLogcatUseCaseImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val fileAdapter: FileAdapter,
    private val shellAdapter: ShellAdapter,
    private val buildConfigProvider: BuildConfigProvider,
) : ShareLogcatUseCase {

    override suspend fun share(): KMResult<Unit> {
        val fileName = "logs/logcat_${FileUtils.createFileDate()}.txt"

        return withContext(Dispatchers.IO) {
            val file: IFile = fileAdapter.getPrivateFile(fileName)
            file.createFile()

            val command = "logcat -d -f ${file.path}"

            shellAdapter.execute(command).then {
                val publicUri = fileAdapter.getPublicUriForPrivateFile(file)

                ShareUtils.shareFile(ctx, publicUri.toUri(), buildConfigProvider.packageName)
                Success(Unit)
            }
        }
    }
}

interface ShareLogcatUseCase {
    suspend fun share(): KMResult<Unit>
}
