package io.github.sds100.keymapper.mappings.keymaps

import android.app.Activity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.keymap
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.ui.OnChipClickCallback
import io.github.sds100.keymapper.util.ui.showPopups
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.str
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import splitties.alertdialog.appcompat.*

/**
 * Created by sds100 on 08/09/20.
 */

class CreateKeyMapShortcutFragment : SimpleRecyclerViewFragment<KeyMapListItem>() {

    private val viewModel by activityViewModels<CreateKeyMapShortcutViewModel> {
        Inject.createActionShortcutViewModel(requireContext())
    }

    override val listItems: Flow<State<List<KeyMapListItem>>>
        get() = viewModel.state

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        viewModel.showPopups(this, binding)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnUidResult.collectLatest { intent ->
                requireActivity().setResult(Activity.RESULT_OK, intent)
                requireActivity().finish()
            }
        }

        binding.caption = str(R.string.caption_create_keymap_shortcut)
    }

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<KeyMapListItem>
    ) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                keymap {
                    id(listItem.keyMapUiState.uid)
                    keyMapUiState(listItem.keyMapUiState)

                    selectionState(listItem.selectionUiState)

                    onTriggerErrorClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                            viewModel.onTriggerErrorChipClick(chipModel)
                        }
                    })

                    onActionChipClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                            viewModel.onActionChipClick(chipModel)
                        }
                    })

                    onConstraintChipClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                            viewModel.onConstraintsChipClick(chipModel)
                        }
                    })

                    onCardClick { _ ->
                        viewModel.onKeyMapCardClick(listItem.keyMapUiState.uid)
                    }
                }
            }
        }
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

            negativeButton(R.string.neg_cancel) { it.cancel() }
            show()
        }
    }
}