package io.github.sds100.keymapper.base.system.inputmethod

import android.os.Build
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.ImeSubtypeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

class KeyMapperImeHelper(
    private val switchImeInterface: SwitchImeInterface,
    private val imeAdapter: InputMethodAdapter,
    private val packageName: String,
) {
    companion object {
        const val KEY_MAPPER_GUI_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.latin"

        private const val KEY_MAPPER_LEANBACK_IME_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.leanback"

        private const val KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE =
            "io.github.sds100.keymapper.inputmethod.hackers"

        const val MIN_SUPPORTED_GUI_KEYBOARD_VERSION_CODE: Int = 20

        fun isKeyMapperInputMethod(imePackage: String, keyMapperPackageName: String): Boolean {
            return imePackage == keyMapperPackageName ||
                imePackage == KEY_MAPPER_GUI_IME_PACKAGE ||
                imePackage == KEY_MAPPER_LEANBACK_IME_PACKAGE ||
                imePackage == KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE
        }
    }

    private val keyMapperImePackageList = arrayOf(
        packageName,
        KEY_MAPPER_GUI_IME_PACKAGE,
        KEY_MAPPER_LEANBACK_IME_PACKAGE,
        KEY_MAPPER_HACKERS_KEYBOARD_PACKAGE,
    )

    val isCompatibleImeEnabledFlow: Flow<Boolean> =
        imeAdapter.inputMethods
            .map { containsCompatibleIme(it) }

    val isCompatibleImeChosenFlow: Flow<Boolean> =
        imeAdapter.chosenIme
            .map { chosenIme ->
                if (chosenIme == null) {
                    false
                } else {
                    isKeyMapperInputMethod(chosenIme.packageName, packageName)
                }
            }

    fun enableCompatibleInputMethod(): KMResult<Unit> {
        var result: KMResult<Unit>? = null

        for (imePackageName in keyMapperImePackageList) {
            val imeId =
                imeAdapter.getInfoByPackageName(imePackageName).valueOrNull()?.id ?: continue

            result = switchImeInterface.enableIme(imeId)

            // Stop trying to enable IMEs if one is enabled.
            if (result.isSuccess) {
                break
            }
        }

        return result ?: KMError.InputMethodNotFound(packageName)
    }

    fun chooseCompatibleInputMethod(): KMResult<String> =
        getLastUsedCompatibleImeId().then { imeId ->
            switchImeInterface.switchIme(imeId).then { Success(imeId) }
        }

    fun chooseLastUsedIncompatibleInputMethod(): KMResult<String> =
        getLastUsedIncompatibleImeId().then { imeId ->
            switchImeInterface.switchIme(imeId).then { Success(imeId) }
        }

    fun toggleCompatibleInputMethod(): KMResult<String> {
        return if (isCompatibleImeChosen()) {
            chooseLastUsedIncompatibleInputMethod()
        } else {
            chooseCompatibleInputMethod()
        }
    }

    fun isCompatibleImeChosen(): Boolean {
        val chosenIme = imeAdapter.getChosenIme() ?: return false

        return isKeyMapperInputMethod(chosenIme.packageName, packageName)
    }

    fun isCompatibleImeEnabled(): Boolean = imeAdapter.inputMethods
        .map { containsCompatibleIme(it) }
        .firstBlocking()

    private fun containsCompatibleIme(imeList: List<ImeInfo>): Boolean = imeList
        .filter { it.isEnabled }
        .any { it.packageName in keyMapperImePackageList }

    private fun getLastUsedCompatibleImeId(): KMResult<String> {
        for (ime in imeAdapter.inputMethodHistory.firstBlocking()) {
            if (ime.packageName in keyMapperImePackageList && ime.isEnabled) {
                return Success(ime.id)
            }
        }

        imeAdapter.getInfoByPackageName(KEY_MAPPER_GUI_IME_PACKAGE).onSuccess { ime ->
            if (ime.isEnabled) {
                return Success(ime.id)
            }
        }

        return imeAdapter.getInfoByPackageName(packageName).then { ime ->
            if (ime.isEnabled) {
                Success(ime.id)
            } else {
                KMError.NoCompatibleImeEnabled
            }
        }
    }

    /**
     * Switch the chosen input method and wait for it to finish switching before returning.
     * See issue #1064.
     */
    suspend fun switchImeAndWait(imeId: String): KMResult<Unit> {
        switchImeInterface.switchIme(imeId)
        return waitForChosenIme(imeId)
    }

    /**
     * Switch the chosen input method to its next enabled subtype (language), wrapping around
     * to the first subtype after the last.
     */
    suspend fun cycleKeyboardLanguage(): KMResult<Unit> {
        val chosenIme = imeAdapter.getChosenIme() ?: return KMError.CycleImeSubtypeFailed

        // See issue #2308. On Android 14+ the physical keyboard layout follows the subtype of
        // the last input method that has subtypes. The Key Mapper input method has none so
        // the layout stays the same while it is chosen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            chosenIme.packageName == packageName
        ) {
            return cycleLanguageOfLastUsedIncompatibleIme(chosenIme.id)
        }

        return getNextSubtypeId(chosenIme.id, imeAdapter.getSelectedSubtype())
            .then { imeAdapter.setSelectedSubtype(it) }
    }

    /**
     * The other input method must be chosen for the system to apply one of its subtypes to the
     * physical keyboard layout.
     */
    private suspend fun cycleLanguageOfLastUsedIncompatibleIme(
        keyMapperImeId: String,
    ): KMResult<Unit> {
        return getLastUsedIncompatibleImeId().then { imeId ->
            val result = imeAdapter.getInfoById(imeId)
                .then { ime -> if (ime.isEnabled) Success(ime) else SystemError.ImeDisabled(ime) }
                .then { getNextSubtypeId(imeId, imeAdapter.getLastUsedSubtype(imeId)) }
                // The subtype must be selected before the input method is chosen so the system
                // chooses the input method with it. The accessibility service can't be used to
                // choose the input method because it resets the selected subtype.
                .then { subtypeId -> imeAdapter.setSelectedSubtype(subtypeId) }
                .then { imeAdapter.setDefaultInputMethod(imeId) }
                .then { waitForChosenIme(imeId) }

            // Always switch back so the user isn't left without the Key Mapper input method.
            switchImeAndWait(keyMapperImeId).then { result }
        }
    }

    private fun getNextSubtypeId(imeId: String, currentSubtype: ImeSubtypeInfo?): KMResult<Int> {
        return imeAdapter.getEnabledSubtypes(imeId).then { subtypes ->
            if (subtypes.size < 2) {
                KMError.NotEnoughInputMethodSubtypes
            } else {
                Success(subtypes[(subtypes.indexOf(currentSubtype) + 1) % subtypes.size].id)
            }
        }
    }

    private suspend fun waitForChosenIme(imeId: String): KMResult<Unit> {
        val chosenIme = withTimeoutOrNull(2.seconds) {
            imeAdapter.chosenIme.filterNotNull().first { it.id == imeId }
        }

        return if (chosenIme == null) KMError.SwitchImeFailed else Success(Unit)
    }

    fun getLastUsedIncompatibleImeId(): KMResult<String> {
        for (ime in imeAdapter.inputMethodHistory.firstBlocking()) {
            if (ime.packageName !in keyMapperImePackageList) {
                return Success(ime.id)
            }
        }

        return KMError.NoIncompatibleKeyboardsInstalled
    }
}
