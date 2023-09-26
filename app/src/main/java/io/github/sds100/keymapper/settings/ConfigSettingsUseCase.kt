package io.github.sds100.keymapper.settings

import androidx.datastore.preferences.core.Preferences
import io.github.sds100.keymapper.actions.sound.SoundFileInfo
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.shizuku.ShizukuAdapter
import io.github.sds100.keymapper.shizuku.ShizukuUtils
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
    private val soundsManager: SoundsManager,
    private val suAdapter: SuAdapter,
    private val packageManagerAdapter: PackageManagerAdapter,
    private val shizukuAdapter: ShizukuAdapter,
    private val devicesAdapter: DevicesAdapter
) : ConfigSettingsUseCase {

    private val imeHelper by lazy { KeyMapperImeHelper(inputMethodAdapter) }

    override val isRootGranted: Flow<Boolean> = suAdapter.isGranted

    override val isWriteSecureSettingsGranted: Flow<Boolean> = channelFlow {
        send(permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS))

        permissionAdapter.onPermissionsUpdate.collectLatest {
            send(permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS))
        }
    }

    override val isShizukuInstalled: Flow<Boolean> by lazy {
        shizukuAdapter.isInstalled
    }

    override val isShizukuStarted: Flow<Boolean> by lazy {
        shizukuAdapter.isStarted
    }

    override val isShizukuPermissionGranted: Flow<Boolean> = channelFlow {
        send(permissionAdapter.isGranted(Permission.SHIZUKU))

        permissionAdapter.onPermissionsUpdate.collectLatest {
            send(permissionAdapter.isGranted(Permission.SHIZUKU))
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

    override val connectedInputDevices: StateFlow<State<List<InputDeviceInfo>>>
        get() = devicesAdapter.connectedInputDevices

    override fun enableCompatibleIme() {
        imeHelper.enableCompatibleInputMethods()
    }

    override suspend fun chooseCompatibleIme(): Result<ImeInfo> {
        return imeHelper.chooseCompatibleInputMethod()
    }

    override suspend fun showImePicker(): Result<*> {
        return inputMethodAdapter.showImePicker(fromForeground = true)
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

    override val defaultLongPressDelay: Flow<Int> =
        preferenceRepository.get(Keys.defaultLongPressDelay)
            .map { it ?: PreferenceDefaults.LONG_PRESS_DELAY }

    override val defaultDoublePressDelay: Flow<Int> =
        preferenceRepository.get(Keys.defaultDoublePressDelay)
            .map { it ?: PreferenceDefaults.DOUBLE_PRESS_DELAY }

    override val defaultRepeatDelay: Flow<Int> =
        preferenceRepository.get(Keys.defaultRepeatDelay)
            .map { it ?: PreferenceDefaults.REPEAT_DELAY }

    override val defaultSequenceTriggerTimeout: Flow<Int> =
        preferenceRepository.get(Keys.defaultSequenceTriggerTimeout)
            .map { it ?: PreferenceDefaults.SEQUENCE_TRIGGER_TIMEOUT }

    override val defaultVibrateDuration: Flow<Int> =
        preferenceRepository.get(Keys.defaultVibrateDuration)
            .map { it ?: PreferenceDefaults.VIBRATION_DURATION }

    override val defaultRepeatRate: Flow<Int> =
        preferenceRepository.get(Keys.defaultRepeatRate)
            .map { it ?: PreferenceDefaults.REPEAT_RATE }

    override fun resetDefaultMappingOptions() {
        preferenceRepository.set(Keys.defaultLongPressDelay, null)
        preferenceRepository.set(Keys.defaultDoublePressDelay, null)
        preferenceRepository.set(Keys.defaultRepeatDelay, null)
        preferenceRepository.set(Keys.defaultSequenceTriggerTimeout, null)
        preferenceRepository.set(Keys.defaultVibrateDuration, null)
        preferenceRepository.set(Keys.defaultRepeatRate, null)
    }

    override fun requestWriteSecureSettingsPermission() {
        permissionAdapter.request(Permission.WRITE_SECURE_SETTINGS)
    }

    override fun requestShizukuPermission() {
        permissionAdapter.request(Permission.SHIZUKU)
    }

    override fun downloadShizuku() {
        packageManagerAdapter.downloadApp(ShizukuUtils.SHIZUKU_PACKAGE)
    }

    override fun openShizukuApp() {
        packageManagerAdapter.openApp(ShizukuUtils.SHIZUKU_PACKAGE)
    }

    override fun getSoundFiles(): List<SoundFileInfo> {
        return soundsManager.soundFiles.value
    }

    override fun deleteSoundFiles(uid: List<String>) {
        uid.forEach {
            soundsManager.deleteSound(it)
        }
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

    val isShizukuInstalled: Flow<Boolean>
    val isShizukuStarted: Flow<Boolean>
    val isShizukuPermissionGranted: Flow<Boolean>
    fun downloadShizuku()
    fun openShizukuApp()

    val rerouteKeyEvents: Flow<Boolean>
    val isCompatibleImeChosen: Flow<Boolean>
    val isCompatibleImeEnabled: Flow<Boolean>
    fun enableCompatibleIme()
    suspend fun chooseCompatibleIme(): Result<ImeInfo>
    suspend fun showImePicker(): Result<*>

    val defaultLongPressDelay: Flow<Int>
    val defaultDoublePressDelay: Flow<Int>
    val defaultRepeatDelay: Flow<Int>
    val defaultSequenceTriggerTimeout: Flow<Int>
    val defaultVibrateDuration: Flow<Int>
    val defaultRepeatRate: Flow<Int>

    fun getSoundFiles(): List<SoundFileInfo>
    fun deleteSoundFiles(uid: List<String>)
    fun resetDefaultMappingOptions()
    fun requestWriteSecureSettingsPermission()
    fun requestShizukuPermission()

    val connectedInputDevices: StateFlow<State<List<InputDeviceInfo>>>
}