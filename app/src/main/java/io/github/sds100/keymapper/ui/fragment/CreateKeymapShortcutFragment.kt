package io.github.sds100.keymapper.ui.fragment

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.viewmodel.CreateKeymapShortcutViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.keymap
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.Failure
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton

/**
 * Created by sds100 on 08/09/20.
 */

class CreateKeymapShortcutFragment : DefaultRecyclerViewFragment() {

    private val viewModel by activityViewModels<CreateKeymapShortcutViewModel> {
        InjectorUtils.provideCreateActionShortcutViewModel(requireContext())
    }

    private lateinit var recoverFailureDelegate: RecoverFailureDelegate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        recoverFailureDelegate = RecoverFailureDelegate(
            "CreateKeymapShortcutFragment",
            requireActivity().activityResultRegistry,
            viewLifecycleOwner) {

            viewModel.rebuildModels()
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {

        binding.caption = str(R.string.caption_create_keymap_shortcut)

        viewModel.eventStream.observe(viewLifecycleOwner, { event ->
            when (event) {
                is FixFailure ->
                    binding.coordinatorLayout.showFixActionSnackBar(
                        event.failure,
                        requireActivity(),
                        recoverFailureDelegate)

                is EnableAccessibilityServicePrompt ->
                    binding.coordinatorLayout.showEnableAccessibilityServiceSnackBar()

                is BuildKeymapListModels -> viewLifecycleScope.launchWhenResumed {
                    viewModel.setModelList(buildModelList(event.keymapList))
                }

                is CreateKeymapShortcutEvent -> viewLifecycleScope.launch {
                    val shortcutInfo = KeymapShortcutUtils.createShortcut(
                        requireActivity(),
                        event.uuid,
                        event.actionList,
                        viewModel.getDeviceInfoList()
                    )

                    ShortcutManagerCompat.createShortcutResultIntent(requireContext(), shortcutInfo)
                        .apply {
                            requireActivity().setResult(Activity.RESULT_OK, this)
                            requireActivity().finish()
                        }
                }
            }
        })

        viewModel.model.observe(viewLifecycleOwner, { state ->
            binding.state = state

            state.ifIsData {
                populateList(binding, it)
            }
        })
    }

    private fun populateList(binding: FragmentRecyclerviewBinding,
                             model: List<KeymapListItemModel>?) {
        binding.epoxyRecyclerView.withModels {
            model?.forEach {
                keymap {
                    id(it.id)
                    model(it)
                    isSelectable(false)

                    onErrorClick(object : ErrorClickCallback {
                        override fun onErrorClick(failure: Failure) {
                            viewModel.fixError(failure)
                        }
                    })

                    onClick { _ ->
                        viewModel.chooseKeymap(it.uid)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.rebuildModels()
    }

    private suspend fun buildModelList(keymapList: List<KeyMap>) =
        keymapList.map { keymap ->
            val deviceInfoList = viewModel.getDeviceInfoList()

            KeymapListItemModel(
                id = keymap.id,
                actionList = keymap.actionList.map { it.buildChipModel(requireContext(), deviceInfoList) },
                triggerDescription = keymap.trigger.buildDescription(requireContext(), deviceInfoList),
                constraintList = keymap.constraintList.map { it.buildModel(requireContext()) },
                constraintMode = keymap.constraintMode,
                flagsDescription = keymap.trigger.buildTriggerFlagsDescription(requireContext()),
                isEnabled = keymap.isEnabled,
                uid = keymap.uid
            )
        }

    override fun onBackPressed() {
        showOnBackPressedWarning()
    }

    private fun showOnBackPressedWarning() {
        requireContext().alertDialog {
            messageResource = R.string.dialog_message_are_you_sure_want_to_leave_without_saving

            positiveButton(R.string.pos_yes) {
                requireActivity().finish()
            }

            cancelButton()
            show()
        }
    }
}