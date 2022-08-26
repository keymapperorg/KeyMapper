package io.github.sds100.keymapper.system.intents

import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.result.ResultRecipient
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.destinations.ChooseActivityScreenDestination
import io.github.sds100.keymapper.system.apps.ActivityInfo
import io.github.sds100.keymapper.util.ui.*

@Composable
@Destination
fun ConfigIntentScreen(
    viewModel: ConfigIntentViewModel2,
    resultBackNavigator: ResultBackNavigator<ConfigIntentResult>,
    navigator: DestinationsNavigator,
    activityResultRecipient: ResultRecipient<ChooseActivityScreenDestination, ActivityInfo>,
) {
    activityResultRecipient.onNavResult { result ->
        if (result is com.ramcosta.composedestinations.result.NavResult.Value) {
            viewModel.onChooseActivity(result.value)
        }
    }

    ConfigIntentScreen(
        state = viewModel.state,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDoneClick = {
            viewModel.buildResult()?.let { resultBackNavigator.navigateBack(it) }
        },
        onBackClick = resultBackNavigator::navigateBack,
        onSelectIntentTarget = viewModel::onSelectIntentTarget,
        onActionChange = viewModel::onActionChange,
        onDataChange = viewModel::onDataChange,
        onPackageChange = viewModel::onPackageChange,
        onChooseActivityClick = {
            navigator.navigate(ChooseActivityScreenDestination)
        },
        onClassChange = viewModel::onClassChange,
        onFlagsTextChange = viewModel::onFlagsTextChange,
        onChooseFlags = viewModel::onChooseFlags,
        onAddCategory = viewModel::onAddCategory,
        onEditCategory = viewModel::onEditCategory,
        onDeleteCategory = viewModel::onDeleteCategory,
        onAddExtra = viewModel::onAddExtra,
        onEditExtra = viewModel::onEditExtra,
        onDeleteExtra = viewModel::onDeleteExtra
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigIntentScreen(
    state: ConfigIntentState,
    onDescriptionChange: (String) -> Unit = {},
    onDoneClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onSelectIntentTarget: (IntentTarget) -> Unit = {},
    onActionChange: (String) -> Unit = {},
    onDataChange: (String) -> Unit = {},
    onPackageChange: (String) -> Unit = {},
    onClassChange: (String) -> Unit = {},
    onChooseActivityClick: () -> Unit = {},
    onFlagsTextChange: (String) -> Unit = {},
    onChooseFlags: (Set<Int>) -> Unit = {},
    onAddCategory: (String) -> Unit = {},
    onEditCategory: (String, String) -> Unit = { _, _ -> },
    onDeleteCategory: (String) -> Unit = {},
    onEditExtra: (String, IntentExtraRow) -> Unit = { _, _ -> },
    onDeleteExtra: (String) -> Unit = {},
    onAddExtra: (IntentExtraRow) -> Unit = {}
) {
    Scaffold(
        bottomBar = {
            BottomAppBar(
                floatingActionButton = {
                    AnimatedVisibility(state.isDoneButtonEnabled, enter = fadeIn(), exit = fadeOut()) {
                        FloatingActionButton(
                            onClick = onDoneClick,
                            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                            containerColor = BottomAppBarDefaults.bottomAppBarFabColor
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = stringResource(R.string.config_intent_screen_done_content_description)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.choose_action_back_content_description)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val error = when (state.descriptionError) {
                IntentDescriptionError.NONE -> ""
                IntentDescriptionError.EMPTY -> stringResource(R.string.config_intent_screen_empty_description_error)
            }

            val focusManager = LocalFocusManager.current

            ErrorOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.description,
                label = stringResource(R.string.config_intent_screen_description_label),
                onValueChange = onDescriptionChange,
                errorMessage = error,
                isError = state.descriptionError != IntentDescriptionError.NONE,
                keyboardActions = KeyboardActions { focusManager.clearFocus() }
            )

            FlowRow(modifier = Modifier.fillMaxWidth()) {
                RadioButtonWithText(
                    isSelected = state.target == IntentTarget.ACTIVITY,
                    text = stringResource(R.string.config_intent_screen_activity_target),
                    onClick = { onSelectIntentTarget(IntentTarget.ACTIVITY) })
                RadioButtonWithText(
                    isSelected = state.target == IntentTarget.BROADCAST_RECEIVER,
                    text = stringResource(R.string.config_intent_screen_broadcast_receiver_target),
                    onClick = { onSelectIntentTarget(IntentTarget.BROADCAST_RECEIVER) })
                RadioButtonWithText(
                    isSelected = state.target == IntentTarget.SERVICE,
                    text = stringResource(R.string.config_intent_screen_service_target),
                    onClick = { onSelectIntentTarget(IntentTarget.SERVICE) })
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.action,
                onValueChange = onActionChange,
                label = { Text(stringResource(R.string.config_intent_screen_action_label)) },
                keyboardActions = KeyboardActions { focusManager.clearFocus() },
                singleLine = true
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.data,
                onValueChange = onDataChange,
                label = { Text(stringResource(R.string.config_intent_screen_data_header)) },
                keyboardActions = KeyboardActions { focusManager.clearFocus() }
            )

            FlagsSection(
                modifier = Modifier.fillMaxWidth(),
                flagsText = state.flagsText,
                flags = state.flags,
                error = state.flagsError,
                onChange = onFlagsTextChange,
                onChooseFlags = onChooseFlags
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.componentPackage,
                onValueChange = onPackageChange,
                label = { Text(stringResource(R.string.config_intent_screen_package_header)) },
                keyboardActions = KeyboardActions { focusManager.clearFocus() },
                singleLine = true
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.componentClass,
                onValueChange = onClassChange,
                label = { Text(stringResource(R.string.config_intent_screen_class_header)) },
                keyboardActions = KeyboardActions { focusManager.clearFocus() },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onChooseActivityClick) {
                Text(stringResource(R.string.config_intent_screen_choose_component_button))
            }

            Spacer(Modifier.height(8.dp))

            CategoriesSection(
                Modifier.fillMaxWidth(),
                categories = state.categories,
                onAddCategory = onAddCategory,
                onEditCategory = onEditCategory,
                onDeleteCategory = onDeleteCategory
            )

            Spacer(Modifier.height(8.dp))

            ExtrasSection(
                modifier = Modifier.fillMaxWidth(),
                extras = state.extraRows,
                onAdd = onAddExtra,
                onEdit = onEditExtra,
                onRemove = onDeleteExtra)
        }
    }
}

@Preview(device = Devices.PIXEL_4_XL)
@Composable
private fun Preview() {
    MaterialTheme {
        Surface {
            ConfigIntentScreen(
                state = ConfigIntentState(
                    description = "description",
                    descriptionError = IntentDescriptionError.EMPTY,
                    target = IntentTarget.SERVICE,
                    data = "data",
                    action = "action",
                    categories = setOf("CATEGORY_DEFAULT"),
                    extraRows = listOf(IntentExtraRow.BooleanExtra("io.github.sds100.keymapper.KEY", true)),
                    flagsText = "123",
                    flags = emptySet(),
                    flagsError = IntentFlagsError.NOT_NUMBER,
                    componentPackage = "",
                    componentClass = "",
                    isDoneButtonEnabled = true
                )
            )
        }
    }
}

@Composable
private fun FlagsSection(
    modifier: Modifier = Modifier,
    flagsText: String,
    flags: Set<Int>,
    error: IntentFlagsError,
    onChange: (String) -> Unit,
    onChooseFlags: (Set<Int>) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(modifier) {
        var showFlagsHelpDialog by rememberSaveable { mutableStateOf(false) }

        ErrorOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = flagsText,
            onValueChange = onChange,
            label = stringResource(R.string.config_intent_screen_flags_header),
            trailingIcon = {
                IconButton(onClick = {
                    showFlagsHelpDialog = true
                }) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = stringResource(R.string.config_intent_screen_flags_help_content_description))
                }
            },
            errorMessage = when (error) {
                IntentFlagsError.NONE -> ""
                IntentFlagsError.NOT_NUMBER -> stringResource(R.string.config_intent_screen_flags_not_a_number_error)
            },
            isError = error == IntentFlagsError.NOT_NUMBER,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions { focusManager.clearFocus() }
        )

        if (showFlagsHelpDialog) {
            FlagsHelpDialog(onDismiss = { showFlagsHelpDialog = false })
        }

        Spacer(Modifier.height(8.dp))

        var showChooseFlagsDialog by rememberSaveable { mutableStateOf(false) }
        var chosenFlags by rememberSaveable { mutableStateOf(emptySet<Int>()) }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                chosenFlags = flags
                showChooseFlagsDialog = true
            }) {
            Text(stringResource(R.string.config_intent_screen_choose_flags_button))
        }

        if (showChooseFlagsDialog) {
            ChooseFlagsDialog(
                chosenFlags = chosenFlags,
                onFlagCheckedChange = { flag, checked ->
                    if (checked) {
                        chosenFlags = chosenFlags.plus(flag)
                    } else {
                        chosenFlags = chosenFlags.minus(flag)
                    }
                },
                onConfirm = {
                    showChooseFlagsDialog = false
                    onChooseFlags(chosenFlags)
                },
                onDismiss = { showChooseFlagsDialog = false }
            )
        }
    }
}

