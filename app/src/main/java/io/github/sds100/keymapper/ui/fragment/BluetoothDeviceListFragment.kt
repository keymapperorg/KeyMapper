package io.github.sds100.keymapper.ui.fragment

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.BluetoothUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 22/02/2020.
 */
class BluetoothDeviceListFragment : DefaultRecyclerViewFragment() {

    companion object {
        const val REQUEST_KEY = "request_key_bluetooth_device"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_ADDRESS = "extra_address"
    }

    override var requestKey: String? = REQUEST_KEY

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.epoxyRecyclerView.withModels {
            val pairedDevices = BluetoothUtils.getPairedDevices()

            if (pairedDevices == null || pairedDevices.isEmpty()) {
                binding.caption = str(R.string.caption_no_paired_bt_devices)
            } else {
                binding.caption = null
            }

            pairedDevices?.forEach { device ->
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
}