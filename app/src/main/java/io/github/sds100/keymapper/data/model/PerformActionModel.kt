package io.github.sds100.keymapper.data.model

import io.github.sds100.keymapper.service.KeyMapperImeService

/**
 * Created by sds100 on 01/06/20.
 */
data class PerformActionModel(val action: Action,
                              val showPerformingActionToast: Boolean = false,
                              val additionalMetaState: Int = 0,
                              val keyEventAction: Int = KeyMapperImeService.ACTION_DOWN_UP)