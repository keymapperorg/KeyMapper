package io.github.sds100.keymapper.Adapters

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.AlphabeticalFilter
import io.github.sds100.keymapper.Delegates.ISectionedAdapter
import io.github.sds100.keymapper.Delegates.ISectionedAdapter.Companion.VIEW_TYPE_DEFAULT
import io.github.sds100.keymapper.Delegates.ISectionedAdapter.Companion.VIEW_TYPE_SECTION
import io.github.sds100.keymapper.Delegates.ISimpleItemAdapter
import io.github.sds100.keymapper.Interfaces.IContext
import io.github.sds100.keymapper.Interfaces.OnItemClickListener
import io.github.sds100.keymapper.SectionItem
import io.github.sds100.keymapper.SectionedItemList
import io.github.sds100.keymapper.SystemActionDef
import io.github.sds100.keymapper.Utils.SystemActionUtils
import io.github.sds100.keymapper.Utils.SystemActionUtils.SYSTEM_ACTION_DEFINITIONS

/**
 * Created by sds100 on 17/07/2018.
 */

class SystemActionAdapter(
        iContext: IContext,
        private val onItemClickListener: OnItemClickListener<SystemActionDef>
) : BaseRecyclerViewAdapter<RecyclerView.ViewHolder>(),
        ISectionedAdapter<SystemActionDef>, ISimpleItemAdapter<SystemActionDef>, Filterable, IContext by iContext {

    override val sectionedItemList: SectionedItemList<SystemActionDef> by lazy {
        val list = SectionedItemList<SystemActionDef>()

        SYSTEM_ACTION_DEFINITIONS.forEachIndexed { i, systemAction ->
            fun getCategoryLabel(): String {
                val resId = SystemActionUtils.CATEGORY_LABEL_MAP[systemAction.category]
                        ?: throw Exception("That system action category id isn't mapped to a label")

                return ctx.getString(resId)
            }

            //if at the end of the list, the next item can't be compared
            if (i == 0 || systemAction.category != SYSTEM_ACTION_DEFINITIONS[i - 1].category) {
                val section = SectionItem(getCategoryLabel())

                list.addSection(section)
            }

            list.addItem(systemAction)
        }

        return@lazy list
    }

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = sectionedItemList.itemList,

            onFilter = { filteredList ->
                filtering = true
                mFilteredList = filteredList
                notifyDataSetChanged()
            },

            getItemText = { getItemText(it) }
    )

    private var filtering = false
    private var mFilteredList: List<SystemActionDef> = listOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SECTION -> super<ISectionedAdapter>.onCreateViewHolder(parent, viewType)
            VIEW_TYPE_DEFAULT -> super<ISimpleItemAdapter>.onCreateViewHolder(parent, viewType)

            else -> throw Exception("This view type's delegate has no delegate")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super<BaseRecyclerViewAdapter>.onBindViewHolder(holder, position)

        when (holder.itemViewType) {
            VIEW_TYPE_SECTION -> super<ISectionedAdapter>.onBindViewHolder(holder, position)
            VIEW_TYPE_DEFAULT -> super<ISimpleItemAdapter>.onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        if (filtering) {
            return mFilteredList.size
        } else {
            return sectionedItemList.size
        }
    }

    override fun getItem(position: Int): SystemActionDef {
        if (filtering) {
            return mFilteredList[position]
        } else {
            return sectionedItemList.getItem(position)
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (filtering) return VIEW_TYPE_DEFAULT

        if (sectionedItemList.isSectionAtIndex(position)) {
            return VIEW_TYPE_SECTION
        } else {
            return VIEW_TYPE_DEFAULT
        }
    }

    override fun onItemClick(position: Int) {
        onItemClickListener.onItemClick(getItem(position))
    }

    override fun getFilter() = mAlphabeticalFilter

    override fun getItemText(item: SystemActionDef): String {
        return ctx.getString(item.descriptionRes)
    }

    override fun getItemDrawable(item: SystemActionDef): Drawable? {
        if (item.iconRes == null) return null
        return ContextCompat.getDrawable(ctx, item.iconRes)
    }
}