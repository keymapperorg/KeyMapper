package io.github.sds100.keymapper.settings

import androidx.datastore.preferences.core.Preferences
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 14/02/2021.
 */
class ConfigSettingsUseCaseImpl(
    private val preferenceRepository: PreferenceRepository,
    private val permissionAdapter: PermissionAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    suAdapter: SuAdapter
) : ConfigSettingsUseCase {

    private val imeHelper by lazy { KeyMapperImeHelper(inputMethodAdapter) }

    override val isRootGranted: Flow<Boolean> = suAdapter.isGranted

    override val isWriteSecureSettingsGranted: Flow<Boolean> = channelFlow {
        send(permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS))

        permissionAdapter.onPermissionsUpdate.collectLatest {
            send(permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS))
        }
    }

    override val rerouteKeyEvents: Flow<Boolean> =
        preferenceRepository.get(Keys.rerouteKeyEvents).map { it ?: false }

    override val isCompatibleImeChosen: Flow<Boolean> = inputMethodAdapter.chosenIme.map {
        imeHelper.isCompatibleImeChosen()
    }

    override val isCompatibleImeEnabled: Flow<Boolean> = inputMethodAdapter.inputMethods.map {
        imeHelper.isCompatibleImeEnabled()
    }

    override fun enableCompatibleIme() {
        imeHelper.enableCompatibleInputMethods()
    }

    override suspend fun chooseCompatibleIme(): Result<ImeInfo> {
        return imeHelper.chooseCompatibleInputMethod(fromForeground = true)
    }

    override fun <T> getPreference(key: Preferences.Key<T>) =
        preferenceRepository.get(key)

    override fun <T> setPreference(key: Preferences.Key<T>, value: T?) =
        preferenceRepository.set(key, value)

    override val automaticBackupLocation =
        preferenceRepository.get(Keys.automaticBackupLocation).map { it ?: "" }

    override fun setAutomaticBackupLocation(uri: String) {
        preferenceRepository.set(Keys.automaticBackupLocation, uri)
    }

    override fun disableAutomaticBackup() {
        preferenceRepository.set(Keys.automaticBackupLocation, null)
    }
}

interface ConfigSettingsUseCase {
    fun <T> getPreference(key: Preferences.Key<T>): Flow<T?>
    fun <T> setPreference(key: Preferences.Key<T>, value: T?)
    val automaticBackupLocation: Flow<String>
    fun setAutomaticBackupLocation(uri: String)
    fun disableAutomaticBackup()
    val isRootGranted: Flow<Boolean>
    val isWriteSecureSettingsGranted: Flow<Boolean>
    val rerouteKeyEvents: Flow<Boolean>
    val isCompatibleImeChosen: Flow<Boolean>
    val isCompatibleImeEnabled: Flow<Boolean>

    fun enableCompatibleIme()
    suspend fun chooseCompatibleIme(): Result<ImeInfo>
}