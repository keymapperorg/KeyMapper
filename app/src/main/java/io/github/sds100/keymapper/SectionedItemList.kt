package io.github.sds100.keymapper

import androidx.collection.SparseArrayCompat
import kotlin.math.max

/**
 * Created by sds100 on 02/12/2018.
 */

class SectionedItemList<T> {
    private val mSectionList = SparseArrayCompat<SectionItem>()
    private val mItemList = SparseArrayCompat<T>()

    val itemList: List<T>
        get() =
            sequence {
                for (i in 0 until mItemList.size()) {
                    yield(mItemList.valueAt(i)!!)
                }
            }.toList()

    val size: Int
        get() = mSectionList.size() + mItemList.size()

    fun isSectionAtIndex(index: Int): Boolean {
        return mSectionList.containsKey(index)
    }

    fun addItem(item: T) {
        mItemList.apply {
            put(getMaxPosition() + 1, item)
        }
    }

    fun addSection(sectionItem: SectionItem) {
        mSectionList.apply {
            put(getMaxPosition() + 1, sectionItem)
        }
    }

    fun getItem(index: Int): T {
        return mItemList.get(index)!!
    }

    fun getSection(index: Int): SectionItem {
        return mSectionList.get(index)!!
    }

    private fun getMaxPosition(): Int {
        val maxSectionPosition = if (mSectionList.isEmpty) {
            -1
        } else {
            mSectionList.keyAt(mSectionList.size() - 1)
        }

        val maxItemPosition = if (mItemList.isEmpty) {
            -1
        } else {
            mItemList.keyAt(mItemList.size() - 1)
        }

        return max(maxSectionPosition, maxItemPosition)
    }
}