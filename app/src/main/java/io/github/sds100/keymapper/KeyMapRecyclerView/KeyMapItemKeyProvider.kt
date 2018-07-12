package io.github.sds100.keymapper.KeyMapRecyclerView

import androidx.recyclerview.selection.ItemKeyProvider
import io.github.sds100.keymapper.KeyMap

/**
 * Created by sds100 on 12/07/2018.
 */

/**
 *
 */
class KeyMapItemKeyProvider(scope: Int, private val mItemList: List<KeyMap>
) : ItemKeyProvider<Long>(scope) {

    override fun getKey(position: Int) = mItemList[position].id

    override fun getPosition(key: Long) = mItemList.indexOfFirst { it.id == key }
}