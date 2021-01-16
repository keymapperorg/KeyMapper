package io.github.sds100.keymapper.ui.fragment

import android.os.Build
import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.UnsupportedSystemActionListItemModel
import io.github.sds100.keymapper.data.viewmodel.UnsupportedActionListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.TintType
import io.github.sds100.keymapper.util.delegate.IModelState
import io.github.sds100.keymapper.util.drawable
import io.github.sds100.keymapper.util.result.SdkVersionTooLow
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 31/03/2020.
 */
class UnsupportedActionListFragment
    : DefaultRecyclerViewFragment<List<UnsupportedSystemActionListItemModel>>() {

    private val viewModel: UnsupportedActionListViewModel by activityViewModels {
        InjectorUtils.provideUnsupportedActionListViewModel(requireContext())
    }

    override val modelState: IModelState<List<UnsupportedSystemActionListItemModel>>
        get() = viewModel

    override fun populateList(
        binding: FragmentRecyclerviewBinding,
        model: List<UnsupportedSystemActionListItemModel>?
    ) {
        binding.epoxyRecyclerView.withModels {
            if (!viewModel.isTapCoordinateActionSupported) {
                simple {
                    id(0)
                    primaryText(str(R.string.action_type_tap_coordinate))
                    secondaryText(SdkVersionTooLow(Build.VERSION_CODES.N)
                        .getFullMessage(requireContext()))
                }
            }

            model?.forEach {
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