package io.github.sds100.keymapper.actions.tapscreenelement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.data.repositories.ViewIdRepository
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.success
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SimpleListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber

class PickScreenElementViewModel(
    resourceProvider: ResourceProvider,
    viewIdRepository: ViewIdRepository
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    private val _viewIdRepository = viewIdRepository

    private val _listItems = MutableStateFlow<State<List<SimpleListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()

    private val _returnResult = MutableSharedFlow<PickScreenElementResult>()
    val returnResult = _returnResult.asSharedFlow()

    fun onListItemClick(elementId: String, packageName: String) {
        Timber.d("onListItemClick %s", elementId)

        viewModelScope.launch {
            _returnResult.emit(
                PickScreenElementResult(
                    elementId = elementId,
                    packageName = packageName,
                    fullName = "${packageName}/${elementId}",
                    description = ""
                )
            )
        }
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            _listItems.value = State.Data(buildListItems())
        }
    }

    private suspend fun buildListItems(): List<SimpleListItem> {
        _viewIdRepository.viewIdList.collect { data ->

            val listItems = arrayListOf<DefaultSimpleListItem>()

            data.dataOrNull()?.forEachIndexed { _, viewIdEntity ->
                listItems.add(
                    DefaultSimpleListItem(
                        id = viewIdEntity.id.toString(),
                        title = viewIdEntity.viewId,
                        subtitle = viewIdEntity.packageName,
                    )
                )
            }

            viewModelScope.launch(Dispatchers.Default) {
                _listItems.value = State.Data(listItems)
            }

        }

        return arrayListOf()
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider,
        private val viewIdRepository: ViewIdRepository
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PickScreenElementViewModel(resourceProvider, viewIdRepository) as T
        }
    }
}