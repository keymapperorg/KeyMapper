package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.FingerprintGestureMapListItemModel
import io.github.sds100.keymapper.data.viewmodel.FingerprintGestureViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.fingerprintGesture
import io.github.sds100.keymapper.util.*

/**
 * Created by sds100 on 22/02/2020.
 */
class FingerprintGestureFragment : DefaultRecyclerViewFragment() {

    private val mSwipeDownViewModel: FingerprintGestureViewModel by viewModels {
        InjectorUtils.provideFingerprintGestureViewModel(requireContext(),
            R.string.key_pref_fingerprint_swipe_down_json)
    }

    private val mSwipeUpViewModel: FingerprintGestureViewModel by viewModels {
        InjectorUtils.provideFingerprintGestureViewModel(requireContext(),
            R.string.key_pref_fingerprint_swipe_up_json)
    }

    private val mController = FingerprintGestureController()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.epoxyRecyclerView.adapter = mController.adapter
    }

    override fun onResume() {
        super.onResume()

        mSwipeDownViewModel.rebuildModel()
    }

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        mSwipeDownViewModel.model.observe(viewLifecycleOwner, {
            binding.state = it

            if (it is Data) {
                mController.swipeDownModel = it.data
            }
        })

        mSwipeDownViewModel.buildModel.observe(viewLifecycleOwner, EventObserver {
            val model = FingerprintGestureMapListItemModel(
                header = str(R.string.header_fingerprint_gesture_down),
                actionModel = it?.action?.buildModel(requireContext())
            )

            mSwipeDownViewModel.setModel(model)
        })
    }

    inner class FingerprintGestureController : EpoxyController() {
        var swipeDownModel: FingerprintGestureMapListItemModel? = null
            set(value) {
                field = value
                requestModelBuild()
            }

        var swipeUpModel: FingerprintGestureMapListItemModel? = null
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            fingerprintGesture {
                id("swipe_down")

                model(swipeDownModel)
            }
        }
    }
}