package io.github.sds100.keymapper.util.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.sds100.keymapper.R
import kotlin.math.floor

/**
 * Created by sds100 on 26/07/2021.
 */

object RecyclerViewUtils {
    fun applySimpleListItemDecorations(recyclerView: RecyclerView) {
        val itemPadding = recyclerView.resources.getDimensionPixelSize(R.dimen.grid_padding)

        recyclerView.setPadding(itemPadding, itemPadding, itemPadding, itemPadding)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.set(itemPadding, itemPadding, itemPadding, itemPadding)
            }
        })
    }

    /**
     * @return the span count
     */
    fun setSpanCountForSimpleListItemGrid(recyclerView: RecyclerView) {
        val gridItemMinWidth =
            recyclerView.resources.getDimensionPixelSize(R.dimen.grid_item_min_width).toDouble()

        val recyclerViewWidth = recyclerView.measuredWidth.toDouble()

        val calculatedSpanCount = floor(recyclerViewWidth / gridItemMinWidth).toInt()

        val spanCount = if (calculatedSpanCount < 1) {
            1
        } else {
            calculatedSpanCount
        }

        (recyclerView.layoutManager as GridLayoutManager).apply {
            this.spanCount = spanCount
        }
    }
}