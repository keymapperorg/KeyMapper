package io.github.sds100.keymapper.util.ui.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.sds100.keymapper.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RowScope.SearchAppBarActions(
    onCloseSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    enabled: Boolean,
    query: String?,
) {
    var isExpanded: Boolean by rememberSaveable { mutableStateOf(false) }

    IconButton(onClick = {
        if (isExpanded) {
            onCloseSearch()
            isExpanded = false
        } else {
            onNavigateBack()
        }
    }) {
        Icon(
            Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.bottom_app_bar_back_content_description),
        )
    }

    DockedSearchBar(
        modifier = Modifier.Companion.align(Alignment.Companion.CenterVertically),
        inputField = {
            SearchBarDefaults.InputField(
                modifier = Modifier.Companion.align(Alignment.Companion.CenterVertically),
                onSearch = {
                    onQueryChange(it)
                    isExpanded = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
                enabled = enabled,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                query = query ?: "",
                onQueryChange = onQueryChange,
                expanded = isExpanded,
                onExpandedChange = { expanded ->
                    if (expanded) {
                        isExpanded = true
                    } else {
                        onCloseSearch()
                        isExpanded = false
                    }
                },
            )
        },
        // This is false to prevent an empty "content" showing underneath.
        expanded = isExpanded,
        onExpandedChange = { expanded ->
            if (expanded) {
                isExpanded = true
            } else {
                onCloseSearch()
                isExpanded = false
            }
        },
        content = {},
    )
}
