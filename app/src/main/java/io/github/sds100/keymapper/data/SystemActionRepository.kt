package io.github.sds100.keymapper.data

import io.github.sds100.keymapper.data.model.SystemActionDef
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 17/05/2020.
 */
interface SystemActionRepository {
    val supportedSystemActions: List<SystemActionDef>
    val unsupportedSystemActions: Map<SystemActionDef, Failure>
}