package io.github.sds100.keymapper.adapter

import android.graphics.drawable.Drawable
import android.view.KeyEvent
import android.widget.Filterable
import com.hannesdorfmann.adapterdelegates4.AbsDelegationAdapter
import io.github.sds100.keymapper.AlphabeticalFilter
import io.github.sds100.keymapper.delegate.SimpleItemAdapterDelegate
import io.github.sds100.keymapper.interfaces.ISimpleItemAdapter
import io.github.sds100.keymapper.interfaces.OnItemClickListener
import io.github.sds100.keymapper.util.KeycodeUtils

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display all keycodes in a RecyclerView
 */
class KeycodeAdapter(
        override val onItemClickListener: OnItemClickListener<Int>,
        private var mKeyCodeList: List<Int> = KeycodeUtils.getKeyCodes()
) : AbsDelegationAdapter<List<Int>>(), ISimpleItemAdapter<Int>, Filterable {

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = mKeyCodeList,
            onFilter = { filteredList ->
                mKeyCodeList = filteredList
                notifyDataSetChanged()
            },
            getItemText = { getItemText(it) }
    )

    init {
        val simpleItemDelegate = SimpleItemAdapterDelegate(this)
        delegatesManager.addDelegate(simpleItemDelegate)

        setItems(mKeyCodeList)
    }
    override fun getFilter() = mAlphabeticalFilter

    override fun getItemCount() = mKeyCodeList.size

    override fun getItem(position: Int) = mKeyCodeList[position]

    override fun getItemText(item: Int): String {
        return KeyEvent.keyCodeToString(item)
    }

    override fun getItemDrawable(item: Int): Drawable? = null
}