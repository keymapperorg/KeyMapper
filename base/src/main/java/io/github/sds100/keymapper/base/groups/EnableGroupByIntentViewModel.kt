package io.github.sds100.keymapper.base.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EnableGroupByIntentViewModel @Inject constructor(
    private val selectGroup: SelectGroupUseCase,
    navigationProvider: NavigationProvider,
) : ViewModel(),
    NavigationProvider by navigationProvider {

    private val groupFamily: StateFlow<GroupFamily> = selectGroup.selectionGroupFamily
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            GroupFamily(null, emptyList(), emptyList()),
        )

    val breadcrumbs: StateFlow<List<GroupListItemModel>> = groupFamily.map { family ->
        family.parents.plus(family.group).filterNotNull().map {
            GroupListItemModel(it.uid, it.name)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<GroupListItemModel>> = groupFamily.map { family ->
        family.children.map { GroupListItemModel(it.uid, it.name) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGroupUid: StateFlow<String?> = groupFamily.map { it.group?.uid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isGroupPickerVisible = MutableStateFlow(false)
    val isGroupPickerVisible: StateFlow<Boolean> = _isGroupPickerVisible.asStateFlow()

    fun onSelectGroupClick() {
        _isGroupPickerVisible.update { true }
    }

    fun onDismissGroupPicker() {
        _isGroupPickerVisible.update { false }
    }

    fun onGroupClick(groupUid: String?) {
        viewModelScope.launch { selectGroup.openSelectionGroup(groupUid) }
    }

    fun onBackClick() {
        viewModelScope.launch { popBackStack() }
    }
}
