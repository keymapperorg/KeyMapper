package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintGestureViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.fingerprintGesture
import io.github.sds100.keymapper.util.*

/**
 * Created by sds100 on 22/02/2020.
 */
class FingerprintGestureFragment : DefaultRecyclerViewFragment() {

    private val mViewModel: FingerprintGestureViewModel by viewModels {
        InjectorUtils.provideFingerprintGestureViewModel(requireContext())
    }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildModels()
    }

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        mViewModel.models.observe(viewLifecycleOwner, { models ->
            binding.state = models

            if (models !is Data) return@observe

            binding.epoxyRecyclerView.withModels {
                models.data.forEach {
                    fingerprintGesture {
                        id(it.id)
                        model(it)

                        onChooseActionClick { _ ->
                            val direction = HomeFragmentDirections.actionHomeFragmentToChooseActionFragment()
                            findNavController().navigate(direction)
                        }
                    }
                }
            }
        })

        mViewModel.buildModels.observe(viewLifecycleOwner, EventObserver { gestureMaps ->
            val models = gestureMaps.map {
                FingerprintGestureMapListItemModel(
                    id = it.key,
                    header = str(FingerprintGestureUtils.HEADERS[it.key]!!),
                    actionModel = it.value?.action?.buildModel(requireContext())
                )
            }

            mViewModel.setModels(models)
        })
    }
}