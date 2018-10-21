package io.github.sds100.keymapper.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.OnItemClickListener
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Selection.SelectionEvent
import io.github.sds100.keymapper.Selection.SelectionEvent.*
import io.github.sds100.keymapper.Utils.ActionUtils
import kotlinx.android.synthetic.main.keymap_adapter_item.view.*

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 * Display a list of [KeyMap]s in a RecyclerView
 */
class KeymapAdapter(private val mOnItemClickListener: OnItemClickListener<KeyMap>
) : SelectableAdapter<KeyMap, KeymapAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return ViewHolder(inflater.inflate(R.layout.keymap_adapter_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        val keyMap = itemList[position]

        holder.itemView.apply {
            //only show the chechbox if the user is selecting items
            if (iSelectionProvider.inSelectingMode) {
                checkBox.visibility = View.VISIBLE
            } else {
                checkBox.isChecked = false
                checkBox.visibility = View.GONE
            }

            checkBox.isChecked = iSelectionProvider.isSelected(holder.itemId)

            //display information about the action to the user
            val actionDescription = ActionUtils.getDescription(context, keyMap.action)
            actionDescriptionLayout.setDescription(actionDescription)

            //show all the triggers in a list
            val triggerAdapter = TriggerAdapter(keyMap.triggerList, showRemoveButton = false)

            recyclerViewTriggers.layoutManager = LinearLayoutManager(context)
            recyclerViewTriggers.adapter = triggerAdapter
        }
    }

    override fun getItemId(position: Int): Long {
        return itemList[position].id
    }

    inner class ViewHolder(itemView: View)
        : SelectableAdapter<KeyMap, ViewHolder>.ViewHolder(itemView) {
        private val mCheckBox = itemView.findViewById<CheckBox>(R.id.checkBox)!!

        init {
            mCheckBox.setOnClickListener {
                if (iSelectionProvider.inSelectingMode) {
                    iSelectionProvider.toggleSelection(getItemId(adapterPosition))
                }
            }
        }

        override fun onClick(v: View?) {
            super.onClick(v)

            if (!iSelectionProvider.inSelectingMode) {
                mOnItemClickListener.onItemClick(itemList[adapterPosition])
            }
        }

        override fun onSelectionEvent(event: SelectionEvent) {
            when (event) {
                START -> {
                    mCheckBox.isChecked = false
                    mCheckBox.visibility = View.VISIBLE
                }

                STOP -> {
                    mCheckBox.isChecked = false
                    mCheckBox.visibility = View.GONE
                }

                SELECTED -> {
                    mCheckBox.isChecked = true
                }

                UNSELECTED -> {
                    mCheckBox.isChecked = false
                }

                SELECT_ALL -> {
                    mCheckBox.isChecked = true
                }
            }
        }

    }
}