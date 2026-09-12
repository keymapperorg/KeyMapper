package io.github.sds100.keymapper.base.backup

import android.os.Build
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.files.IFile
import io.github.sds100.keymapper.system.leanback.LeanbackAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

sealed class ExportedBackupLocation {
    data class PublicUri(val uri: String) : ExportedBackupLocation()
    data class Downloads(val fileName: String) : ExportedBackupLocation()
}

class BackupRestoreMappingsUseCaseImpl @Inject constructor(
    private val fileAdapter: FileAdapter,
    private val backupManager: BackupManager,
    private val leanbackAdapter: LeanbackAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    private val permissionAdapter: PermissionAdapter,
) : BackupRestoreMappingsUseCase {

    override val onAutomaticBackupResult: Flow<KMResult<*>> = backupManager.onAutomaticBackupResult

    override suspend fun backupEverything(): KMResult<ExportedBackupLocation> {
        val fileName = BackupUtils.createBackupFileName()

        // Share in private files so the share sheet can show the file name. This is some quirk
        // of the storage access framework https://issuetracker.google.com/issues/268079113.
        // Saving it directly to Downloads with the MediaStore returns a content URI
        // that only contains a numerical ID, not the file name.
        val file = fileAdapter.getPrivateFile("${BackupManagerImpl.BACKUP_DIR}/$fileName")
        file.createFile()
        backupManager.backupEverything(file)

        return exportToUserAccessibleLocation(file, fileName)
    }

    override suspend fun restoreKeyMaps(uri: String, restoreType: RestoreType): KMResult<*> {
        val file = fileAdapter.getFileFromUri(uri)
        return backupManager.restore(file, restoreType = restoreType)
            .onFailure { Timber.e(it.toString()) }
    }

    override suspend fun getKeyMapCountInBackup(uri: String): KMResult<Int> {
        val file = fileAdapter.getFileFromUri(uri)

        return backupManager.getBackupContent(file)
            .then { Success(it.keyMapList?.size ?: 0) }
            .onFailure { Timber.e(it.toString()) }
    }

    override fun getDownloadedBackups(): KMResult<List<IFile>> =
        fileAdapter.getDownloads().then { files ->
            Success(files.filter { it.extension.equals("zip", ignoreCase = true) })
        }

    override fun canRequestFullFileAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            buildConfigProvider.canRequestAllFilesAccess &&
            !permissionAdapter.isGranted(Permission.MANAGE_EXTERNAL_STORAGE)

    override fun requestFullFileAccess() {
        permissionAdapter.request(Permission.MANAGE_EXTERNAL_STORAGE)
    }

    override suspend fun exportToUserAccessibleLocation(
        privateFile: IFile,
        fileName: String,
    ): KMResult<ExportedBackupLocation> {
        if (leanbackAdapter.isTvDevice() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return fileAdapter.openDownloadsFile(fileName, FileUtils.MIME_TYPE_ZIP).then { target ->
                privateFile.inputStream()!!.use { input ->
                    target.outputStream()!!.use { output -> input.copyTo(output) }
                }

                Success(ExportedBackupLocation.Downloads(fileName))
            }
        }

        return Success(
            ExportedBackupLocation.PublicUri(fileAdapter.getPublicUriForPrivateFile(privateFile)),
        )
    }
}

interface BackupRestoreMappingsUseCase {
    val onAutomaticBackupResult: Flow<KMResult<*>>
    suspend fun backupEverything(): KMResult<ExportedBackupLocation>
    suspend fun restoreKeyMaps(uri: String, restoreType: RestoreType): KMResult<*>

    suspend fun getKeyMapCountInBackup(uri: String): KMResult<Int>

    /**
     * Lists backup zips found in the Downloads folder, for choosing one to import on a
     * device with no usable system file picker (Android TV).
     */
    fun getDownloadedBackups(): KMResult<List<IFile>>

    /**
     * Whether the user could be offered the option to grant MANAGE_EXTERNAL_STORAGE so
     * [getDownloadedBackups] can see backups other apps put in Downloads, not just ones
     * this app itself exported. False if already granted or this build can't request it.
     */
    fun canRequestFullFileAccess(): Boolean
    fun requestFullFileAccess()

    /**
     * Decides where a freshly created backup zip should end up. On Android TV there is no
     * usable system file picker (see HomeKeyMapListScreen's import/export handling — SAF
     * and share intents resolve to fake stub activities that always fail), so the file is
     * written directly into the public Downloads collection instead of being shared
     * through the private-file + share-sheet flow used everywhere else.
     */
    suspend fun exportToUserAccessibleLocation(
        privateFile: IFile,
        fileName: String,
    ): KMResult<ExportedBackupLocation>
}
