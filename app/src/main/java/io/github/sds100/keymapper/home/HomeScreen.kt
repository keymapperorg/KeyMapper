package io.github.sds100.keymapper.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.home.BaseHomeViewModel
import io.github.sds100.keymapper.base.home.HomeKeyMapListScreen

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: BaseHomeViewModel,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    finishActivity: () -> Unit,
) {
    val snackbarState = remember { SnackbarHostState() }

    HomeKeyMapListScreen(
        modifier = modifier,
        viewModel = viewModel.keyMapListViewModel,
        snackbarState = snackbarState,
        onSettingsClick = onSettingsClick,
        onAboutClick = onAboutClick,
        finishActivity = finishActivity,
        fabBottomPadding = 0.dp,
    )
}
