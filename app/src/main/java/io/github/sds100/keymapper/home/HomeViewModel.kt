package io.github.sds100.keymapper.home

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.floating.ListFloatingLayoutsUseCase
import io.github.sds100.keymapper.floating.ListFloatingLayoutsViewModel
import io.github.sds100.keymapper.keymaps.KeyMapListViewModel
import io.github.sds100.keymapper.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.util.ui.DialogResponse
import io.github.sds100.keymapper.util.ui.NavigationViewModel
import io.github.sds100.keymapper.util.ui.NavigationViewModelImpl
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.showPopup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val listKeyMaps: ListKeyMapsUseCase,
    private val pauseKeyMaps: PauseKeyMapsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val onboarding: OnboardingUseCase,
    resourceProvider: ResourceProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val sortKeyMaps: SortKeyMapsUseCase,
    private val listFloatingLayouts: ListFloatingLayoutsUseCase,
    private val showInputMethodPickerUseCase: ShowInputMethodPickerUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    val navBarItems: StateFlow<List<HomeNavBarItem>> =
        combine(
            listFloatingLayouts.showFloatingLayouts,
            onboarding.hasViewedAdvancedTriggers,
            transform = ::buildNavBarItems,
        )
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                buildNavBarItems(
                    showFloatingLayouts = false,
                    viewedAdvancedTriggers = false,
                ),
            )

    val keyMapListViewModel by lazy {
        KeyMapListViewModel(
            viewModelScope,
            listKeyMaps,
            resourceProvider,
            setupGuiKeyboard,
            sortKeyMaps,
            showAlertsUseCase,
            pauseKeyMaps,
            backupRestore,
            showInputMethodPickerUseCase,
            onboarding,
        )
    }

    val listFloatingLayoutsViewModel by lazy {
        ListFloatingLayoutsViewModel(
            viewModelScope,
            listFloatingLayouts,
            resourceProvider,
        )
    }

    init {
        viewModelScope.launch {
            onboarding.showWhatsNew.collect { showWhatsNew ->
                if (showWhatsNew) {
                    showWhatsNewDialog()
                }
            }
        }

        viewModelScope.launch {
            if (setupGuiKeyboard.isInstalled.first() && !setupGuiKeyboard.isCompatibleVersion.first()) {
                showUpgradeGuiKeyboardDialog()
            }
        }
    }

    private fun buildNavBarItems(
        showFloatingLayouts: Boolean,
        viewedAdvancedTriggers: Boolean,
    ): List<HomeNavBarItem> {
        val items = mutableListOf<HomeNavBarItem>()
        items.add(
            HomeNavBarItem(
                HomeDestination.KeyMaps,
                getString(R.string.home_nav_bar_key_maps),
                icon = Icons.Outlined.Keyboard,
                badge = null,
            ),
        )

        if (showFloatingLayouts && Build.VERSION.SDK_INT >= Constants.MIN_API_FLOATING_BUTTONS) {
            items.add(
                HomeNavBarItem(
                    HomeDestination.FloatingButtons,
                    getString(R.string.home_nav_bar_floating_buttons),
                    icon = Icons.Outlined.BubbleChart,
                    badge = if (viewedAdvancedTriggers) {
                        null
                    } else {
                        getString(R.string.button_advanced_triggers_badge)
                    },
                ),
            )
        }

        return items
    }

    private suspend fun showWhatsNewDialog() {
        val dialog = PopupUi.Dialog(
            title = getString(R.string.whats_new),
            message = onboarding.getWhatsNewText(),
            positiveButtonText = getString(R.string.pos_ok),
            neutralButtonText = getString(R.string.neutral_changelog),
        )

        // don't return if they dismiss the dialog because this is common behaviour.
        val response = showPopup("whats-new", dialog)

        if (response == DialogResponse.NEUTRAL) {
            showPopup("url_changelog", PopupUi.OpenUrl(getString(R.string.url_changelog)))
        }

        onboarding.showedWhatsNew()
    }

    private suspend fun showUpgradeGuiKeyboardDialog() {
        val dialog = PopupUi.Dialog(
            title = getString(R.string.dialog_upgrade_gui_keyboard_title),
            message = getString(R.string.dialog_upgrade_gui_keyboard_message),
            positiveButtonText = getString(R.string.dialog_upgrade_gui_keyboard_positive),
            negativeButtonText = getString(R.string.dialog_upgrade_gui_keyboard_neutral),
        )

        val response = showPopup("upgrade_gui_keyboard", dialog)

        if (response == DialogResponse.POSITIVE) {
            showPopup(
                "gui_keyboard_play_store",
                PopupUi.OpenUrl(getString(R.string.url_play_store_keymapper_gui_keyboard)),
            )
        }
    }
}

enum class SelectedKeyMapsEnabled {
    ALL,
    NONE,
    MIXED,
}

data class HomeWarningListItem(
    val id: String,
    val text: String,
)

data class HomeNavBarItem(
    val destination: HomeDestination,
    val label: String,
    val icon: ImageVector,
    val badge: String? = null,
)
