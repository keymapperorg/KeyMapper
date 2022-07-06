package io.github.sds100.keymapper.mappings.fingerprintmaps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.sds100.keymapper.databinding.FragmentFingerprintMapListBinding
import io.github.sds100.keymapper.fingerprintMap
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.ui.OnChipClickCallback
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 11/12/2020.
 */
class FingerprintMapListFragment :
    RecyclerViewFragment<FingerprintMapListItem, FragmentFingerprintMapListBinding>() {

    private val homeViewModel: HomeViewModel by activityViewModels()

    private val viewModel by lazy { homeViewModel.fingerprintMapListViewModel }

    override val listItems: Flow<State<List<FingerprintMapListItem>>>
        get() = viewModel.state

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFingerprintMapListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun subscribeUi(binding: FragmentFingerprintMapListBinding) {
        binding.viewModel = viewModel
    }

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<FingerprintMapListItem>
    ) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                fingerprintMap {
                    id(listItem.id.toString())

                    model(listItem)

                    onCardClick { _ ->
                        viewModel.onCardClick(listItem.id)
                    }

                    onEnabledSwitchClickListener { view ->
                        viewModel.onEnabledSwitchChange(listItem.id, (view as SwitchMaterial).isChecked)
                    }

                    onActionChipClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                            viewModel.onActionChipClick(chipModel)
                        }
                    })

                    onConstraintChipClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                            viewModel.onConstraintsChipClick(chipModel)
                        }
                    })
                }
            }
        }
    }

    override fun getRecyclerView(binding: FragmentFingerprintMapListBinding) =
        binding.epoxyRecyclerView

    override fun getProgressBar(binding: FragmentFingerprintMapListBinding) = binding.progressBar
    override fun getEmptyListPlaceHolderTextView(binding: FragmentFingerprintMapListBinding) =
        binding.emptyListPlaceHolder
}