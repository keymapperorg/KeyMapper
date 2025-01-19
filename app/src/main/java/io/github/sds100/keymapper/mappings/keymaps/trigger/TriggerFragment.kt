package io.github.sds100.keymapper.mappings.keymaps.trigger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.EpoxyTouchHelper
import com.google.android.material.card.MaterialCardView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.TriggerKeyBindingModel_
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.databinding.FragmentTriggerBinding
import io.github.sds100.keymapper.fixError
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.triggerKey
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 25/11/20.
 */

class TriggerFragment : RecyclerViewFragment<TriggerKeyListItem, FragmentTriggerBinding>() {

    class Info :
        FragmentInfo(
            R.string.trigger_header,
            R.string.url_trigger_guide,
            { TriggerFragment() },
        )

    private val configTriggerViewModel: ConfigTriggerViewModel by lazy {
        navGraphViewModels<ConfigKeyMapViewModel>(R.id.nav_config_keymap) {
            Inject.configKeyMapViewModel(requireContext())
        }.value.configTriggerViewModel
    }

    private val triggerKeyController = TriggerKeyController()

    override val listItems: Flow<State<List<TriggerKeyListItem>>>
        get() = configTriggerViewModel.triggerKeyListItems

    @OptIn(ExperimentalMaterial3Api::class)
    override fun bind(inflater: LayoutInflater, container: ViewGroup?) = FragmentTriggerBinding.inflate(inflater, container, false).apply {
        lifecycleOwner = viewLifecycleOwner

        composeViewRecordTriggerButtons.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                KeyMapperTheme {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    val state by configTriggerViewModel.setupGuiKeyboardState.collectAsStateWithLifecycle()

                    if (configTriggerViewModel.showDpadTriggerSetupBottomSheet) {
                        DpadTriggerSetupBottomSheet(
                            modifier = Modifier.systemBarsPadding(),
                            onDismissRequest = {
                                configTriggerViewModel.showDpadTriggerSetupBottomSheet = false
                            },
                            guiKeyboardState = state,
                            onEnableKeyboardClick = configTriggerViewModel::onEnableGuiKeyboardClick,
                            onChooseKeyboardClick = configTriggerViewModel::onChooseGuiKeyboardClick,
                            onNeverShowAgainClick = configTriggerViewModel::onNeverShowSetupDpadClick,
                            sheetState = sheetState,
                        )
                    }

                    if (configTriggerViewModel.showNoKeysRecordedBottomSheet) {
                        NoKeysRecordedBottomSheet(
                            modifier = Modifier.systemBarsPadding(),
                            onDismissRequest = {
                                configTriggerViewModel.showNoKeysRecordedBottomSheet = false
                            },
                            viewModel = configTriggerViewModel,
                            sheetState = sheetState,
                        )
                    }

                    RecordTriggerButtonRow(Modifier.fillMaxWidth(), configTriggerViewModel)
                }
            }
        }
    }

    override fun subscribeUi(binding: FragmentTriggerBinding) {
        binding.viewModel = configTriggerViewModel

        binding.recyclerViewTriggerKeys.adapter = triggerKeyController.adapter

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            configTriggerViewModel.errorListItems.collectLatest { listItems ->

                binding.enableTriggerKeyDragging(triggerKeyController)

                binding.recyclerViewError.withModels {
                    listItems.forEach {
                        fixError {
                            id(it.id)
                            model(it)

                            onFixClick { _ ->
                                configTriggerViewModel.onTriggerErrorClick(it.id)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<TriggerKeyListItem>,
    ) {
        triggerKeyController.modelList = listItems
    }

    override fun getRecyclerView(binding: FragmentTriggerBinding) = binding.recyclerViewTriggerKeys
    override fun getProgressBar(binding: FragmentTriggerBinding) = binding.progressBar
    override fun getEmptyListPlaceHolderTextView(binding: FragmentTriggerBinding) = binding.emptyListPlaceHolder

    private fun FragmentTriggerBinding.enableTriggerKeyDragging(controller: EpoxyController): ItemTouchHelper = EpoxyTouchHelper.initDragging(controller)
        .withRecyclerView(recyclerViewTriggerKeys)
        .forVerticalList()
        .withTarget(TriggerKeyBindingModel_::class.java)
        .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<TriggerKeyBindingModel_>() {

            override fun isDragEnabledForModel(model: TriggerKeyBindingModel_?): Boolean = model?.model()?.isDragDropEnabled ?: false

            override fun onModelMoved(
                fromPosition: Int,
                toPosition: Int,
                modelBeingMoved: TriggerKeyBindingModel_?,
                itemView: View?,
            ) {
                configTriggerViewModel.onMoveTriggerKey(fromPosition, toPosition)
            }

            override fun onDragStarted(
                model: TriggerKeyBindingModel_?,
                itemView: View?,
                adapterPosition: Int,
            ) {
                itemView?.findViewById<MaterialCardView>(R.id.cardView)?.isDragged = true
            }

            override fun onDragReleased(model: TriggerKeyBindingModel_?, itemView: View?) {
                itemView?.findViewById<MaterialCardView>(R.id.cardView)?.isDragged = false
            }
        })

    private inner class TriggerKeyController : EpoxyController() {
        var modelList: List<TriggerKeyListItem> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            modelList.forEach { model ->
                triggerKey {
                    id(model.id)
                    model(model)

                    onRemoveClick { _ ->
                        configTriggerViewModel.onRemoveKeyClick(model.id)
                    }

                    onMoreClick { _ ->
                        configTriggerViewModel.onTriggerKeyOptionsClick(model.id)
                    }

                    onDeviceClick { _ ->
                        configTriggerViewModel.onChooseDeviceClick(model.id)
                    }
                }
            }
        }
    }
}
