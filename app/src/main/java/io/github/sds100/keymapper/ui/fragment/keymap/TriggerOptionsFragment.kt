package io.github.sds100.keymapper.ui.fragment.keymap

import android.content.ClipData
import android.content.Context
import android.widget.CheckBox
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.fragment.app.FragmentActivity
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
import io.github.sds100.keymapper.ui.fragment.DefaultRecyclerViewFragment
import io.github.sds100.keymapper.util.*
import splitties.systemservices.clipboardManager
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
class TriggerOptionsFragment : DefaultRecyclerViewFragment() {

    val configKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext())
    }

    val optionsViewModel: TriggerOptionsViewModel
        get() = configKeymapViewModel.triggerViewModel.optionsViewModel

    override var isInPagerAdapter = true
    override var isAppBarVisible = false

    private val mController by lazy {

        object : OptionsController(this) {
            var triggerByIntentModel: TriggerByIntentModel? = null
                set(value) {
                    field = value
                    requestModelBuild()
                }

            override val activity: FragmentActivity
                get() = requireActivity()

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
                            requireContext().openUrl(str(R.string.url_trigger_by_intent_guide))
                        }
                    }
                }

                super.buildModels()
            }
        }
    }

    private suspend fun createLauncherShortcut(uuid: String) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(requireContext())) return

        val actionList = configKeymapViewModel.actionListViewModel.actionList.value ?: return

        val shortcutInfo = KeymapShortcutUtils.createShortcut(
            requireActivity(),
            uuid,
            actionList,
            optionsViewModel.getDeviceInfoList()
        )

        ShortcutManagerCompat.requestPinShortcut(requireContext(), shortcutInfo, null)
    }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        binding.apply {
            epoxyRecyclerView.adapter = mController.adapter

            optionsViewModel.triggerFromOtherApps.observe(viewLifecycleOwner, {
                mController.triggerByIntentModel = it
            })

            optionsViewModel.checkBoxModels.observe(viewLifecycleOwner, {
                state = Data(Unit)
                mController.checkBoxModels = it
            })

            optionsViewModel.sliderModels.observe(viewLifecycleOwner, {
                state = Data(Unit)
                mController.sliderModels = it
            })
        }
    }
}