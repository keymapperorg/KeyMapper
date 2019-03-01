package io.github.sds100.keymapper.adapter

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
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
 * Display apps in a RecyclerView
 */
class AppListAdapter(
        override val onItemClickListener: OnItemClickListener<ApplicationInfo>,
        private var mAppList: List<ApplicationInfo>,
        private val mPackageManager: PackageManager
) : AbsDelegationAdapter<List<ApplicationInfo>>(), ISimpleItemAdapter<ApplicationInfo>, Filterable {

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = mAppList,
            onFilter = { filteredList ->
                mAppList = filteredList
                notifyDataSetChanged()
            },
            getItemText = { getItemText(it) }
    )

    init {
        val simpleItemDelegate = SimpleItemAdapterDelegate(this)
        delegatesManager.addDelegate(simpleItemDelegate)

        setItems(mAppList)
    }

    override fun getFilter() = mAlphabeticalFilter

    override fun getItemCount() = mAppList.size

    override fun getItem(position: Int) = mAppList[position]

    override fun getItemDrawable(item: ApplicationInfo): Drawable? = item.loadIcon(mPackageManager)

    override fun getItemText(item: ApplicationInfo) = item.loadLabel(mPackageManager).toString()
}