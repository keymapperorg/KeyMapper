package io.github.sds100.keymapper.ui.fragment.keymap

import android.content.ClipData
import android.content.Context
import android.widget.CheckBox
import androidx.fragment.app.FragmentActivity
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
import io.github.sds100.keymapper.ui.fragment.DefaultRecyclerViewFragment
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.str
import splitties.systemservices.clipboardManager
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
class TriggerOptionsFragment : DefaultRecyclerViewFragment() {

    val optionsViewModel: TriggerOptionsViewModel by lazy {
        navGraphViewModels<ConfigKeymapViewModel>(R.id.nav_config_keymap) {
            InjectorUtils.provideConfigKeymapViewModel(requireContext())
        }.value.triggerViewModel.optionsViewModel
    }

    override var isInPagerAdapter = true
    override var isAppBarVisible = false

    private val mController by lazy {

        object : OptionsController(this) {
            var triggerByIntent: TriggerByIntentModel? = null
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
                if (triggerByIntent != null) {
                    triggerByIntent {
                        id("trigger_by_intent")

                        model(triggerByIntent)

                        onClick { view ->
                            viewModel.setValue(
                                TriggerOptions.ID_TRIGGER_BY_INTENT, (view as CheckBox).isChecked)
                        }

                        onCopyClick { _ ->
                            triggerByIntent ?: return@onCopyClick

                            val clipData = ClipData.newPlainText(
                                str(R.string.clipboard_label_keymap_uid),
                                triggerByIntent?.uid)

                            clipboardManager.setPrimaryClip(clipData)

                            toast(R.string.toast_copied_keymap_uid_to_clipboard)
                        }
                    }
                }

                super.buildModels()
            }
        }
    }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        binding.apply {
            epoxyRecyclerView.adapter = mController.adapter

            optionsViewModel.triggerByIntent.observe(viewLifecycleOwner, {
                mController.triggerByIntent = it
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