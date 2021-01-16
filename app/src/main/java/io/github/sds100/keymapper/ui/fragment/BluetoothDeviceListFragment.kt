package io.github.sds100.keymapper.ui.fragment

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.IModelState

/**
 * Created by sds100 on 22/02/2020.
 */
class BluetoothDeviceListFragment
    : DefaultRecyclerViewFragment<List<BluetoothDevice>>(), IModelState<List<BluetoothDevice>> {

    companion object {
        const val REQUEST_KEY = "request_key_bluetooth_device"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_ADDRESS = "extra_address"
    }

    override var requestKey: String? = REQUEST_KEY

    override val model = MutableLiveData<DataState<List<BluetoothDevice>>>(Loading())
    override val viewState = MutableLiveData<ViewState>(ViewLoading())

    override val modelState: IModelState<List<BluetoothDevice>>
        get() = this

    override fun populateList(
        binding: FragmentRecyclerviewBinding,
        model: List<BluetoothDevice>?
    ) {
        binding.epoxyRecyclerView.withModels {
            model?.forEach { device ->
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

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        model.value = Loading()

        val pairedDevices = BluetoothUtils.getPairedDevices()

        if (pairedDevices == null || pairedDevices.isEmpty()) {
            binding.caption = str(R.string.caption_no_paired_bt_devices)
            model.value = Empty()
        } else {
            binding.caption = null
            model.value = Data(pairedDevices)
        }
    }
}