@Composable
private fun FlagsHelpDialog(
    onDismiss: () -> Unit
) {
    val text = buildAnnotatedString {
        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface))
        append(stringResource(R.string.config_intent_screen_flags_help_dialog_text_1))

        pushStringAnnotation(tag = "docs", annotation = stringResource(R.string.url_intent_set_flags_help))
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
            append(stringResource(R.string.config_intent_screen_flags_help_dialog_text_2))
        }
        pop()

        append(stringResource(R.string.config_intent_screen_flags_help_dialog_text_3))
        pop()
    }

    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.pos_ok))
            }
        },
        title = { Text(stringResource(R.string.config_intent_screen_flags_help_dialog_title)) },
        text = {
            ClickableText(text, style = LocalTextStyle.current) { offset ->
                text.getStringAnnotations("docs", offset, offset).firstOrNull()?.let {
                    uriHandler.openUri(it.item)
                }
            }
        })
}

@Composable
private fun ChooseFlagsDialog(
    chosenFlags: Set<Int>,
    onFlagCheckedChange: (Int, Boolean) -> Unit = { _, _ -> },
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val flagList = rememberSaveable { IntentUtils.availableFlags }

    CustomDialog(
        title = stringResource(R.string.config_intent_screen_choose_flags_dialog_title),
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.pos_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.neg_cancel))
            }
        }) {
        LazyColumn {
            items(flagList) { (flag, label) ->
                CheckBoxWithText(
                    modifier = Modifier.fillMaxWidth(),
                    isChecked = chosenFlags.contains(flag),
                    text = { Text(text = label, style = MaterialTheme.typography.bodySmall) },
                    onCheckedChange = { checked ->
                        onFlagCheckedChange(flag, checked)
                    })
            }
        }
    }
}

