package io.github.sds100.keymapper.system.inputmethod

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.system.JobSchedulerHelper
import io.github.sds100.keymapper.system.root.SuAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@Singleton
class AndroidInputMethodAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val suAdapter: SuAdapter,
) : InputMethodAdapter {

    companion object {
        const val SETTINGS_SECURE_SUBTYPE_HISTORY_KEY = "input_methods_subtype_history"

        /**
         * @param history The value of [SETTINGS_SECURE_SUBTYPE_HISTORY_KEY].
         * @return The subtype the input method had when it was last used or null if it isn't in
         * the history.
         */
        fun getSubtypeHashCodeFromHistory(history: String?, imeId: String): Int? {
            return history
                ?.split(':')
                ?.map { it.split(';') }
                ?.firstOrNull { it[0] == imeId }
                ?.getOrNull(1)
                ?.toIntOrNull()
        }
    }

    override val inputMethodHistory: MutableStateFlow<List<ImeInfo>> by lazy {
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

    override val inputMethods: MutableStateFlow<List<ImeInfo>> by lazy {
        MutableStateFlow(
            getInputMethods(),
        )
    }

    override val chosenIme: StateFlow<ImeInfo?> =
        inputMethods
            .map { imeInfoList -> imeInfoList.find { it.isChosen } }
            .onEach {
                if (it == null) {
                    Timber.e("No input method is chosen.")
                } else {
                    Timber.d("On input method chosen, chosen IME = ${chosenIme.value}")
                }
            }
            .stateIn(coroutineScope, SharingStarted.Lazily, getChosenIme())

    init {
        // use job scheduler because there is there is a much shorter delay when the app is in the background
        JobSchedulerHelper.observeInputMethods(ctx)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

        ContextCompat.registerReceiver(
            ctx,
            broadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun showImePicker(fromForeground: Boolean): KMResult<*> {
        when {
            fromForeground || Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1 -> {
                inputMethodManager.showInputMethodPicker()
                return Success(Unit)
            }

            (Build.VERSION_CODES.O_MR1..Build.VERSION_CODES.P).contains(Build.VERSION.SDK_INT) -> {
                val command =
                    "am broadcast -a com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER"
                return runBlocking { suAdapter.execute(command) }
            }

            else -> return KMError.CantShowImePickerInBackground
        }
    }

    override fun getInfoById(imeId: String): KMResult<ImeInfo> {
        val info =
            getInputMethods().find { it.id == imeId } ?: return KMError.InputMethodNotFound(imeId)

        return Success(info)
    }

    override fun getInfoByPackageName(packageName: String): KMResult<ImeInfo> =
        getImeId(packageName).then { getInfoById(it) }

    /**
     * Example:
     * io.github.sds100.keymapper.system.inputmethod.latin/.LatinIME;-921088104
     * :com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME;1891618174
     */
    private fun getImeHistory(): List<String> {
        val subtypeString = getSubtypeHistoryString(ctx) ?: return emptyList()

        val ids = subtypeString
            .split(':')
            .map { it.split(';')[0] }

        return ids
    }

    fun onInputMethodsUpdate() {
        inputMethods.value = getInputMethods()
        inputMethodHistory.value = getImeHistory().mapNotNull { getInfoById(it).valueOrNull() }
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

    private fun getSubtypeHistoryString(ctx: Context): String? = Settings.Secure.getString(
        ctx.contentResolver,
        SETTINGS_SECURE_SUBTYPE_HISTORY_KEY,
    )

    override fun getChosenIme(): ImeInfo? {
        val chosenImeId = getChosenImeId()

        return getInfoById(chosenImeId).valueOrNull()
    }

    private fun getChosenImeId(): String =
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

    private fun getImeId(packageName: String): KMResult<String> {
        val imeId =
            inputMethodManager.inputMethodList.find { it.packageName == packageName }?.id

        return if (imeId == null) {
            KMError.InputMethodNotFound(packageName)
        } else {
            Success(imeId)
        }
    }

    override fun setDefaultInputMethod(imeId: String): KMResult<Unit> {
        return putSecureSetting(Settings.Secure.DEFAULT_INPUT_METHOD, imeId)
    }

    override fun getEnabledSubtypes(imeId: String): KMResult<List<ImeSubtypeInfo>> {
        val inputMethodInfo = inputMethodManager.inputMethodList.find { it.id == imeId }
            ?: return KMError.InputMethodNotFound(imeId)

        return Success(
            inputMethodManager.getEnabledInputMethodSubtypeList(inputMethodInfo, true)
                .map { ImeSubtypeInfo(id = it.hashCode(), languageTag = it.languageTag) },
        )
    }

    override fun getSelectedSubtype(): ImeSubtypeInfo? {
        val subtypeId = Settings.Secure.getInt(
            ctx.contentResolver,
            Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE,
            -1,
        )

        return getEnabledSubtypes(getChosenImeId()).valueOrNull()?.find { it.id == subtypeId }
    }

    override fun getLastUsedSubtype(imeId: String): ImeSubtypeInfo? {
        val subtypeId = getSubtypeHashCodeFromHistory(getSubtypeHistoryString(ctx), imeId)
            ?: return null

        return getEnabledSubtypes(imeId).valueOrNull()?.find { it.id == subtypeId }
    }

    override fun setSelectedSubtype(subtypeId: Int): KMResult<Unit> {
        return putSecureSetting(Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE, subtypeId)
    }

    private inline fun <reified T> putSecureSetting(name: String, value: T): KMResult<Unit> {
        return try {
            if (SettingsUtils.putSecureSetting(ctx, name, value)) {
                Success(Unit)
            } else {
                KMError.FailedToModifySystemSetting(name)
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Failed to write secure setting $name")
            KMError.FailedToModifySystemSetting(name)
        }
    }

    override fun getNextInputMethod(): KMResult<ImeInfo> {
        val chosenImeId = getChosenImeId()
        val enabledImes = getInputMethods().filter { it.isEnabled }

        if (enabledImes.isEmpty()) {
            return getInfoById(chosenImeId)
        }

        val currentIndex = enabledImes.indexOfFirst { it.id == chosenImeId }
        val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % enabledImes.size

        return Success(enabledImes[nextIndex])
    }
}
