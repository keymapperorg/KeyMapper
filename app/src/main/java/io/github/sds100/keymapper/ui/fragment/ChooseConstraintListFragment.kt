package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.data.viewmodel.ChooseConstraintListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.sectionHeader
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.ConstraintUtils
import io.github.sds100.keymapper.util.EventObserver
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.util.str
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.okButton

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseConstraintListFragment : DefaultRecyclerViewFragment() {

    companion object {
        const val REQUEST_KEY = "request_constraint"
        const val EXTRA_CONSTRAINT = "extra_constraint"
    }

    private val mViewModel: ChooseConstraintListViewModel by viewModels {
        InjectorUtils.provideChooseConstraintListViewModel()
    }

    override var requestKey: String? = REQUEST_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(AppListFragment.REQUEST_KEY) { _, result ->
            val packageName = result.getString(AppListFragment.EXTRA_PACKAGE_NAME)

            mViewModel.packageChosen(packageName!!)
        }

        setFragmentResultListener(BluetoothDeviceListFragment.REQUEST_KEY) { _, result ->
            val address = result.getString(BluetoothDeviceListFragment.EXTRA_ADDRESS)
            val name = result.getString(BluetoothDeviceListFragment.EXTRA_NAME)

            mViewModel.bluetoothDeviceChosen(address!!, name!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mViewModel.choosePackageEvent.observe(viewLifecycleOwner, EventObserver {
            val direction = ChooseConstraintListFragmentDirections.actionChooseConstraintListFragmentToAppListFragment()
            findNavController().navigate(direction)
        })

        mViewModel.chooseBluetoothDeviceEvent.observe(viewLifecycleOwner, EventObserver {
            val direction =
                ChooseConstraintListFragmentDirections.actionChooseConstraintListFragmentToBluetoothDevicesFragment()

            findNavController().navigate(direction)
        })

        mViewModel.notifyUserEvent.observe(viewLifecycleOwner, EventObserver { model ->
            requireContext().alertDialog {
                messageResource = model.message

                okButton {
                    model.onApproved.invoke()
                }

                show()
            }
        })

        mViewModel.selectModelEvent.observe(viewLifecycleOwner, EventObserver {
            returnResult(EXTRA_CONSTRAINT to it)
        })

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.epoxyRecyclerView.withModels {
            for ((sectionHeader, constraints) in mViewModel.constraintsSortedByCategory) {

                sectionHeader {
                    id(sectionHeader)
                    header(requireContext().str(sectionHeader))
                }

                constraints.forEach { constraint ->
                    simple {
                        id(constraint.id)
                        primaryText(requireContext().str(constraint.description))
                        isSecondaryTextAnError(true)

                        val isSupported = ConstraintUtils.isSupported(requireContext(), constraint.id)

                        if (isSupported == null) {
                            secondaryText(null)
                        } else {
                            secondaryText(isSupported.getFullMessage(requireContext()))
                        }

                        onClick { _ ->
                            mViewModel.chooseConstraint(constraint.id)
                        }
                    }
                }
            }
        }
    }
}