@Preview(heightDp = 500)
@Composable
private fun ChooseFlagsDialogPreview() {
    MaterialTheme {
        ChooseFlagsDialog(
            chosenFlags = emptySet()
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
private fun CategoriesSection(
    modifier: Modifier = Modifier,
    categories: Set<String>,
    onAddCategory: (String) -> Unit,
    onEditCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.config_intent_screen_category_header),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary
        )

        var showAddCategoryDialog by rememberSaveable { mutableStateOf(false) }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                showAddCategoryDialog = true
            }) {
            Text(stringResource(R.string.config_intent_screen_add_category_button))
        }

        if (showAddCategoryDialog) {
            var text by rememberSaveable { mutableStateOf("") }
            val alreadyExists by remember { derivedStateOf { categories.contains(text) } }

            EditCategoryDialog(
                title = stringResource(R.string.config_intent_screen_add_category_dialog_title),
                text = text,
                onTextChange = { text = it },
                alreadyExists = alreadyExists,
                onConfirm = {
                    showAddCategoryDialog = false
                    onAddCategory(it)
                },
                onDismiss = { showAddCategoryDialog = false }
            )
        }

        var showEditCategoriesDialog: Boolean by rememberSaveable { mutableStateOf(false) }
        var oldEditCategoryText: String? by rememberSaveable { mutableStateOf(null) }
        var editCategoryText: String by rememberSaveable { mutableStateOf("") }

        if (showEditCategoriesDialog) {
            val alreadyExists by remember { derivedStateOf { categories.contains(editCategoryText) } }

            EditCategoryDialog(
                title = stringResource(R.string.config_intent_screen_edit_category_dialog_title),
                alreadyExists = alreadyExists,
                text = editCategoryText,
                onTextChange = { editCategoryText = it },
                onConfirm = {
                    showEditCategoriesDialog = false
                    onEditCategory(oldEditCategoryText!!, editCategoryText)
                },
                onDismiss = { showEditCategoriesDialog = false }
            )
        }

        categories.forEach { category ->
            CategoryRow(
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                text = category,
                onEditClick = {
                    oldEditCategoryText = category
                    editCategoryText = category
                    showEditCategoriesDialog = true
                },
                onRemoveClick = { onDeleteCategory(category) }
            )
        }
    }
}

