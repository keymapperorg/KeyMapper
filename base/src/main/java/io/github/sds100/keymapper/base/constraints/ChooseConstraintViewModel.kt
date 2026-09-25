package io.github.sds100.keymapper.base.constraints

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.containsQuery
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemGroup
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemModel
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.State
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class ChooseConstraintViewModel @Inject constructor(
    useCase: CreateConstraintUseCase,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    companion object {
        // Synthetic IDs for consolidated orientation list items (not actual ConstraintIds)
        private const val DISPLAY_ORIENTATION_LIST_ITEM_ID = "display_orientation"
        private const val PHYSICAL_ORIENTATION_LIST_ITEM_ID = "physical_orientation"

        private val CATEGORY_ORDER = arrayOf(
            ConstraintCategory.APPS,
            ConstraintCategory.MEDIA,
            ConstraintCategory.BLUETOOTH,
            ConstraintCategory.DISPLAY,
            ConstraintCategory.FLASHLIGHT,
            ConstraintCategory.WIFI,
            ConstraintCategory.KEYBOARD,
            ConstraintCategory.LOCK,
            ConstraintCategory.PHONE,
            ConstraintCategory.POWER,
            ConstraintCategory.DEVICE,
            ConstraintCategory.NOTIFICATIONS,
            ConstraintCategory.TIME,
        )
    }

    val createConstraintDelegate =
        CreateConstraintDelegate(viewModelScope, useCase, this, this, this)

    private val allGroupedListItems: List<SimpleListItemGroup> by lazy { buildListGroups(useCase) }

    val searchQuery = MutableStateFlow<String?>(null)

    val groups: StateFlow<State<List<SimpleListItemGroup>>> =
        searchQuery.map { query ->
            val groups = allGroupedListItems.mapNotNull { group ->

                val filteredItems = group.items.filter { it.title.containsQuery(query) }

                if (filteredItems.isEmpty()) {
                    return@mapNotNull null
                } else {
                    group.copy(items = filteredItems)
                }
            }

            State.Data(groups)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    init {
        viewModelScope.launch {
            createConstraintDelegate.constraintResult.filterNotNull().collect { constraintData ->
                popBackStackWithResult(Json.encodeToString(constraintData))
            }
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            // Handle synthetic list item IDs for consolidated orientation constraints
            when (id) {
                DISPLAY_ORIENTATION_LIST_ITEM_ID -> {
                    onSelectDisplayOrientationConstraint()
                    return@launch
                }

                PHYSICAL_ORIENTATION_LIST_ITEM_ID -> {
                    onSelectPhysicalOrientationConstraint()
                    return@launch
                }
            }

            createConstraintDelegate.createConstraint(ConstraintId.valueOf(id))
        }
    }

    private suspend fun onSelectDisplayOrientationConstraint() {
        val items = listOf(
            ConstraintId.DISPLAY_ORIENTATION_PORTRAIT to
                getString(R.string.constraint_choose_orientation_portrait),
            ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE to
                getString(R.string.constraint_choose_orientation_landscape),
            ConstraintId.DISPLAY_ORIENTATION_0 to
                getString(R.string.constraint_choose_orientation_0),
            ConstraintId.DISPLAY_ORIENTATION_90 to
                getString(R.string.constraint_choose_orientation_90),
            ConstraintId.DISPLAY_ORIENTATION_180 to
                getString(R.string.constraint_choose_orientation_180),
            ConstraintId.DISPLAY_ORIENTATION_270 to
                getString(R.string.constraint_choose_orientation_270),
        )

        val dialog = DialogModel.SingleChoice(items)
        val selectedOrientation = showDialog("choose_display_orientation", dialog) ?: return

        createConstraintDelegate.createConstraint(selectedOrientation)
    }

    private suspend fun onSelectPhysicalOrientationConstraint() {
        val items = listOf(
            PhysicalOrientation.PORTRAIT to
                getString(R.string.constraint_choose_physical_orientation_portrait),
            PhysicalOrientation.LANDSCAPE to
                getString(R.string.constraint_choose_physical_orientation_landscape),
            PhysicalOrientation.PORTRAIT_INVERTED to
                getString(R.string.constraint_choose_physical_orientation_portrait_inverted),
            PhysicalOrientation.LANDSCAPE_INVERTED to
                getString(R.string.constraint_choose_physical_orientation_landscape_inverted),
        )

        val dialog = DialogModel.SingleChoice(items)
        val selectedOrientation = showDialog("choose_physical_orientation", dialog) ?: return

        val constraintId = when (selectedOrientation) {
            PhysicalOrientation.PORTRAIT -> ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT
            PhysicalOrientation.LANDSCAPE -> ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE
            PhysicalOrientation.PORTRAIT_INVERTED ->
                ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED
            PhysicalOrientation.LANDSCAPE_INVERTED ->
                ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED
        }

        createConstraintDelegate.createConstraint(constraintId)
    }

    private fun buildListGroups(useCase: CreateConstraintUseCase): List<SimpleListItemGroup> =
        buildList {
            // Filter out individual orientation constraints - show only the consolidated ones
            val filteredConstraints = ConstraintId.entries.filter { constraintId ->
                constraintId !in listOf(
                    ConstraintId.DISPLAY_ORIENTATION_PORTRAIT,
                    ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE,
                    ConstraintId.DISPLAY_ORIENTATION_0,
                    ConstraintId.DISPLAY_ORIENTATION_90,
                    ConstraintId.DISPLAY_ORIENTATION_180,
                    ConstraintId.DISPLAY_ORIENTATION_270,
                    ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT,
                    ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE,
                    ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED,
                    ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED,
                )
            }

            val listItems = buildListItems(useCase, filteredConstraints)

            // Add synthetic orientation list items
            val displayOrientationItem = SimpleListItemModel(
                id = DISPLAY_ORIENTATION_LIST_ITEM_ID,
                title = getString(R.string.constraint_choose_screen_orientation),
                icon = ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait),
                isEnabled = true,
            )

            val physicalOrientationItem = SimpleListItemModel(
                id = PHYSICAL_ORIENTATION_LIST_ITEM_ID,
                title = getString(R.string.constraint_choose_physical_orientation),
                icon = ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait),
                isEnabled = true,
            )

            for (category in CATEGORY_ORDER) {
                val header = getString(ConstraintUtils.getCategoryLabel(category))

                val categoryItems = listItems.filter { item ->
                    item.isEnabled &&
                        try {
                            ConstraintUtils.getCategory(ConstraintId.valueOf(item.id)) == category
                        } catch (e: IllegalArgumentException) {
                            false
                        }
                }.toMutableList()

                // Add synthetic orientation items to DISPLAY category
                if (category == ConstraintCategory.DISPLAY) {
                    categoryItems.add(displayOrientationItem)
                    categoryItems.add(physicalOrientationItem)
                }

                val group = SimpleListItemGroup(
                    header,
                    items = categoryItems,
                )

                if (group.items.isNotEmpty()) {
                    add(group)
                }
            }

            val unsupportedItems = listItems.filter { !it.isEnabled }
            if (unsupportedItems.isNotEmpty()) {
                val unsupportedGroup = SimpleListItemGroup(
                    header = getString(R.string.choose_constraint_group_unsupported),
                    items = unsupportedItems,
                )
                add(unsupportedGroup)
            }
        }

    private fun buildListItems(
        useCase: CreateConstraintUseCase,
        constraintIds: List<ConstraintId>,
    ): List<SimpleListItemModel> = buildList {
        for (constraintId in constraintIds) {
            val title = getString(ConstraintUtils.getTitleStringId(constraintId))
            val icon = ConstraintUtils.getIcon(constraintId)
            val error = useCase.isSupported(constraintId)

            val listItem = SimpleListItemModel(
                id = constraintId.toString(),
                title = title,
                icon = icon,
                subtitle = error?.getFullMessage(this@ChooseConstraintViewModel),
                isSubtitleError = true,
                isEnabled = error == null,
            )

            add(listItem)
        }
    }
}
