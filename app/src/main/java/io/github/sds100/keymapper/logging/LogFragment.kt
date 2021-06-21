package io.github.sds100.keymapper.logging

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.TypedEpoxyController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
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

    private val recyclerViewController by lazy { RecyclerViewController() }

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

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.pickFileToSaveTo.collectLatest {
                saveLogToFileLauncher.launch(LogUtils.createLogFileName())
            }
        }
    }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        binding.epoxyRecyclerView.setController(recyclerViewController)
    }


    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<LogEntryListItem>) {
        recyclerViewController.setData(listItems)
    }

    private inner class RecyclerViewController : TypedEpoxyController<List<LogEntryListItem>>() {
        private var scrollToBottom = false
        private var scrolledToBottomInitially = false

        init {
            addModelBuildListener {
                currentData?.also { currentData ->
                    if (scrollToBottom || !scrolledToBottomInitially) {
                        if (!scrolledToBottomInitially) {
                            binding.epoxyRecyclerView.doOnNextLayout {
                                binding.epoxyRecyclerView.smoothScrollToPosition(currentData.size)
                            }
                        }else{
                            binding.epoxyRecyclerView.smoothScrollToPosition(currentData.size)
                        }

                        scrolledToBottomInitially = true
                    }
                }
            }
        }

        override fun buildModels(data: List<LogEntryListItem>?) {
            if (data == null) {
                return
            }

            if (binding.epoxyRecyclerView.scrollState != RecyclerView.SCROLL_STATE_SETTLING) {
                //only automatically scroll to the bottom if the recyclerview is already scrolled to the button
                val layoutManager = binding.epoxyRecyclerView.layoutManager as LinearLayoutManager?

                if (layoutManager != null) {
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItemPosition == RecyclerView.NO_POSITION) {
                        scrollToBottom = false
                    } else {
                        scrollToBottom = lastVisibleItemPosition == layoutManager.itemCount - 1
                    }
                }
            }

            data.forEach { model ->
                logEntry {
                    id(model.id)
                    model(model)
                }
            }
        }
    }
}