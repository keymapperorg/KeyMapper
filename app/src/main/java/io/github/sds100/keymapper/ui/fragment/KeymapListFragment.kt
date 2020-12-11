package io.github.sds100.keymapper.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.KeymapListItemModel
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.keymap
import io.github.sds100.keymapper.ui.callback.ErrorClickCallback
import io.github.sds100.keymapper.ui.callback.SelectionCallback
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 22/02/2020.
 */
class KeymapListFragment : DefaultRecyclerViewFragment() {

    private val mViewModel: KeymapListViewModel by activityViewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    private val mBackupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private val selectionProvider: ISelectionProvider
        get() = mViewModel.selectionProvider

    private val mBackupLauncher by lazy {
        requireActivity().registerForActivityResult(ActivityResultContracts.CreateDocument()) {
            it ?: return@registerForActivityResult

            mBackupRestoreViewModel.backup(requireActivity().contentResolver.openOutputStream(it),
                *selectionProvider.selectedIds)

            selectionProvider.stopSelecting()
        }
    }

    private val mController = KeymapController()

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        mViewModel.apply {

            keymapModelList.observe(viewLifecycleOwner, { keymapList ->
                binding.state = keymapList

                mController.keymapList = if (keymapList is Data) {
                    keymapList.data
                } else {
                    listOf()
                }
            })

            selectionProvider.callback = mController

            selectionProvider.isSelectable.observe(viewLifecycleOwner, {
                mController.requestModelBuild()
            })

            mViewModel.eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is BuildKeymapListModels -> lifecycleScope.launchWhenStarted {
                        mViewModel.setModelList(buildModelList(it.keymapList))
                    }

                    is BackupSelectedKeymaps -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            mBackupLauncher.launch(BackupUtils.createFileName())
                        }
                    }
                }
            })
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //assign in onViewCreated in case context is required when building the models.
        binding.epoxyRecyclerView.adapter = mController.adapter
    }

    private suspend fun buildModelList(keymapList: List<KeyMap>) =
        keymapList.map { keymap ->
            val deviceInfoList = mViewModel.getDeviceInfoList()

            KeymapListItemModel(
                id = keymap.id,
                actionList = keymap.actionList.map { it.buildChipModel(requireContext(), deviceInfoList) },
                triggerDescription = keymap.trigger.buildDescription(requireContext(), deviceInfoList),
                constraintList = keymap.constraintList.map { it.buildModel(requireContext()) },
                constraintMode = keymap.constraintMode,
                flagsDescription = keymap.trigger.buildTriggerFlagsDescription(requireContext()),
                isEnabled = keymap.isEnabled
            )
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
                    model(it)
                    isSelectable(selectionProvider.isSelectable.value)
                    isSelected(selectionProvider.isSelected(it.id))

                    onErrorClick(object : ErrorClickCallback {
                        override fun onErrorClick(failure: Failure) {
                            mViewModel.fixError(failure)
                        }
                    })

                    onClick { _ ->
                        val id = it.id

                        if (selectionProvider.isSelectable.value == true) {
                            selectionProvider.toggleSelection(id)
                        } else {
                            val direction = HomeFragmentDirections.actionToConfigKeymap(id)
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