package io.github.sds100.keymapper.system.bluetooth

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 22/02/2020.
 */
class ChooseBluetoothDeviceFragment : SimpleRecyclerViewFragment<BluetoothDeviceInfo>() {

    companion object {
        const val REQUEST_KEY = "request_key_bluetooth_device"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_ADDRESS = "extra_address"
    }

    private val viewModel: ChooseBluetoothDeviceViewModel by viewModels {
        Inject.chooseBluetoothDeviceViewModel(requireContext())
    }

    override var requestKey: String? = REQUEST_KEY

    override val listItems: Flow<ListUiState<BluetoothDeviceInfo>>
        get() = viewModel.listItems

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<BluetoothDeviceInfo>
    ) {
        recyclerView.withModels {
            listItems.forEach { device ->
                simple {
                    id(device.address)
                    primaryText(device.name)
                    secondaryText(device.address)

                    onClick { _ ->
                        returnResult(EXTRA_ADDRESS to device.address, EXTRA_NAME to device.name)
                    }
                }
            }
        }
    }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

       viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED){
           viewModel.caption.collectLatest {
               binding.caption = it
           }
       }
    }
}