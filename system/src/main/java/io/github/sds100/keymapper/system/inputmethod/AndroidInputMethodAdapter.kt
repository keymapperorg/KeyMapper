package io.github.sds100.keymapper.system.inputmethod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.otherwise
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.system.JobSchedulerHelper
import io.github.sds100.keymapper.system.SettingsUtils
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidInputMethodAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val serviceAdapter: AccessibilityServiceAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val suAdapter: SuAdapter,
    private val buildConfigProvider: BuildConfigProvider,
) : InputMethodAdapter {

    companion object {
        const val SETTINGS_SECURE_SUBTYPE_HISTORY_KEY = "input_methods_subtype_history"
    }

    override val inputMethodHistory by lazy {
        val initialValues = getImeHistory().mapNotNull { getInfoById(it).valueOrNull() }
        MutableStateFlow(initialValues)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            context ?: return

            when (intent.action) {
                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    onInputMethodsUpdate()
                }
            }
        }
    }

    private val ctx = context.applicationContext

    private val inputMethodManager: InputMethodManager = ctx.getSystemService()!!

    override val inputMethods by lazy { MutableStateFlow(getInputMethods()) }

    override val chosenIme: StateFlow<ImeInfo?> =
        inputMethods
            .map { imeInfoList -> imeInfoList.find { it.isChosen } }
            .onEach {
                if (it == null) {
                    Timber.e("No input method is chosen.")
                }
            }
            .stateIn(coroutineScope, SharingStarted.Lazily, getChosenIme())

    override val isUserInputRequiredToChangeIme: Flow<Boolean> = channelFlow {
        suspend fun invalidate() {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    serviceAdapter.state.first() == AccessibilityServiceState.ENABLED -> send(
                    true,
                )

                permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS) -> send(true)

                else -> send(false)
            }
        }

        invalidate()

        launch {
            permissionAdapter.onPermissionsUpdate.collectLatest {
                invalidate()
            }
        }

        launch {
            serviceAdapter.state.collectLatest {
                invalidate()
            }
        }
    }

    init {
        // use job scheduler because there is there is a much shorter delay when the app is in the background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            JobSchedulerHelper.observeInputMethods(ctx)
        } else {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)

                    onInputMethodsUpdate()
                }
            }

            ctx.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_INPUT_METHODS),
                false,
                observer,
            )

            ctx.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(
                    SETTINGS_SECURE_SUBTYPE_HISTORY_KEY,
                ),
                false,
                observer,
            )
        }

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

        ContextCompat.registerReceiver(
            ctx,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun showImePicker(fromForeground: Boolean): Result<*> {
        when {
            fromForeground || Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 -> {
                inputMethodManager.showInputMethodPicker()
                return Success(Unit)
            }

            (Build.VERSION_CODES.O_MR1..Build.VERSION_CODES.P).contains(Build.VERSION.SDK_INT) -> {
                val command =
                    "am broadcast -a com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER"
                return suAdapter.execute(command)
            }

            else -> return Error.CantShowImePickerInBackground
        }
    }

    override suspend fun enableIme(imeId: String): Result<*> = enableImeWithoutUserInput(imeId).otherwise {
        try {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK

            ctx.startActivity(intent)
            Success(Unit)
        } catch (e: Exception) {
            Error.CantFindImeSettings
        }
    }

    private suspend fun enableImeWithoutUserInput(imeId: String): Result<*> {
        return getInfoByPackageName(buildConfigProvider.packageName).then { keyMapperImeInfo ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && imeId == keyMapperImeInfo.id) {
                serviceAdapter.send(AccessibilityServiceEvent.EnableInputMethod(keyMapperImeInfo.id))
            } else {
                suAdapter.execute("ime enable $imeId")
            }
        }
    }

    override suspend fun chooseImeWithoutUserInput(imeId: String): Result<ImeInfo> {
        getInfoById(imeId).onSuccess {
            if (!it.isEnabled) {
                return SystemError.ImeDisabled(it)
            }
        }.onFailure {
            return it
        }

        var failed = true

        if (failed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && serviceAdapter.state.value == AccessibilityServiceState.ENABLED) {
            serviceAdapter.send(AccessibilityServiceEvent.ChangeIme(imeId)).onSuccess {
                failed = false
            }
        }

        if (failed && permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
            SettingsUtils.putSecureSetting(
                ctx,
                Settings.Secure.DEFAULT_INPUT_METHOD,
                imeId,
            )

            failed = false
        }

        if (failed) {
            return Error.FailedToChangeIme
        }

        // wait for the ime to change and then return the info of the ime
        val didImeChange = withTimeoutOrNull(2000) {
            chosenIme.first { it?.id == imeId }
        }

        if (didImeChange != null) {
            return Success(didImeChange)
        } else {
            return Error.FailedToChangeIme
        }
    }

    override fun getInfoById(imeId: String): Result<ImeInfo> {
        val info =
            getInputMethods().find { it.id == imeId } ?: return Error.InputMethodNotFound(imeId)

        return Success(info)
    }

    override fun getInfoByPackageName(packageName: String): Result<ImeInfo> = getImeId(packageName).then { getInfoById(it) }

    /**
     * Example:
     * io.github.sds100.keymapper.system.inputmethod.latin/.LatinIME;-921088104
     * :com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME;1891618174
     */
    private fun getImeHistory(): List<String> {
        val ids = getSubtypeHistoryString(ctx)
            .split(':')
            .map { it.split(';')[0] }

        return ids
    }

    fun onInputMethodsUpdate() {
        inputMethods.value = getInputMethods()
        inputMethodHistory.value = getImeHistory().mapNotNull { getInfoById(it).valueOrNull() }
        Timber.i("On input method update, chosen IME = ${chosenIme.value}")
    }

    private fun getInputMethods(): List<ImeInfo> {
        val chosenImeId = getChosenImeId()

        val enabledInputMethods = inputMethodManager.enabledInputMethodList

        return inputMethodManager.inputMethodList.map { inputMethodInfo ->
            ImeInfo(
                inputMethodInfo.id,
                inputMethodInfo.packageName,
                inputMethodInfo.loadLabel(ctx.packageManager).toString(),
                isChosen = inputMethodInfo.id == chosenImeId,
                isEnabled = enabledInputMethods.any { it.id == inputMethodInfo.id },
            )
        }
    }

    private fun getSubtypeHistoryString(ctx: Context): String = Settings.Secure.getString(
        ctx.contentResolver,
        SETTINGS_SECURE_SUBTYPE_HISTORY_KEY,
    )

    private fun getChosenIme(): ImeInfo? {
        val chosenImeId = getChosenImeId()

        return getInfoById(chosenImeId).valueOrNull()
    }

    private fun getChosenImeId(): String = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

    private fun getImeId(packageName: String): Result<String> {
        val imeId =
            inputMethodManager.inputMethodList.find { it.packageName == packageName }?.id

        return if (imeId == null) {
            Error.InputMethodNotFound(packageName)
        } else {
            Success(imeId)
        }
    }
}
