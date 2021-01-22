package io.github.sds100.keymapper.ui.fragment.keymap

import android.content.ClipData
import android.content.Context
import android.widget.CheckBox
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.TriggerByIntentModel
import io.github.sds100.keymapper.data.model.options.TriggerOptions
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.TriggerOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.triggerFromOtherApps
import io.github.sds100.keymapper.ui.adapter.OptionsController
import io.github.sds100.keymapper.ui.fragment.OptionsFragment
import io.github.sds100.keymapper.util.*
import splitties.systemservices.clipboardManager
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
class TriggerOptionsFragment : OptionsFragment<TriggerOptions>() {

    class Info : FragmentInfo(
        R.string.option_list_header,
        R.string.url_trigger_options_guide,
        { TriggerOptionsFragment() }
    )

    val configKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext())
    }

    override val optionsViewModel: TriggerOptionsViewModel
        get() = configKeymapViewModel.triggerViewModel.optionsViewModel

    override var isAppBarVisible = false

    override val controller by lazy { TriggerOptionsController() }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        super.subscribeUi(binding)

        optionsViewModel.triggerFromOtherApps.observe(viewLifecycleOwner, {
            controller.triggerByIntentModel = it
        })
    }

    inner class TriggerOptionsController : OptionsController(viewLifecycleOwner) {
        var triggerByIntentModel: TriggerByIntentModel? = null
            set(value) {
                field = value
                requestModelBuild()
            }

        override val ctx: Context
            get() = requireContext()

        override val viewModel: BaseOptionsViewModel<*>
            get() = optionsViewModel

        override fun buildModels() {
            if (triggerByIntentModel != null) {
                triggerFromOtherApps {
                    id("trigger_by_intent")

                    model(triggerByIntentModel)

                    onClick { view ->
                        viewModel.setValue(
                            TriggerOptions.ID_TRIGGER_FROM_OTHER_APPS, (view as CheckBox).isChecked)
                    }

                    onCopyClick { _ ->
                        triggerByIntentModel ?: return@onCopyClick

                        val clipData = ClipData.newPlainText(
                            str(R.string.clipboard_label_keymap_uid),
                            triggerByIntentModel?.uid)

                        clipboardManager.setPrimaryClip(clipData)

                        toast(R.string.toast_copied_keymap_uid_to_clipboard)
                    }

                    isCreatingLauncherShortcutsSupported(
                        ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())
                    )

                    onCreateLauncherShortcutClick { _ ->
                        triggerByIntentModel?.uid?.let {
                            viewLifecycleScope.launchWhenResumed {
                                createLauncherShortcut(it)
                            }
                        }
                    }

                    openIntentGuide { _ ->
                        UrlUtils.openUrl(
                            requireContext(),
                            str(R.string.url_trigger_by_intent_guide)
                        )
                    }
                }
            }

            super.buildModels()
        }
    }

    private suspend fun createLauncherShortcut(uuid: String) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) return

        val actionList = configKeymapViewModel.actionListViewModel.actionList.value ?: return

        val shortcutInfo = KeymapShortcutUtils.createShortcut(
            requireContext(),
            viewLifecycleOwner,
            uuid,
            actionList,
            optionsViewModel.getDeviceInfoList()
        )

        ShortcutManagerCompat.requestPinShortcut(requireContext(), shortcutInfo, null)
    }
}