@Composable
private fun EditCategoryDialog(
    title: String,
    text: String,
    alreadyExists: Boolean,
    onTextChange: (String) -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emptyError = stringResource(R.string.config_intent_screen_empty_category_error)
    val existsError = stringResource(R.string.config_intent_screen_category_exists_error)

    @SuppressLint("UnrememberedMutableState")
    val error: String? by derivedStateOf {
        when {
            text.isEmpty() -> emptyError
            alreadyExists -> existsError
            else -> null
        }
    }

    TextFieldDialog(
        title = title,
        text = text,
        onTextChange = onTextChange,
        error = error,
        label = stringResource(R.string.config_intent_screen_category_name_label),
        onConfirm = onConfirm,
        onDismiss = onDismiss)
}

@Composable
private fun ExtrasSection(
    modifier: Modifier = Modifier,
    extras: List<IntentExtraRow>,
    onAdd: (IntentExtraRow) -> Unit,
    onEdit: (String, IntentExtraRow) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.config_intent_screen_extras_header),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary
        )

        var showAddExtraDialog by rememberSaveable { mutableStateOf(false) }

        if (showAddExtraDialog) {
            var state: IntentExtraRow by rememberSaveable { mutableStateOf(IntentExtraRow.BooleanExtra("", true)) }
            val alreadyExists by remember(state.name) {
                derivedStateOf { extras.any { it.name == state.name } }
            }

            EditExtraDialog(
                title = stringResource(R.string.config_intent_screen_add_extra_dialog_title),
                state = state,
                keyExists = alreadyExists,
                onChange = { state = it },
                onConfirm = {
                    onAdd(state)
                    showAddExtraDialog = false
                },
                onDismiss = { showAddExtraDialog = false }
            )
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                showAddExtraDialog = true
            }) {
            Text(stringResource(R.string.config_intent_screen_add_extra_button))
        }

        var showEditExtraDialog by rememberSaveable { mutableStateOf(false) }
        var oldExtra: IntentExtraRow? by rememberSaveable { mutableStateOf(null) }
        var newExtra: IntentExtraRow? by rememberSaveable { mutableStateOf(null) }

        if (showEditExtraDialog) {
            val alreadyExists by remember(newExtra!!.name) {
                derivedStateOf {
                    extras.any { extra ->
                        extra.name != oldExtra!!.name && extra.name == newExtra!!.name
                    }
                }
            }

            EditExtraDialog(
                title = stringResource(R.string.config_intent_screen_edit_extra_dialog_title),
                state = newExtra!!,
                keyExists = alreadyExists,
                onChange = { newExtra = it },
                onConfirm = {
                    onEdit(oldExtra!!.name, newExtra!!)
                    showEditExtraDialog = false
                },
                onDismiss = { showEditExtraDialog = false }
            )
        }

        extras.forEach { extra ->
            ExtraRow(
                modifier = Modifier.fillMaxWidth(),
                state = extra,
                onEditClick = {
                    oldExtra = extra
                    newExtra = extra
                    showEditExtraDialog = true
                },
                onRemoveClick = { onRemove(extra.name) }
            )
        }
    }
}

