package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.util.result.InputMethodNotFound
import io.github.sds100.keymapper.util.result.NoEnabledInputMethods
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import splitties.init.appCtx
import splitties.systemservices.inputMethodManager

/**
 * Created by sds100 on 28/12/2018.
 */

object KeyboardUtils {
    fun getInputMethodLabel(id: String): Result<String> {
        val label = inputMethodManager.enabledInputMethodList.find { it.id == id }
            ?.loadLabel(appCtx.packageManager)?.toString() ?: return InputMethodNotFound(id)

        return Success(label)
    }

    fun getInputMethodIds(): Result<List<String>> {
        if (inputMethodManager.enabledInputMethodList.isEmpty()) {
            return NoEnabledInputMethods()
        }

        return Success(inputMethodManager.enabledInputMethodList.map { it.id })
    }
}