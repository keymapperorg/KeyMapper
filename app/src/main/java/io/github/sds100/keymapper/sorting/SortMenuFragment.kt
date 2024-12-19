package io.github.sds100.keymapper.sorting

import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSortMenuBinding
import io.github.sds100.keymapper.databinding.SortMenuChipBinding
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

        // Retrieve the sort priority from the view model and create a chip for each field
        sortViewModel.sortPriority.map(::getChipByField).forEach { chip ->
            binding.chipGroupSortBy.addView(chip.root)
        }

        binding.chipGroupSortBy.setOnDragListener(chipGroupOnDragListener)

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

    private var chipDragJob: Job? = null
    private val chipGroupOnDragListener = View.OnDragListener { chipGroup, dragEvent ->
        chipGroup as ChipGroup
        val dragView = dragEvent.localState as View

        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED,
            DragEvent.ACTION_DRAG_ENTERED,
            DragEvent.ACTION_DRAG_EXITED,
            -> true

            DragEvent.ACTION_DRAG_ENDED -> {
                dragView.visibility = View.VISIBLE
                true
            }

            DragEvent.ACTION_DRAG_LOCATION,
            DragEvent.ACTION_DROP,
            -> {
                val itemIndex = chipGroup.indexOfChild(dragView)
                val newIndex = chipGroup.children.indexOfFirst { child ->
                    val rect = Rect()
                    child.getHitRect(rect)
                    rect.contains(dragEvent.x.toInt(), dragEvent.y.toInt())
                }.takeIf { it != -1 } ?: return@OnDragListener true

                binding.chipGroupSortBy.apply {
                    // Delay the removal and addition of the views to prevent flickering
                    chipDragJob?.cancel()
                    chipDragJob = lifecycleScope.launch {
                        delay(30)

                        // Animation disabled because it is to clumsy

                        // Can't use `android:animateLayoutChanges = "true"`
                        // because bottom sheet will jump up and down when dragging chips
                        // https://stackoverflow.com/a/69829813
                        // TransitionManager.beginDelayedTransition(binding.chipGroupSortBy)

                        removeViewAt(itemIndex)
                        addView(dragView, newIndex)
                        sortViewModel.swapSortPriority(itemIndex, newIndex)
                    }
                }

                true
            }

            else -> false
        }
    }

    private fun getChipByField(field: SortField): SortMenuChipBinding {
        val chipBinding = SortMenuChipBinding.inflate(
            LayoutInflater.from(requireContext()),
            binding.chipGroupSortBy,
            false,
        )

        chipBinding.chip.setOnLongClickListener { view ->
            val clipShadow = View.DragShadowBuilder(view)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                @Suppress("DEPRECATION")
                view.startDrag(null, clipShadow, view, 0)
            } else {
                view.startDragAndDrop(null, clipShadow, view, 0)
            }

            view.visibility = View.INVISIBLE

            true
        }

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
        chipDragJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