@Composable
private fun EditExtraDialog(
    title: String,
    state: IntentExtraRow,
    onChange: (IntentExtraRow) -> Unit,
    keyExists: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val type = IntentExtraRow.toType(state)

    val keyError = when {
        state.name.isEmpty() -> stringResource(R.string.config_intent_screen_empty_extra_key_error)
        keyExists -> stringResource(R.string.config_intent_screen_extra_exists_error)
        else -> null
    }

    val valueError: String? = getExtraValueError(state)
    val isValid: Boolean = keyError == null && valueError == null

    EditExtraDialog(
        title = title,
        name = state.name,
        nameError = keyError,
        type = type,
        isConfirmButtonEnabled = isValid,
        value = {
            when (state) {
                is IntentExtraRow.BooleanExtra -> {
                    EditBooleanExtraValue(
                        value = state.value,
                        onChange = {
                            onChange(state.copy(value = it))
                        }
                    )
                }
                is IntentExtraRow.StringExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        onChange = {
                            onChange(state.copy(value = it))
                        }
                    )
                }
                is IntentExtraRow.BooleanArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_boolean_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.ByteArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_byte_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.ByteExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        onChange = { onChange(state.copy(value = it)) },
                        keyboardType = KeyboardType.Number)
                }
                is IntentExtraRow.CharArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_char_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.CharExtra -> {
                    EditCharExtraValue(char = state.value, onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.DoubleArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_double_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.DoubleExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        onChange = { onChange(state.copy(value = it)) },
                        keyboardType = KeyboardType.Number)
                }
                is IntentExtraRow.FloatArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_float_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.FloatExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        onChange = { onChange(state.copy(value = it)) },
                        keyboardType = KeyboardType.Number)
                }
                is IntentExtraRow.IntegerArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_integer_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.IntegerExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        onChange = { onChange(state.copy(value = it)) },
                        keyboardType = KeyboardType.Number)
                }
                is IntentExtraRow.LongArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_long_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.LongExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        onChange = { onChange(state.copy(value = it)) },
                        keyboardType = KeyboardType.Number)
                }
                is IntentExtraRow.ShortArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_short_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
                is IntentExtraRow.ShortExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        onChange = { onChange(state.copy(value = it)) },
                        keyboardType = KeyboardType.Number)
                }
                is IntentExtraRow.StringArrayExtra -> {
                    EditExtraTextValue(
                        value = state.value,
                        error = valueError,
                        caption = stringResource(R.string.config_intent_screen_extra_string_array_help),
                        onChange = { onChange(state.copy(value = it)) })
                }
            }
        },
        onNameChange = { name -> onChange(setIntentExtraName(state, name)) },
        onSelectType = { type -> onChange(IntentExtraRow.fromType(type, state.name)) },
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditExtraDialog(
    title: String,
    name: String,
    nameError: String?,
    isConfirmButtonEnabled: Boolean,
    type: IntentExtraType2,
    value: @Composable ColumnScope.() -> Unit,
    onNameChange: (String) -> Unit,
    onSelectType: (IntentExtraType2) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    CustomDialog(
        title = title,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = isConfirmButtonEnabled) {
                Text(stringResource(R.string.pos_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            var expanded by remember { mutableStateOf(false) }
            val focusManager = LocalFocusManager.current

            ErrorOutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                errorMessage = nameError ?: "",
                isError = nameError != null,
                label = stringResource(R.string.config_intent_screen_extra_name_label),
                onValueChange = onNameChange,
                keyboardActions = KeyboardActions {
                    focusManager.clearFocus()
                })

            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }) {

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = stringResource(getIntentExtraTypeTitle(type)),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        focusManager.clearFocus()
                    }) {
                    IntentExtraType2.values().forEach { extraType ->
                        DropdownMenuItem(
                            text = { Text(stringResource(getIntentExtraTypeTitle(extraType))) },
                            onClick = {
                                expanded = false
                                onSelectType(extraType)
                            })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            value()
        }
    }
}

