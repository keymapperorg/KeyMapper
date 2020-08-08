package io.github.sds100.keymapper.ui.fragment

import android.os.Build
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.UnsupportedActionListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.TintType
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.result.SdkVersionTooLow
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 31/03/2020.
 */
class UnsupportedActionListFragment : DefaultRecyclerViewFragment() {

    private val mViewModel: UnsupportedActionListViewModel by activityViewModels {
        InjectorUtils.provideUnsupportedActionListViewModel(requireContext())
    }

    override val progressCallback: ProgressCallback?
        get() = mViewModel

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.apply {
            mViewModel.unsupportedSystemActions.observe(viewLifecycleOwner) { unsupportedActions ->
                epoxyRecyclerView.withModels {
                    if (!mViewModel.isTapCoordinateActionSupported) {
                        simple {
                            id(0)
                            primaryText(str(R.string.action_type_tap_coordinate))
                            secondaryText(SdkVersionTooLow(Build.VERSION_CODES.N).getFullMessage(requireContext()))
                        }
                    }

                    unsupportedActions.forEach {
                        simple {
                            id(it.id)
                            icon(drawable(it.icon))
                            tintType(TintType.ON_SURFACE)
                            primaryText(str(it.description))
                            secondaryText(it.reason.getFullMessage(requireContext()))
                        }
                    }
                }
            }
        }
    }
}