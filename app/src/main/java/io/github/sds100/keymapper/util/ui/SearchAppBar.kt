package io.github.sds100.keymapper.util.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 30/07/2022.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun SearchAppBar(
    navigateBack: () -> Unit,
    searchState: SearchState,
    setSearchState: (SearchState) -> Unit,
    defaultIcons: @Composable RowScope.() -> Unit
) {
    val isSearching: Boolean by derivedStateOf { searchState is SearchState.Searching }
    val onBack = {
        if (isSearching) {
            setSearchState(SearchState.Idle)
        } else {
            navigateBack()
        }
    }

    BackHandler(onBack = onBack)

    val searchFieldFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    BottomAppBar(
        icons = {
            // only show normal icons if not searching.
            AnimatedVisibility(visible = !isSearching) {
                defaultIcons()
            }

            if (isSearching) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.choose_action_back_content_description)
                    )
                }
            }

            AnimatedVisibility(visible = isSearching, modifier = Modifier.fillMaxWidth()) {
                //show the keyboard when opening for the first time
                if (transition.currentState == EnterExitState.Visible && transition.targetState == EnterExitState.Visible) {
                    SideEffect {
                        searchFieldFocusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }

                val textValue = when (searchState) {
                    SearchState.Idle -> ""
                    is SearchState.Searching -> searchState.query
                }

                TextField(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .focusRequester(searchFieldFocusRequester),
                    value = textValue,
                    onValueChange = { setSearchState(SearchState.Searching(it)) },
                    trailingIcon = {
                        IconButton(onClick = { setSearchState(SearchState.Searching("")) }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.clear_search_query_content_description)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
        },
        floatingActionButton = {
            AnimatedVisibility(visible = !isSearching) {
                FloatingActionButton(
                    onClick = { setSearchState(SearchState.Searching("")) },
                    elevation = BottomAppBarDefaults.FloatingActionButtonElevation,
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.choose_action_search_content_description)
                    )
                }
            }
        }
    )
}