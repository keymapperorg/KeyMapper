package io.github.sds100.keymapper.ui.fragment.keymap

import android.content.ClipData
import android.content.Context
import android.widget.CheckBox
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.TriggerByIntentModel
import io.github.sds100.keymapper.data.model.options.TriggerOptions
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.TriggerOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.triggerByIntent
import io.github.sds100.keymapper.ui.adapter.OptionsController
import io.github.sds100.keymapper.ui.fragment.OptionsFragment
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.str
import splitties.systemservices.clipboardManager
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
class TriggerOptionsFragment : OptionsFragment<TriggerOptions>() {

    override val optionsViewModel: TriggerOptionsViewModel by lazy {
        navGraphViewModels<ConfigKeymapViewModel>(R.id.nav_config_keymap) {
            InjectorUtils.provideConfigKeymapViewModel(requireContext())
        }.value.triggerViewModel.optionsViewModel
    }

    override var isAppBarVisible = false

    override val controller by lazy { TriggerOptionsController() }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        super.subscribeUi(binding)

        optionsViewModel.triggerByIntent.observe(viewLifecycleOwner, {
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
                triggerByIntent {
                    id("trigger_by_intent")

                    model(triggerByIntentModel)

                    onClick { view ->
                        viewModel.setValue(
                            TriggerOptions.ID_TRIGGER_BY_INTENT, (view as CheckBox).isChecked)
                    }

                    onCopyClick { _ ->
                        triggerByIntentModel ?: return@onCopyClick

                        val clipData = ClipData.newPlainText(
                            str(R.string.clipboard_label_keymap_uid),
                            triggerByIntentModel?.uid)

                        clipboardManager.setPrimaryClip(clipData)

                        toast(R.string.toast_copied_keymap_uid_to_clipboard)
                    }
                }
            }

            super.buildModels()
        }
    }
}

