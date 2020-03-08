package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.BuildConfig
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
import io.github.sds100.keymapper.databinding.FragmentKeymapListBinding
import io.github.sds100.keymapper.keymap
import io.github.sds100.keymapper.ui.callback.ActionErrorClickCallback
import io.github.sds100.keymapper.ui.callback.SelectionCallback
import io.github.sds100.keymapper.util.ISelectionProvider
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.worker.SeedDatabaseWorker
import kotlinx.android.synthetic.main.fragment_keymap_list.*
import splitties.experimental.ExperimentalSplittiesApi
import splitties.resources.color
import splitties.snackbar.action
import splitties.snackbar.longSnack

/**
 * A placeholder fragment containing a simple view.
 */
@ExperimentalSplittiesApi
class KeymapListFragment : Fragment() {

    private val mViewModel: KeymapListViewModel by viewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    private val selectionProvider: ISelectionProvider
        get() = mViewModel.selectionProvider

    private var mController = KeymapController()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentKeymapListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@KeymapListFragment
            viewModel = mViewModel
            epoxyRecyclerView.adapter = mController.adapter

            setOnNewKeymapClick {
                val direction = KeymapListFragmentDirections.actionHomeToNewKeymap()
                findNavController().navigate(direction)
            }

            appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_toggle_dark_theme -> {

                        lifecycleScope.launchWhenCreated {
                            AppPreferences().toggleDarkThemeMode()
                        }

                        true
                    }

                    R.id.action_seed_database -> {
                        val request = OneTimeWorkRequestBuilder<SeedDatabaseWorker>().build()
                        WorkManager.getInstance(requireContext()).enqueue(request)
                        true
                    }

                    R.id.action_select_all -> {
                        selectionProvider.selectAll()
                        true
                    }

                    else -> false
                }
            }

            appBar.setNavigationOnClickListener {
                if (selectionProvider.isSelectable.value == true) {
                    selectionProvider.stopSelecting()
                }
            }

            setOnConfirmSelectionClick {
                mViewModel.delete(*mViewModel.selectionProvider.selectedIds)
                mViewModel.selectionProvider.stopSelecting()
            }
        }

        mViewModel.apply {

            keymapList.observe(viewLifecycleOwner) { keymapList ->
                mController.keymapList = keymapList
            }

            selectionProvider.apply {

                isSelectable.observe(viewLifecycleOwner, Observer { isSelectable ->
                    mController.requestModelBuild()

                    if (isSelectable) {
                        appBar.replaceMenu(R.menu.menu_multi_select)
                    } else {
                        appBar.replaceMenu(R.menu.menu_keymap_list)

                        // only show the button to seed the database in debug builds.
                        appBar.menu.findItem(R.id.action_seed_database).isVisible = BuildConfig.DEBUG
                    }
                })

                callback = mController
            }
        }

        return binding.root
    }

    inner class KeymapController : EpoxyController(), SelectionCallback {
        var keymapList: List<KeymapListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            keymapList.forEach {
                keymap {
                    id(it.id)
                    isSelectable(selectionProvider.isSelectable.value)
                    isSelected(selectionProvider.isSelected(it.id))
                    isEnabled(it.isEnabled)
                    actions(it.actionList)
                    trigger(it.triggerModel)
                    flags(it.flagList)

                    onActionErrorClick(object : ActionErrorClickCallback {

                        override fun onActionErrorClick(failure: Failure) {
                            coordinatorLayout.longSnack(failure.errorMessage) {

                                //only add an action to fix the error if the error can be recovered from
                                if (failure is RecoverableFailure) {
                                    action(R.string.snackbar_fix) {
                                        failure.recover(requireContext())
                                    }
                                }

                                setAnchorView(R.id.fab)
                                show()
                            }
                        }
                    })

                    onClick { _ ->
                        val id = it.id

                        if (selectionProvider.isSelectable.value == true) {
                            selectionProvider.toggleSelection(id)
                        } else {
                            val direction = KeymapListFragmentDirections.actionHomeToConfigKeymap(id)
                            findNavController().navigate(direction)
                        }
                    }

                    onLongClick { _ ->
                        selectionProvider.run {
                            val startedSelecting = startSelecting()

                            if (startedSelecting) {
                                toggleSelection(it.id)
                            }

                            startedSelecting
                        }
                    }
                }
            }
        }

        override fun onSelect(id: Long) {
            requestModelBuild()
        }

        override fun onUnselect(id: Long) {
            requestModelBuild()
        }

        override fun onSelectAll() {
            requestModelBuild()
        }
    }
}