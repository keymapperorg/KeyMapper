package io.github.sds100.keymapper.ui.fragment

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.BluetoothUtils
import io.github.sds100.keymapper.util.str
import java.io.Serializable

/**
 * Created by sds100 on 22/02/2020.
 */
class BluetoothDeviceListFragment : RecyclerViewFragment() {

    companion object {
        const val SAVED_STATE_KEY = "key_bt_device"
    }

    override var selectedModelKey: String? = SAVED_STATE_KEY

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
                        selectModel(Model(device.address, device.name))
                    }
                }
            }
        }
    }

    data class Model(val address: String, val name: String) : Serializable
}