@Composable
private fun EditBooleanExtraValue(modifier: Modifier = Modifier, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier) {
        RadioButtonWithText(
            isSelected = value,
            text = stringResource(R.string.config_intent_screen_extra_boolean_true),
            onClick = { onChange(true) })
        RadioButtonWithText(
            isSelected = !value,
            text = stringResource(R.string.config_intent_screen_extra_boolean_false),
            onClick = { onChange(false) })
    }
}

@Composable
private fun EditExtraTextValue(modifier: Modifier = Modifier,
                               value: String,
                               error: String?,
                               caption: String = "",
                               onChange: (String) -> Unit,
                               keyboardType: KeyboardType = KeyboardType.Text) {
    val focusManager = LocalFocusManager.current

    Column {
        ErrorOutlinedTextField(
            modifier = modifier,
            value = value,
            onValueChange = onChange,
            label = stringResource(R.string.config_intent_screen_extra_value_label),
            errorMessage = error ?: "",
            isError = error != null,
            keyboardActions = KeyboardActions {
                focusManager.clearFocus()
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCharExtraValue(modifier: Modifier = Modifier,
                               char: Char?,
                               onChange: (Char?) -> Unit) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        modifier = modifier,
        value = char?.toString() ?: "",
        onValueChange = { value ->
            when {
                value.isEmpty() -> onChange(null)
                value.length == 1 -> onChange(value[0])
            }
        },
        label = { Text(stringResource(R.string.config_intent_screen_extra_value_label)) },
        keyboardActions = KeyboardActions {
            focusManager.clearFocus()
        }
    )
}

@Preview
@Composable
private fun EditExtraDialogPreview() {
    MaterialTheme {
        EditExtraDialog(
            title = stringResource(R.string.config_intent_screen_add_extra_dialog_title),
            name = "io.github.sds100.keymapper.KEY",
            nameError = stringResource(R.string.config_intent_screen_empty_extra_key_error),
            isConfirmButtonEnabled = true,
            value = {
                EditBooleanExtraValue(value = true, onChange = {})
            },
            type = IntentExtraType2.BOOLEAN,
            onConfirm = {},
            onDismiss = {},
            onNameChange = {},
            onSelectType = {}
        )
    }
}

@Composable
private fun ExtraRow(
    modifier: Modifier = Modifier,
    state: IntentExtraRow,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    OutlinedCard(modifier) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Column(Modifier.weight(1f)) {
                val header = stringResource(getIntentExtraTypeTitle(state))

                Text(header, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(modifier = Modifier.fillMaxWidth(), text = state.name)
                Spacer(Modifier.height(8.dp))
                Text(state.valueString)
            }

            Spacer(Modifier.width(8.dp))

            Column(Modifier.wrapContentWidth()) {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.config_intent_screen_edit_extra_content_description))
                }
                IconButton(onClick = onRemoveClick) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.config_intent_screen_delete_extra_content_description))
                }
            }
        }
    }
}

