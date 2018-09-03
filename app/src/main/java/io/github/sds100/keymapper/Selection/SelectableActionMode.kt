package io.github.sds100.keymapper.Selection

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 12/08/2018.
 */
class SelectableActionMode(
        private val mCtx: Context,
        private val mISelectionProvider: ISelectionProvider
) : ActionMode.Callback, SelectionCallback {

    private var mActionMode: ActionMode? = null

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode!!.menuInflater.inflate(R.menu.menu_multi_select, menu)

        mActionMode = mode
        mISelectionProvider.subscribeToSelectionEvents(this)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item!!.itemId) {
            android.R.id.home -> {
                mode!!.finish()
                mISelectionProvider.stopSelecting()
                mISelectionProvider.unsubscribeToSelectionEvents(this)
            }
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        //set the title to the number of items selected. E.g 10 Selected
        mode!!.title = mCtx.getString(
                R.string.selection_count,
                mISelectionProvider.selectionCount)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        mActionMode = null
        mISelectionProvider.unsubscribeToSelectionEvents(this)
        mISelectionProvider.stopSelecting()
    }

    override fun onItemSelected(id: Long) {
        mActionMode?.invalidate()
    }

    override fun onItemUnselected(id: Long) {
        mActionMode?.invalidate()
    }

    override fun onStopMultiSelect() {}
    override fun onStartMultiSelect() {}
}