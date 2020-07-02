package io.github.sds100.keymapper.util

import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.util.result.Result

/**
 * Created by sds100 on 11/06/2020.
 */
interface IActionError {
    fun canActionBePerformed(action: Action): Result<Action>
}