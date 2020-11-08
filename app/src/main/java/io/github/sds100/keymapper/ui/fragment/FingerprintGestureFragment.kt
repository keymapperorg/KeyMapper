package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.viewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.FingerprintGestureViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.fingerprintGesture
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 22/02/2020.
 */
class FingerprintGestureFragment : DefaultRecyclerViewFragment() {

    private val mViewModel: FingerprintGestureViewModel by viewModels {
        InjectorUtils.provideFingerprintGestureViewModel(requireContext())
    }

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.state = Data(true)

        binding.epoxyRecyclerView.withModels {
            //SWIPE DOWN
            fingerprintGesture {
                id("swipe_down")

                header(requireContext().str(R.string.header_fingerprint_gesture_down))

                onChooseActionClick { _ ->
                }

                onActionOptionsClick { _ ->

                }

                onRemoveActionClick { _ ->

                }

                model(null)
            }
        }
    }
}