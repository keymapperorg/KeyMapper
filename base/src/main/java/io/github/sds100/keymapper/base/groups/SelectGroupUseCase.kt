package io.github.sds100.keymapper.base.groups

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SelectGroupUseCase {
    val selectionGroupUid: StateFlow<String?>
    val selectionGroupFamily: Flow<GroupFamily>
    suspend fun openSelectionGroup(uid: String?)
}