@Preview
@Composable
private fun ExtraRowPreview() {
    MaterialTheme {
        Surface {
            ExtraRow(
                state = IntentExtraRow.BooleanExtra(
                    name = "io.github.sds100.keymapper.KEY",
                    value = true,
                ),
                onEditClick = {},
                onRemoveClick = {}
            )
        }
    }
}

@Composable
private fun getExtraValueError(extraRow: IntentExtraRow): String? {
    val formattedIncorrectlyMessage = when (extraRow) {
        is IntentExtraRow.BooleanArrayExtra,
        is IntentExtraRow.ByteArrayExtra,
        is IntentExtraRow.CharArrayExtra,
        is IntentExtraRow.DoubleArrayExtra,
        is IntentExtraRow.FloatArrayExtra,
        is IntentExtraRow.IntegerArrayExtra,
        is IntentExtraRow.LongArrayExtra,
        is IntentExtraRow.ShortArrayExtra ->
            stringResource(R.string.config_intent_screen_extra_boolean_array_incorrect_format_error)

        is IntentExtraRow.ByteExtra ->
            stringResource(R.string.config_intent_screen_extra_byte_incorrect_format_error)
        is IntentExtraRow.DoubleExtra ->
            stringResource(R.string.config_intent_screen_extra_double_incorrect_format_error)
        is IntentExtraRow.FloatExtra ->
            stringResource(R.string.config_intent_screen_extra_float_incorrect_format_error)
        is IntentExtraRow.IntegerExtra ->
            stringResource(R.string.config_intent_screen_extra_integer_incorrect_format_error)
        is IntentExtraRow.LongExtra ->
            stringResource(R.string.config_intent_screen_extra_long_incorrect_format_error)
        is IntentExtraRow.ShortExtra ->
            stringResource(R.string.config_intent_screen_extra_short_incorrect_format_error)

        is IntentExtraRow.BooleanExtra,
        is IntentExtraRow.CharExtra,
        is IntentExtraRow.StringExtra,
        is IntentExtraRow.StringArrayExtra -> null
    }

    if (!extraRow.isFormattedCorrectly) {
        return formattedIncorrectlyMessage
    } else {
        return null
    }
}

@StringRes
private fun getIntentExtraTypeTitle(type: IntentExtraType2): Int {
    return when (type) {
        IntentExtraType2.BOOLEAN -> R.string.config_intent_screen_extra_boolean_title
        IntentExtraType2.BOOLEAN_ARRAY -> R.string.config_intent_screen_extra_boolean_array_title
        IntentExtraType2.INTEGER -> R.string.config_intent_screen_extra_integer_title
        IntentExtraType2.INTEGER_ARRAY -> R.string.config_intent_screen_extra_integer_array_title
        IntentExtraType2.STRING -> R.string.config_intent_screen_extra_string_title
        IntentExtraType2.STRING_ARRAY -> R.string.config_intent_screen_extra_string_array_title
        IntentExtraType2.LONG -> R.string.config_intent_screen_extra_long_title
        IntentExtraType2.LONG_ARRAY -> R.string.config_intent_screen_extra_long_array_title
        IntentExtraType2.BYTE -> R.string.config_intent_screen_extra_byte_title
        IntentExtraType2.BYTE_ARRAY -> R.string.config_intent_screen_extra_byte_array_title
        IntentExtraType2.DOUBLE -> R.string.config_intent_screen_extra_double_title
        IntentExtraType2.DOUBLE_ARRAY -> R.string.config_intent_screen_extra_double_array_title
        IntentExtraType2.CHAR -> R.string.config_intent_screen_extra_char_title
        IntentExtraType2.CHAR_ARRAY -> R.string.config_intent_screen_extra_char_array_title
        IntentExtraType2.FLOAT -> R.string.config_intent_screen_extra_float_title
        IntentExtraType2.FLOAT_ARRAY -> R.string.config_intent_screen_extra_float_array_title
        IntentExtraType2.SHORT -> R.string.config_intent_screen_extra_short_title
        IntentExtraType2.SHORT_ARRAY -> R.string.config_intent_screen_extra_short_array_title
    }
}

