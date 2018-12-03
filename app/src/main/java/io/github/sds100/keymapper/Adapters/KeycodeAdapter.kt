package io.github.sds100.keymapper.Adapters

import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.AlphabeticalFilter
import io.github.sds100.keymapper.Delegates.ISimpleItemAdapter
import io.github.sds100.keymapper.Interfaces.OnItemClickListener
import io.github.sds100.keymapper.Utils.KeycodeUtils

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display all keycodes in a RecyclerView
 */
class KeycodeAdapter(
        override val onItemClickListener: OnItemClickListener<Int>,
        private var mKeyCodeList: List<Int> = KeycodeUtils.getKeyCodes()
) : BaseRecyclerViewAdapter<RecyclerView.ViewHolder>(), ISimpleItemAdapter<Int>, Filterable {

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = mKeyCodeList,
            onFilter = { filteredList ->
                mKeyCodeList = filteredList
                notifyDataSetChanged()
            },
            getItemText = { getItemText(it) }
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return super.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super<ISimpleItemAdapter>.onBindViewHolder(holder, position)
    }

    override fun getFilter() = mAlphabeticalFilter

    override fun getItemCount() = mKeyCodeList.size

    override fun getItem(position: Int) = mKeyCodeList[position]

    override fun getItemText(item: Int): String {
        return KeyEvent.keyCodeToString(item)
    }

    override fun getItemDrawable(item: Int): Drawable? = null
}