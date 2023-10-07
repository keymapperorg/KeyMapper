package io.github.sds100.keymapper.mappings.fingerprintmaps

import androidx.navigation.navGraphViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ui.utils.configuredCheckBox
import io.github.sds100.keymapper.ui.utils.configuredSlider
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.CheckBoxListItem
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.ui.SliderListItem
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 29/11/20.
 */
class FingerprintMapOptionsFragment : SimpleRecyclerViewFragment<ListItem>() {

    class Info : FragmentInfo(
        R.string.option_list_header,
        R.string.url_fingerprint_map_options_guide,
        { FingerprintMapOptionsFragment() }
    )

    private val configViewModel: ConfigFingerprintMapViewModel by navGraphViewModels(R.id.nav_config_fingerprint_map) {
        Inject.configFingerprintMapViewModel(requireContext())
    }

    private val viewModel: ConfigFingerprintMapOptionsViewModel
        get() = configViewModel.configOptionsViewModel

    override var isAppBarVisible = false

    override val listItems: Flow<State<List<ListItem>>>
        get() = viewModel.state

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<ListItem>) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                if (listItem is CheckBoxListItem) {
                    configuredCheckBox(listItem) { isChecked ->
                        viewModel.setCheckboxValue(listItem.id, isChecked)
                    }
                }

                if (listItem is SliderListItem) {
                    configuredSlider(this@FingerprintMapOptionsFragment, listItem) { newValue ->
                        viewModel.setSliderValue(listItem.id, newValue)
                    }
                }
            }
        }
    }
}