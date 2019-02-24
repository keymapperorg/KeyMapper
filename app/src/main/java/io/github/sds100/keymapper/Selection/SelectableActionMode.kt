package io.github.sds100.keymapper.Selection

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.view.ActionMode
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 12/08/2018.
 */
class SelectableActionMode(
        private val mCtx: Context,
        @MenuRes private val mMenuId: Int,
        private val mISelectionProvider: ISelectionProvider,
        private val mOnMenuItemClickListener: MenuItem.OnMenuItemClickListener
) : ActionMode.Callback, SelectionCallback {

    private var mActionMode: ActionMode? = null

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode!!.menuInflater.inflate(mMenuId, menu)

        mActionMode = mode
        mISelectionProvider.subscribeToSelectionEvents(this)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item!!.itemId) {
            //when back button is pressed
            android.R.id.home -> onDestroyActionMode(mode!!)

            R.id.action_select_all -> {
                mISelectionProvider.selectAll()
                return true
            }
        }

        mOnMenuItemClickListener.onMenuItemClick(item)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        //set the title to the number of items selected. E.g "10 Selected"
        mode!!.title = mCtx.getString(
                R.string.selection_count,
                mISelectionProvider.selectionCount)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mode.finish()
        mActionMode = null
        mISelectionProvider.unsubscribeFromSelectionEvents(this)
        mISelectionProvider.stopSelecting()
    }

    override fun onSelectionEvent(id: Long?, event: SelectionEvent) {
        mActionMode?.invalidate()

        if (event == SelectionEvent.STOP) {
            onDestroyActionMode(mActionMode!!)
        }
    }
}