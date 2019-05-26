package io.github.sds100.keymapper.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.KeymapAdapterModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import io.github.sds100.keymapper.onSuccess
import io.github.sds100.keymapper.selection.SelectionCallback
import io.github.sds100.keymapper.selection.SelectionEvent
import io.github.sds100.keymapper.selection.SelectionProvider
import io.github.sds100.keymapper.util.FlagUtils
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.viewholder.SelectableViewHolder
import kotlinx.android.synthetic.main.keymap_adapter_item.view.*

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 * Display a list of [KeyMap]s in a RecyclerView
 */
class KeymapAdapter(private val mOnItemClickListener: OnItemClickListener<KeymapAdapterModel>
) : BaseRecyclerViewAdapter<KeymapAdapter.ViewHolder>(), SelectionCallback {

    val iSelectionProvider = SelectionProvider()

    var itemList: List<KeymapAdapterModel> = listOf()
        set(value) {
            iSelectionProvider.allItemIds = value.map { it.id }
            field = value
        }

    init {
        setHasStableIds(true)

        iSelectionProvider.subscribeToSelectionEvents(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.keymap_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val model = itemList[position]

        holder.itemView.apply {
            //only show the chechbox if the user is selecting items
            if (iSelectionProvider.inSelectingMode) {
                checkBox.visibility = View.VISIBLE
            } else {
                checkBox.isChecked = false
                checkBox.visibility = View.GONE
            }

            //only show the flag layout if the keymap has chosen flags.
            if (model.flags != 0) {
                flagsLayout.visibility = View.VISIBLE
                chipGroupFlags.removeAllViews()

                FlagUtils.getFlags(model.flags).forEach { flag ->

                    //if a label for a flag can be found, set the text of the chip to the flag
                    val chip = Chip(context).apply {
                        FlagUtils.getFlagLabel(flag).onSuccess {
                            text = str(it)
                        }
                    }

                    chipGroupFlags.addView(chip)
                }

            } else {
                flagsLayout.visibility = View.GONE
            }

            if (model.triggerList.isEmpty()) {
                triggerLayout.visibility = View.GONE

            } else {
                triggerLayout.visibility = View.VISIBLE
                //show all the triggers in a list
                val triggerAdapter = TriggerAdapter(model.triggerList.toMutableList(), showRemoveButton = false)

                recyclerViewTriggers.layoutManager = LinearLayoutManager(context)
                recyclerViewTriggers.adapter = triggerAdapter
            }

            checkBox.isChecked = iSelectionProvider.isSelected(holder.itemId)

            actionDescriptionLayout.setDescription(model.actionDescription)

            if (model.isEnabled) {
                textViewDisabled.visibility = View.GONE
            } else {
                textViewDisabled.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return itemList[position].id
    }

    override fun getItemCount() = itemList.size

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        //if the event affects only a single viewholder.
        if (id != null) {
            boundViewHolders.find { it.itemId == id }!!.onSelectionEvent(event)
        } else {
            boundViewHolders.forEach { viewHolder ->
                viewHolder.onSelectionEvent(event)
            }
        }
    }

    inner class ViewHolder(itemView: View) : SelectableViewHolder(iSelectionProvider, itemView) {

        private val mCheckBox = itemView.findViewById<CheckBox>(R.id.checkBox)!!

        init {
            mCheckBox.setOnClickListener {
                if (iSelectionProvider.inSelectingMode) {
                    iSelectionProvider.toggleSelection(itemId)
                }
            }
        }

        override fun onClick(v: View?) {
            super.onClick(v)

            if (!inSelectingMode) {
                mOnItemClickListener.onItemClick(itemList[adapterPosition])
            }
        }

        override fun onSelectionEvent(event: SelectionEvent) {
            when (event) {
                SelectionEvent.START -> {
                    mCheckBox.isChecked = false
                    mCheckBox.visibility = View.VISIBLE
                }

                SelectionEvent.STOP -> {
                    mCheckBox.isChecked = false
                    mCheckBox.visibility = View.GONE
                }

                SelectionEvent.SELECTED -> {
                    mCheckBox.isChecked = true
                }

                SelectionEvent.UNSELECTED -> {
                    mCheckBox.isChecked = false
                }

                SelectionEvent.SELECT_ALL -> {
                    mCheckBox.isChecked = true
                }
            }
        }
    }
}