@StringRes
private fun getIntentExtraTypeTitle(extra: IntentExtraRow): Int {
    return when (extra) {
        is IntentExtraRow.BooleanExtra -> R.string.config_intent_screen_extra_boolean_title
        is IntentExtraRow.StringExtra -> R.string.config_intent_screen_extra_string_title
        is IntentExtraRow.BooleanArrayExtra -> R.string.config_intent_screen_extra_boolean_array_title
        is IntentExtraRow.ByteArrayExtra -> R.string.config_intent_screen_extra_byte_array_title
        is IntentExtraRow.ByteExtra -> R.string.config_intent_screen_extra_byte_title
        is IntentExtraRow.CharArrayExtra -> R.string.config_intent_screen_extra_char_array_title
        is IntentExtraRow.CharExtra -> R.string.config_intent_screen_extra_char_title
        is IntentExtraRow.DoubleArrayExtra -> R.string.config_intent_screen_extra_double_array_title
        is IntentExtraRow.DoubleExtra -> R.string.config_intent_screen_extra_double_title
        is IntentExtraRow.FloatArrayExtra -> R.string.config_intent_screen_extra_float_array_title
        is IntentExtraRow.FloatExtra -> R.string.config_intent_screen_extra_float_title
        is IntentExtraRow.IntegerArrayExtra -> R.string.config_intent_screen_extra_integer_array_title
        is IntentExtraRow.IntegerExtra -> R.string.config_intent_screen_extra_integer_title
        is IntentExtraRow.LongArrayExtra -> R.string.config_intent_screen_extra_long_array_title
        is IntentExtraRow.LongExtra -> R.string.config_intent_screen_extra_long_title
        is IntentExtraRow.ShortArrayExtra -> R.string.config_intent_screen_extra_short_array_title
        is IntentExtraRow.ShortExtra -> R.string.config_intent_screen_extra_short_title
        is IntentExtraRow.StringArrayExtra -> R.string.config_intent_screen_extra_string_array_title
    }
}

private fun setIntentExtraName(extra: IntentExtraRow, name: String): IntentExtraRow {
    return when (extra) {
        is IntentExtraRow.BooleanExtra -> extra.copy(name = name)
        is IntentExtraRow.StringExtra -> extra.copy(name = name)
        is IntentExtraRow.BooleanArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.ByteArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.ByteExtra -> extra.copy(name = name)
        is IntentExtraRow.CharArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.CharExtra -> extra.copy(name = name)
        is IntentExtraRow.DoubleArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.DoubleExtra -> extra.copy(name = name)
        is IntentExtraRow.FloatArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.FloatExtra -> extra.copy(name = name)
        is IntentExtraRow.IntegerArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.IntegerExtra -> extra.copy(name = name)
        is IntentExtraRow.LongArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.LongExtra -> extra.copy(name = name)
        is IntentExtraRow.ShortArrayExtra -> extra.copy(name = name)
        is IntentExtraRow.ShortExtra -> extra.copy(name = name)
        is IntentExtraRow.StringArrayExtra -> extra.copy(name = name)
    }
}

@Composable
private fun CategoryRow(
    modifier: Modifier = Modifier,
    text: String,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    OutlinedCard(modifier) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(modifier = Modifier.weight(1f), text = text)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEditClick) {
                Icon(Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.config_intent_screen_edit_category_content_description))
            }
            IconButton(onClick = onRemoveClick) {
                Icon(Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.config_intent_screen_delete_category_content_description))
            }
        }
    }
}

@Preview
@Composable
private fun CategoryRowPreview() {
    MaterialTheme {
        Surface {
            CategoryRow(
                text = "CATEGORY_DEFAULT",
                onEditClick = {},
                onRemoveClick = {})
        }
    }
}