package io.github.sds100.keymapper.sorting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSortMenuBinding
import io.github.sds100.keymapper.databinding.SortMenuChipBinding
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SortMenuFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSortMenuBinding? = null
    private val binding get() = _binding!!

    private val sortViewModel: SortViewModel by activityViewModels {
        Inject.sortViewModel(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentSortMenuBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this
            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sortViewModel.saveCheckpoint()

        val triggerChip = getChipByField(SortField.TRIGGER)
        val actionsChip = getChipByField(SortField.ACTIONS)
        val constraintsChip = getChipByField(SortField.CONSTRAINTS)
        val optionsChip = getChipByField(SortField.OPTIONS)

        binding.chipGroupSortBy.addView(triggerChip.root)
        binding.chipGroupSortBy.addView(actionsChip.root)
        binding.chipGroupSortBy.addView(constraintsChip.root)
        binding.chipGroupSortBy.addView(optionsChip.root)

        binding.buttonApply.setOnClickListener {
            sortViewModel.applySortOrder()
            dismiss()
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
            sortViewModel.restoreCheckpoint()
        }

        binding.buttonSortReset.setOnClickListener {
            sortViewModel.resetSortOrder()
        }

        lifecycleScope.launch {
            combine(
                sortViewModel.triggerSortState,
                sortViewModel.actionsSortState,
                sortViewModel.constraintsSortState,
                sortViewModel.optionsSortState,
            ) { trigger, actions, constraints, options ->
                trigger != SortOrder.NONE || actions != SortOrder.NONE || constraints != SortOrder.NONE || options != SortOrder.NONE
            }.collectLatest { hasSortOrder ->
                binding.canResetSort = hasSortOrder
            }
        }
    }

    private fun getChipByField(
        field: SortField,
    ): SortMenuChipBinding {
        val chipBinding = SortMenuChipBinding.inflate(
            LayoutInflater.from(requireContext()),
            binding.chipGroupSortBy,
            false,
        )

        chipBinding.text = when (field) {
            SortField.TRIGGER -> getString(R.string.trigger_header)
            SortField.ACTIONS -> getString(R.string.action_list_header)
            SortField.CONSTRAINTS -> getString(R.string.constraint_list_header)
            SortField.OPTIONS -> getString(R.string.option_list_header)
        }

        lifecycleScope.launch {
            val stateFlow = when (field) {
                SortField.TRIGGER -> sortViewModel.triggerSortState
                SortField.ACTIONS -> sortViewModel.actionsSortState
                SortField.CONSTRAINTS -> sortViewModel.constraintsSortState
                SortField.OPTIONS -> sortViewModel.optionsSortState
            }

            stateFlow.collectLatest { sortOrder ->
                chipBinding.sortOrder = sortOrder
                chipBinding.chip.isSelected = sortOrder != SortOrder.NONE
                chipBinding.onClickListener = View.OnClickListener {
                    sortViewModel.toggleSortOrder(field)
                }
            }
        }

        return chipBinding
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
