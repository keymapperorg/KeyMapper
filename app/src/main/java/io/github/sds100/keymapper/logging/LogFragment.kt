package io.github.sds100.keymapper.logging

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.logEntry
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 13/05/2021.
 */
class LogFragment : SimpleRecyclerViewFragment<LogEntryListItem>() {

    private val viewModel by viewModels<LogViewModel> {
        Inject.logViewModel(requireContext())
    }

    override val listItems: Flow<State<List<LogEntryListItem>>>
        get() = viewModel.listItems

    override val appBarMenu: Int = R.menu.menu_log
    override var isAppBarVisible = true

    private val saveLogToFileLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            viewModel.onPickFileToSaveTo(it.toString())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showPopups(this, binding)

        getBottomAppBar(binding)?.setOnMenuItemClickListener { menuItem ->
            viewModel.onMenuItemClick(menuItem.itemId)
            true
        }

        addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.pickFileToSaveTo.collectLatest {
                saveLogToFileLauncher.launch(LogUtils.createLogFileName())
            }
        }
    }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<LogEntryListItem>) {

        //only automatically scroll to the bottom if the recyclerview is already scrolled to the button
        val layoutManager = recyclerView.layoutManager
        val scrollToBottom: Boolean

        if (layoutManager != null) {
            val lastVisibleItemPosition =
                (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

            if (lastVisibleItemPosition == RecyclerView.NO_POSITION) {
                scrollToBottom = false
            } else {
                scrollToBottom =
                    lastVisibleItemPosition == layoutManager.itemCount - 1
            }
        } else {
            scrollToBottom = false
        }

        recyclerView.withModels {
            listItems.forEach { model ->
                logEntry {
                    id(model.id)
                    model(model)
                }
            }
        }

        if (scrollToBottom && listItems.isNotEmpty()) {
            recyclerView.smoothScrollToPosition(listItems.lastIndex)
        }
    }
}