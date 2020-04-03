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
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
import io.github.sds100.keymapper.databinding.FragmentKeymapListBinding
import io.github.sds100.keymapper.keymapSimple
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.ui.callback.SelectionCallback
import io.github.sds100.keymapper.util.ISelectionProvider
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.PermissionUtils
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.worker.SeedDatabaseWorker
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
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

    private val mController = KeymapController()

    private lateinit var mBinding: FragmentKeymapListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        FragmentKeymapListBinding.inflate(inflater, container, false).apply {
            mBinding = this
            lifecycleOwner = this@KeymapListFragment
            viewModel = mViewModel
            epoxyRecyclerView.adapter = mController.adapter

            setOnNewKeymapClick {
                val direction =
                    KeymapListFragmentDirections.actionHomeToConfigKeymap(ConfigKeymapViewModel.NEW_KEYMAP_ID)
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

                    R.id.action_help -> {
                        findNavController().navigate(R.id.action_global_helpFragment)

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

                    R.id.action_enable -> {
                        mViewModel.enableKeymaps(*selectionProvider.selectedIds)
                        true
                    }

                    R.id.action_disable -> {
                        mViewModel.disableKeymaps(*selectionProvider.selectedIds)
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
                selectionProvider.stopSelecting()
            }

            mViewModel.apply {

                keymapModelList.observe(viewLifecycleOwner) { keymapList ->
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

            return this.root
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionUtils.REQUEST_CODE_PERMISSION) {
            mViewModel.rebuildModels()
        }
    }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildModels()
    }

    inner class KeymapController : EpoxyController(), SelectionCallback {
        var keymapList: List<KeymapListItemModel> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            keymapList.forEach {
                keymapSimple {
                    id(it.id)
                    model(it)
                    isSelectable(selectionProvider.isSelectable.value)
                    isSelected(selectionProvider.isSelected(it.id))

                    onErrorClick(object : ErrorClickCallback {
                        override fun onErrorClick(failure: Failure) {
                            mBinding.coordinatorLayout.longSnack(failure.fullMessage) {

                                //only add an action to fix the error if the error can be recovered from
                                if (failure is RecoverableFailure) {
                                    action(R.string.snackbar_fix) {
                                        lifecycleScope.launch {
                                            failure.recover(this@KeymapListFragment)
                                        }
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