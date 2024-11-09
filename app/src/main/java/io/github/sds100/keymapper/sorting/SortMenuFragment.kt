package io.github.sds100.keymapper.sorting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSortMenuBinding
import io.github.sds100.keymapper.databinding.SortMenuItemBinding
import io.github.sds100.keymapper.mappings.keymaps.KeyMapField
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SortMenuFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSortMenuBinding? = null
    private val binding get() = _binding!!

    private val sortViewModel: SortViewModel by activityViewModels {
        Inject.sortViewModel(requireContext())
    }

    private lateinit var adapter: SortAdapter

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

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0,
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ): Boolean {
                val fromPosition = viewHolder.absoluteAdapterPosition
                val toPosition = target.absoluteAdapterPosition
                sortViewModel.setLocalSortOrder(fromPosition, toPosition)
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
            ) {
                super.clearView(recyclerView, viewHolder)
                sortViewModel.saveSortOrder()
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        adapter = SortAdapter(
            items = emptyList(),
            onItemToggle = sortViewModel::toggleField,
            itemTouchHelper = itemTouchHelper,
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            sortViewModel.sortState.collectLatest {
                adapter.update(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class KeyMapFieldSortOrder(
    val field: KeyMapField,
    var order: SortOrder,
)

class SortAdapter(
    private var items: List<KeyMapFieldSortOrder>,
    private val onItemToggle: (KeyMapField) -> Unit,
    private val itemTouchHelper: ItemTouchHelper,
) : RecyclerView.Adapter<SortAdapter.SortViewHolder>() {
    fun update(newState: SortState) {
        val oldItems = items
        items = newState.fieldsOrder.map { KeyMapFieldSortOrder(it, newState.getSortOrder(it)) }

        // If the old list is empty it means that the adapter has just been created
        if (oldItems.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        // Update only fields which order has changed
        items.forEachIndexed { index, item ->
            // Find the index of the item in the old list
            oldItems.first { it.field == item.field }.let {
                // If the order has changed, notify the adapter
                if (it.order != item.order) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortViewHolder {
        SortMenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
            return SortViewHolder(this.root, this)
        }
    }

    override fun onBindViewHolder(holder: SortViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SortViewHolder(
        itemView: View,
        private val binding: SortMenuItemBinding,
    ) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: KeyMapFieldSortOrder) {
            val header = when (item.field) {
                KeyMapField.TRIGGER -> R.string.trigger_header
                KeyMapField.ACTIONS -> R.string.action_list_header
                KeyMapField.CONSTRAINTS -> R.string.constraint_list_header
                KeyMapField.OPTIONS -> R.string.option_list_header
            }
            binding.sortMenuItemName.text = binding.root.context.resources.getString(header)

            binding.isAscending = item.order == SortOrder.ASCENDING

            binding.onClickListener = View.OnClickListener { onItemToggle(item.field) }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    itemTouchHelper.startDrag(this)
                }

                false
            }
        }
    }
}
