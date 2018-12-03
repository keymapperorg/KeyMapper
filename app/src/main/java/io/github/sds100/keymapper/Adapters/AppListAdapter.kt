package io.github.sds100.keymapper.Adapters

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.AlphabeticalFilter
import io.github.sds100.keymapper.Delegates.ISimpleItemAdapter
import io.github.sds100.keymapper.Interfaces.OnItemClickListener

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
) : BaseRecyclerViewAdapter<RecyclerView.ViewHolder>(), ISimpleItemAdapter<ApplicationInfo>, Filterable {

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = mAppList,
            onFilter = { filteredList ->
                mAppList = filteredList
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

    override fun getItemCount() = mAppList.size

    override fun getItem(position: Int) = mAppList[position]

    override fun getItemDrawable(item: ApplicationInfo): Drawable? = item.loadIcon(mPackageManager)

    override fun getItemText(item: ApplicationInfo) = item.loadLabel(mPackageManager).toString()
}