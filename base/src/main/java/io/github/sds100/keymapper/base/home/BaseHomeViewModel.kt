package io.github.sds100.keymapper.base.home

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.backup.BackupRestoreMappingsUseCase
import io.github.sds100.keymapper.base.keymaps.KeyMapListViewModel
import io.github.sds100.keymapper.base.keymaps.ListKeyMapsUseCase
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.sorting.SortKeyMapsUseCase
import io.github.sds100.keymapper.base.system.inputmethod.ShowInputMethodPickerUseCase
import io.github.sds100.keymapper.base.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogResponse
import io.github.sds100.keymapper.base.utils.ui.PopupUi
import io.github.sds100.keymapper.base.utils.ui.PopupViewModel
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showPopup
import io.github.sds100.keymapper.common.BuildConfigProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

abstract class BaseHomeViewModel(
    private val listKeyMaps: ListKeyMapsUseCase,
    private val pauseKeyMaps: PauseKeyMapsUseCase,
    private val backupRestore: BackupRestoreMappingsUseCase,
    private val showAlertsUseCase: ShowHomeScreenAlertsUseCase,
    private val onboarding: OnboardingUseCase,
    resourceProvider: ResourceProvider,
    private val setupGuiKeyboard: SetupGuiKeyboardUseCase,
    private val sortKeyMaps: SortKeyMapsUseCase,
    private val showInputMethodPickerUseCase: ShowInputMethodPickerUseCase,
    private val buildConfigProvider: BuildConfigProvider,
    navigationProvider: NavigationProvider,
    popupViewModel: PopupViewModel,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by popupViewModel,
    NavigationProvider by navigationProvider {

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
            navigationProvider,
            popupViewModel,
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

    fun launchSettings() {
        viewModelScope.launch {
            navigate("settings", NavDestination.Settings)
        }
    }

    fun launchAbout() {
        viewModelScope.launch {
            navigate("about", NavDestination.About)
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

        if (showFloatingLayouts && Build.VERSION.SDK_INT >= buildConfigProvider.minApi) {
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
