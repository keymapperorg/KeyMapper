package io.github.sds100.keymapper.mappings.fingerprintmaps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.databinding.FragmentFingerprintMapListBinding
import io.github.sds100.keymapper.home.HomeFragmentDirections
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.OnChipClickCallback
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 11/12/2020.
 */
class FingerprintMapListFragment :
    RecyclerViewFragment<FingerprintMapListItem, FragmentFingerprintMapListBinding>() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        Inject.homeViewModel(requireContext())
    }

    private val viewModel by lazy { homeViewModel.fingerprintMapListViewModel }

    override val listItems: Flow<ListUiState<FingerprintMapListItem>>
        get() = viewModel.state

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFingerprintMapListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun subscribeUi(binding: FragmentFingerprintMapListBinding) {
        binding.viewModel = viewModel

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.launchConfigFingerprintMap.collectLatest { id ->
                findNavController().navigate(
                    HomeFragmentDirections.actionToConfigFingerprintMap(
                        Json.encodeToString(id)
                    )
                )
            }
        }
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
    override fun getEmptyListPlaceHolder(binding: FragmentFingerprintMapListBinding) =
        binding.emptyListPlaceHolder
}