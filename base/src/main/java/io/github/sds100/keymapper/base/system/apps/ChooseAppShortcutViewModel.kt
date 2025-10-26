package io.github.sds100.keymapper.base.system.apps

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.filterByQuery
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.IconInfo
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.common.utils.valueOrNull
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class ChooseAppShortcutViewModel @Inject constructor(
    private val useCase: DisplayAppShortcutsUseCase,
    private val resourceProvider: ResourceProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    DialogProvider by dialogProvider,
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
            listItems,
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

            // the shortcut intents seem to be returned in 2 different formats.
            @Suppress("DEPRECATION")
            if (intent.extras != null &&
                intent.extras!!.containsKey(Intent.EXTRA_SHORTCUT_INTENT)
            ) {
                // get intent from selected shortcut
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
                shortcutName = showDialog(
                    "create_shortcut_name",
                    DialogModel.Text(
                        hint = getString(R.string.hint_shortcut_name),
                        allowEmpty = false,
                    ),
                ) ?: return@launch
            }

            _returnResult.emit(
                ChooseAppShortcutResult(
                    packageName = packageName,
                    shortcutName = shortcutName,
                    uri = uri,
                ),
            )
        }
    }
}
