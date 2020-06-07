package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.data.viewmodel.ChooseConstraintListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.sectionHeader
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.getFullMessage
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.okButton

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseConstraintListFragment : RecyclerViewFragment() {

    companion object {
        const val SAVED_STATE_KEY = "key_constraint"
    }

    private val mViewModel: ChooseConstraintListViewModel by viewModels {
        InjectorUtils.provideChooseConstraintListViewModel()
    }

    override var selectedModelKey: String? = SAVED_STATE_KEY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        observeFragmentChildrenLiveData()

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
            selectModel(it)
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

    private fun observeFragmentChildrenLiveData() = findNavController().currentBackStackEntry?.apply {
        observeLiveDataEvent<AppListItemModel>(
            viewLifecycleOwner,
            AppListFragment.SAVED_STATE_KEY
        ) {
            mViewModel.packageChosen(it.packageName)
        }

        observeLiveDataEvent<BluetoothDeviceListFragment.Model>(
            viewLifecycleOwner,
            BluetoothDeviceListFragment.SAVED_STATE_KEY
        ) {
            mViewModel.bluetoothDeviceChosen(it.address, it.name)
        }
    }
}