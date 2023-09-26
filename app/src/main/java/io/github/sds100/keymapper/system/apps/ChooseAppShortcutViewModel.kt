package io.github.sds100.keymapper.system.apps

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.filterByQuery
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.ui.*
import io.github.sds100.keymapper.util.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by sds100 on 27/01/2020.
 */
class ChooseAppShortcutViewModel internal constructor(
    private val useCase: DisplayAppShortcutsUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider {

    val searchQuery = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow<State<List<AppShortcutListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _returnResult = MutableSharedFlow<ChooseAppShortcutResult>()
    val returnResult = _returnResult.asSharedFlow()

    private val listItems = useCase.shortcuts.map { state ->
        state.mapData { appShortcuts ->
            appShortcuts
                .mapNotNull {
                    val name = useCase.getShortcutName(it).valueOrNull()
                        ?: return@mapNotNull null

                    val icon = useCase.getShortcutIcon(it).valueOrNull()
                        ?: return@mapNotNull null

                    AppShortcutListItem(shortcutInfo = it, name, IconInfo(icon))
                }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }
    }.flowOn(Dispatchers.Default)

    init {
        combine(
            searchQuery,
            listItems
        ) { query, listItems ->
            when (listItems) {
                is State.Data -> {
                    listItems.data.filterByQuery(query).collect {
                        _state.value = it
                    }
                }

                State.Loading -> _state.value = State.Loading
            }
        }.launchIn(viewModelScope)
    }

    fun onConfigureShortcutResult(intent: Intent) {
        viewModelScope.launch {
            val uri: String

            //the shortcut intents seem to be returned in 2 different formats.
            @Suppress("DEPRECATION")
            if (intent.extras != null &&
                intent.extras!!.containsKey(Intent.EXTRA_SHORTCUT_INTENT)
            ) {
                //get intent from selected shortcut
                val shortcutIntent =
                    intent.extras!!.get(Intent.EXTRA_SHORTCUT_INTENT) as Intent
                uri = shortcutIntent.toUri(0)

            } else {
                uri = intent.toUri(0)
            }

            val packageName = Intent.parseUri(uri, 0).`package`
                ?: intent.component?.packageName
                ?: Intent.parseUri(uri, 0).component?.packageName

            val intentShortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)

            val shortcutName: String

            if (intentShortcutName != null) {
                shortcutName = intentShortcutName
            } else {
                shortcutName = showPopup(
                    "create_shortcut_name",
                    PopupUi.Text(
                        hint = getString(R.string.hint_shortcut_name),
                        allowEmpty = false
                    )
                ) ?: return@launch
            }

            _returnResult.emit(
                ChooseAppShortcutResult(
                    packageName = packageName,
                    shortcutName = shortcutName,
                    uri = uri
                )
            )
        }
    }

    class Factory(
        private val useCase: DisplayAppShortcutsUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            ChooseAppShortcutViewModel(
                useCase,
                resourceProvider
            ) as T
    }
}
