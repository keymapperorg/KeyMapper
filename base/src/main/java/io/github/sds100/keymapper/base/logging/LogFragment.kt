package io.github.sds100.keymapper.base.logging

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.TypedEpoxyController
import com.michaelflisar.dragselectrecyclerview.DragSelectTouchListener
import com.michaelflisar.dragselectrecyclerview.DragSelectionProcessor
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.base.logEntry
import io.github.sds100.keymapper.base.utils.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.ui.showPopups
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.files.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class LogFragment : SimpleRecyclerViewFragment<LogEntryListItem>() {

    private val viewModel: LogViewModel by viewModels()

    override val listItems: Flow<State<List<LogEntryListItem>>>
        get() = viewModel.listItems

    override val appBarMenu: Int = R.menu.menu_log
    override var isAppBarVisible = true

    private val recyclerViewController by lazy { RecyclerViewController() }

    private val saveLogToFileLauncher =
        registerForActivityResult(CreateDocument(FileUtils.MIME_TYPE_TEXT)) {
            it ?: return@registerForActivityResult

            viewModel.onPickFileToSaveTo(it.toString())

            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
        }

    private lateinit var dragSelectTouchListener: DragSelectTouchListener

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showPopups(this, binding)

        getBottomAppBar(binding)?.setOnMenuItemClickListener { menuItem ->
            viewModel.onMenuItemClick(menuItem.itemId)
            true
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.pickFileToSaveTo.collectLatest {
                saveLogToFileLauncher.launch(LogUtils.createLogFileName())
            }
        }
    }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.appBarState.collectLatest { appBarState ->
                when (appBarState) {
                    LogAppBarState.MULTI_SELECTING -> {
                        binding.appBar.setNavigationIcon(R.drawable.ic_outline_clear_24)
                    }

                    LogAppBarState.NORMAL -> {
                        binding.appBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
                    }
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.goBack.collectLatest {
                findNavController().navigateUp()
            }
        }

        val dragSelectionProcessor = DragSelectionProcessor(viewModel.dragSelectionHandler)
            .withMode(DragSelectionProcessor.Mode.Simple)

        dragSelectTouchListener = DragSelectTouchListener()
            .withSelectListener(dragSelectionProcessor)

        binding.epoxyRecyclerView.setController(recyclerViewController)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<LogEntryListItem>) {
        recyclerViewController.setData(listItems)
    }

    private inner class RecyclerViewController : TypedEpoxyController<List<LogEntryListItem>>() {
        private var scrollToBottom = false
        private var scrolledToBottomInitially = false
        private var recyclerView: RecyclerView? = null

        init {
            addModelBuildListener {
                currentData?.also { currentData ->
                    if (!scrolledToBottomInitially) {
                        recyclerView?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                            ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                recyclerView?.scrollToPosition(currentData.size - 1)
                                recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                            }
                        })

                        scrolledToBottomInitially = true
                    } else if (scrollToBottom) {
                        recyclerView?.smoothScrollToPosition(currentData.size)
                    }
                }
            }
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            this.recyclerView = recyclerView
            super.onAttachedToRecyclerView(recyclerView)
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            this.recyclerView = null
            super.onDetachedFromRecyclerView(recyclerView)
        }

        override fun buildModels(data: List<LogEntryListItem>?) {
            if (data == null) {
                return
            }

            if (recyclerView?.scrollState != RecyclerView.SCROLL_STATE_SETTLING) {
                // only automatically scroll to the bottom if the recyclerview is already scrolled to the button
                val layoutManager = recyclerView?.layoutManager as LinearLayoutManager?

                if (layoutManager != null) {
                    val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                    if (lastVisibleItemPosition == RecyclerView.NO_POSITION) {
                        scrollToBottom = false
                    } else {
                        scrollToBottom = lastVisibleItemPosition == layoutManager.itemCount - 1
                    }
                }
            }

            data.forEachIndexed { index, model ->
                logEntry {
                    id(model.id)
                    model(model)

                    onClick { _ ->
                        viewModel.onListItemClick(model.id)
                    }

                    onLongClick { _ ->
                        dragSelectTouchListener.startDragSelection(index)
                        true
                    }
                }
            }
        }
    }
}
