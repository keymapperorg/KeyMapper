package io.github.sds100.keymapper.mappings.keymaps.trigger

import android.content.ClipData
import android.content.ClipboardManager
import androidx.core.content.getSystemService
import androidx.navigation.navGraphViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapTriggerOptionsViewModel
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.triggerFromOtherApps
import io.github.sds100.keymapper.ui.utils.configuredCheckBox
import io.github.sds100.keymapper.ui.utils.configuredSlider
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.CheckBoxListItem
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.ui.SliderListItem
import kotlinx.coroutines.flow.Flow
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
class ConfigTriggerOptionsFragment : SimpleRecyclerViewFragment<ListItem>() {

    class Info : FragmentInfo(
        R.string.option_list_header,
        R.string.url_trigger_options_guide,
        { ConfigTriggerOptionsFragment() }
    )

    private val configKeyMapViewModel: ConfigKeyMapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    private val viewModel: ConfigKeyMapTriggerOptionsViewModel
        get() = configKeyMapViewModel.configTriggerViewModel.optionsViewModel

    override var isAppBarVisible = false

    override val listItems: Flow<State<List<ListItem>>>
        get() = viewModel.state

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<ListItem>) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                if (listItem is CheckBoxListItem) {
                    configuredCheckBox(listItem) { isChecked ->
                        viewModel.setCheckboxValue(listItem.id, isChecked)
                    }
                }

                if (listItem is SliderListItem) {
                    configuredSlider(this@ConfigTriggerOptionsFragment, listItem) { newValue ->
                        viewModel.setSliderValue(listItem.id, newValue)
                    }
                }

                if (listItem is TriggerFromOtherAppsListItem) {
                    triggerFromOtherApps {
                        id(listItem.id)

                        model(listItem)

                        onCheckedChange { buttonView, isChecked ->
                            viewModel.setCheckboxValue(listItem.id, isChecked)
                        }

                        onCopyClick { _ ->
                            val clipData = ClipData.newPlainText(
                                str(R.string.clipboard_label_keymap_uid),
                                listItem.keyMapUid
                            )

                            val clipboardManager: ClipboardManager =
                                requireContext().getSystemService()!!

                            clipboardManager.setPrimaryClip(clipData)

                            toast(R.string.toast_copied_keymap_uid_to_clipboard)
                        }

                        onCreateLauncherShortcutClick { _ ->
                            viewModel.createLauncherShortcut()
                        }

                        openIntentGuide { _ ->
                            UrlUtils.openUrl(
                                requireContext(),
                                str(R.string.url_trigger_by_intent_guide)
                            )
                        }
                    }
                }
            }
        }
    }
}

