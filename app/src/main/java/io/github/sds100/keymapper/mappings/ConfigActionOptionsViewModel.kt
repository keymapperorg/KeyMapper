package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.ListItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * Created by sds100 on 12/04/2021.
 */

abstract class ConfigActionOptionsViewModel<M : Mapping<A>, A : Action>(
    coroutineScope: CoroutineScope,
    config: ConfigMappingUseCase<A, M>
) : OptionsViewModel {
    private val _actionUid = MutableStateFlow<String?>(null)
    val actionUid = _actionUid.asStateFlow()

    override val state = combine(config.mapping, _actionUid) { mapping, actionUid ->

        when {
            mapping is State.Data -> {
                val action = mapping.data.actionList.find { it.uid == actionUid }
                    ?: return@combine OptionsUiState(showProgressBar = true)

                OptionsUiState(
                    listItems = createListItems(mapping.data, action),

                    showProgressBar = false
                )
            }

            else -> OptionsUiState(showProgressBar = true)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            OptionsUiState(showProgressBar = true)
        )

    fun setActionToConfigure(uid: String) {
        _actionUid.value = uid
    }

    abstract fun createListItems(mapping: M, action: A): List<ListItem>
}