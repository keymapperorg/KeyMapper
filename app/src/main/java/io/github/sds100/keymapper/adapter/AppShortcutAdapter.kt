package io.github.sds100.keymapper.adapter

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.widget.Filterable
import com.hannesdorfmann.adapterdelegates4.AbsDelegationAdapter
import io.github.sds100.keymapper.AlphabeticalFilter
import io.github.sds100.keymapper.delegate.SimpleItemAdapterDelegate
import io.github.sds100.keymapper.interfaces.ISimpleItemAdapter
import io.github.sds100.keymapper.interfaces.OnItemClickListener

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display app shortcuts in a RecyclerView
 */
class AppShortcutAdapter(
        override val onItemClickListener: OnItemClickListener<ResolveInfo>,
        private var mAppShortcutList: List<ResolveInfo>,
        private val mPackageManager: PackageManager
) : AbsDelegationAdapter<List<ResolveInfo>>(), ISimpleItemAdapter<ResolveInfo>, Filterable {

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = mAppShortcutList,
            onFilter = { filteredList ->
                mAppShortcutList = filteredList
                notifyDataSetChanged()
            },
            getItemText = { getItemText(it) }
    )

    init {
        val simpleItemDelegate = SimpleItemAdapterDelegate(this)
        delegatesManager.addDelegate(simpleItemDelegate)

        setItems(mAppShortcutList)
    }

    override fun getFilter() = mAlphabeticalFilter

    override fun getItemCount() = mAppShortcutList.size

    override fun getItem(position: Int) = mAppShortcutList[position]

    override fun getItemText(item: ResolveInfo): String {
        return item.loadLabel(mPackageManager).toString()
    }

    override fun getItemDrawable(item: ResolveInfo): Drawable? {
        return item.loadIcon(mPackageManager)
    }
}