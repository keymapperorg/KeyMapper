package io.github.sds100.keymapper.base.system.bluetooth

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.base.fixError
import io.github.sds100.keymapper.base.simple
import io.github.sds100.keymapper.base.utils.ui.ListItem
import io.github.sds100.keymapper.base.utils.ui.RecyclerViewUtils
import io.github.sds100.keymapper.base.utils.ui.SimpleListItemOld
import io.github.sds100.keymapper.base.utils.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.base.utils.ui.TextListItem
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ChooseBluetoothDeviceFragment : SimpleRecyclerViewFragment<ListItem>() {

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_ADDRESS = "extra_address"
    }

    private val args: ChooseBluetoothDeviceFragmentArgs by navArgs()

    private val viewModel: ChooseBluetoothDeviceViewModel by viewModels()

    override val listItems: Flow<State<List<ListItem>>>
        get() = viewModel.listItems

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        RecyclerViewUtils.applySimpleListItemDecorations(binding.epoxyRecyclerView)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.caption.collectLatest {
                binding.caption = it
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest { device ->
                returnResult(EXTRA_ADDRESS to device.address, EXTRA_NAME to device.name)
            }
        }
    }

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<ListItem>,
    ) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                if (listItem is SimpleListItemOld) {
                    simple {
                        id(listItem.id)
                        model(listItem)

                        onClickListener { _ ->
                            viewModel.onBluetoothDeviceListItemClick(listItem.id)
                        }
                    }
                } else if (listItem is TextListItem.Error) {
                    fixError {
                        id(listItem.id)
                        model(listItem)
                        onFixClick { _ ->
                            viewModel.onFixMissingPermissionListItemClick()
                        }
                    }
                }
            }
        }
    }

    override fun getRequestKey(): String = args.requestKey
}
