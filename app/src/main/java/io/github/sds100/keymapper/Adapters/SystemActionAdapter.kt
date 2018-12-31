package io.github.sds100.keymapper.Adapters

import android.graphics.drawable.Drawable
import android.widget.Filterable
import com.hannesdorfmann.adapterdelegates4.AbsDelegationAdapter
import io.github.sds100.keymapper.AlphabeticalFilter
import io.github.sds100.keymapper.Delegates.SectionedAdapterDelegate
import io.github.sds100.keymapper.Delegates.SectionedAdapterDelegate.Companion.VIEW_TYPE_SECTION
import io.github.sds100.keymapper.Delegates.SimpleItemAdapterDelegate
import io.github.sds100.keymapper.Interfaces.IContext
import io.github.sds100.keymapper.Interfaces.ISimpleItemAdapter
import io.github.sds100.keymapper.Interfaces.OnItemClickListener
import io.github.sds100.keymapper.SectionItem
import io.github.sds100.keymapper.SystemActionDef
import io.github.sds100.keymapper.Utils.SystemActionUtils
import io.github.sds100.keymapper.Utils.drawable
import io.github.sds100.keymapper.Utils.str

/**
 * Created by sds100 on 17/07/2018.
 */

class SystemActionAdapter(
        iContext: IContext,
        onItemClickListener: OnItemClickListener<SystemActionDef>
) : AbsDelegationAdapter<List<Any>>(),
        ISimpleItemAdapter<Any>,
        Filterable,
        IContext by iContext {

    @Suppress("UNCHECKED_CAST")
    override val onItemClickListener = onItemClickListener as OnItemClickListener<Any>

    private val mSystemActionDefinitions = SystemActionUtils.getSystemActionDefinitions()

    private val mAlphabeticalFilter = AlphabeticalFilter(
            mOriginalList = mSystemActionDefinitions,

            onFilter = { filteredList ->
                filtering = true
                setItems(filteredList)
                notifyDataSetChanged()
            },

            getItemText = { getItemText(it) }
    )

    private var filtering = false

    init {
        val sectionedDelegate = SectionedAdapterDelegate()
        val simpleItemDelegate = SimpleItemAdapterDelegate(this)

        delegatesManager
                .addDelegate(VIEW_TYPE_SECTION, sectionedDelegate)
                .addDelegate(simpleItemDelegate)

        setItems(createSystemActionDefListWithCategories())
    }

    override fun getFilter() = mAlphabeticalFilter

    override fun getItem(position: Int): Any? {
        val item = items[position]

        if (item is SectionItem) {
            return null
        } else {
            return item
        }
    }

    override fun getItemText(item: Any): String {
        return str((item as SystemActionDef).descriptionRes)
    }

    override fun getItemCount() = items.size

    override fun getItemDrawable(item: Any): Drawable? {
        if ((item as SystemActionDef).iconRes == null) return null

        return drawable(item.iconRes!!)
    }

    private fun createSystemActionDefListWithCategories(): List<Any> {
        return sequence {
            mSystemActionDefinitions.forEachIndexed { i, systemAction ->
                fun getCategoryLabel(): String {
                    val resId = SystemActionUtils.CATEGORY_LABEL_MAP[systemAction.category]
                            ?: throw Exception("That system action category id isn't mapped to a label. " +
                                    "id: ${systemAction.category}")

                    return str(resId)
                }

                //if at the end of the list, the next item can't be compared
                if (i == 0 || systemAction.category != mSystemActionDefinitions[i - 1].category) {
                    val section = SectionItem(getCategoryLabel())

                    yield(section)
                }

                yield(systemAction)
            }
        }.toList()
    }
}