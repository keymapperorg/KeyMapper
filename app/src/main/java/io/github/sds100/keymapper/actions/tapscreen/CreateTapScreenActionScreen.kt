package io.github.sds100.keymapper.actions.tapscreen

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.decodeBitmap
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.util.ui.ErrorTextField

/**
 * Created by sds100 on 31/07/2022.
 */

@Destination
@Composable
fun CreateTapScreenActionScreen(
    viewModel: CreateTapScreenActionViewModel,
    resultBackNavigator: ResultBackNavigator<PickCoordinateResult>
) {
    val contentResolver = LocalContext.current.contentResolver

    val chooseScreenshotLauncher: ManagedActivityResultLauncher<String, Uri?> =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.createSource(contentResolver, uri).decodeBitmap { _, _ -> }
            } else {
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }

            viewModel.onSelectScreenshot(bitmap)
        }

    CreateTapScreenActionScreen(
        modifier = Modifier.fillMaxSize(),
        state = viewModel.state,
        onBackClick = resultBackNavigator::navigateBack,
        onTapScreenshot = viewModel::onTouchScreenshot,
        onChooseScreenshotClick = {
            chooseScreenshotLauncher.launch(FileUtils.MIME_TYPE_IMAGES)
        },
        onXTextChange = viewModel::onXTextChange,
        onYTextChange = viewModel::onYTextChange,
        onDismissIncorrectResolutionSnackbar = viewModel::onDismissIncorrectResolutionSnackBar,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDoneClick = {
            val result = viewModel.createResult()
            resultBackNavigator.navigateBack(result)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTapScreenActionScreen(
    modifier: Modifier = Modifier,
    state: CreateTapScreenActionState,
    onBackClick: () -> Unit = {},
    onTapScreenshot: (Offset, IntSize) -> Unit = { _, _ -> },
    onChooseScreenshotClick: () -> Unit = {},
    onXTextChange: (String) -> Unit = {},
    onYTextChange: (String) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onDismissIncorrectResolutionSnackbar: () -> Unit = {},
    onDoneClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

    if (state.showIncorrectResolutionSnackBar) {
        val message = stringResource(R.string.create_tap_screen_action_incorrect_resolution_error)
        LaunchedEffect(state.showIncorrectResolutionSnackBar) {
            snackbarHostState.showSnackbar(message)
            onDismissIncorrectResolutionSnackbar()
        }
    }

    Scaffold(
        modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.choose_action_back_content_description)
                        )
                    }
                },
                floatingActionButton = {
                    AnimatedVisibility(state.isDoneButtonEnabled, enter = fadeIn(), exit = fadeOut()) {
                        FloatingActionButton(
                            onClick = onDoneClick,
                            elevation = BottomAppBarDefaults.BottomAppBarFabElevation
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = stringResource(R.string.create_tap_screen_action_done_content_description)
                            )
                        }
                    }
                }
            )
        }) { padding ->

        Content(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            state = state,
            onTapScreenshot = onTapScreenshot,
            onChooseScreenshotClick = onChooseScreenshotClick,
            onXTextChange = onXTextChange,
            onYTextChange = onYTextChange,
            onDescriptionChange = onDescriptionChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Content(
    modifier: Modifier,
    state: CreateTapScreenActionState,
    onTapScreenshot: (Offset, IntSize) -> Unit,
    onChooseScreenshotClick: () -> Unit,
    onDescriptionChange: (String) -> Unit,
    onXTextChange: (String) -> Unit,
    onYTextChange: (String) -> Unit,
) {
    Column(modifier.padding(16.dp)) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.create_tap_screen_action_description_label)) }
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            val focusManager = LocalFocusManager.current
            ErrorTextField(
                modifier = Modifier.weight(0.5f),
                value = state.x,
                onValueChange = onXTextChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }),
                errorMessage = getCoordinateErrorMessageText(state.xError),
                isError = state.xError != CoordinateError.NONE,
                label = stringResource(R.string.create_tap_screen_action_x_label)
            )
            Spacer(Modifier.width(16.dp))
            ErrorTextField(
                modifier = Modifier.weight(0.5f),
                value = state.y,
                onValueChange = onYTextChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    }), errorMessage = getCoordinateErrorMessageText(state.yError),
                isError = state.yError != CoordinateError.NONE,
                label = stringResource(R.string.create_tap_screen_action_y_label)
            )
        }

        Spacer(Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onChooseScreenshotClick
        ) {
            Text(stringResource(R.string.button_pick_coordinate_select_screenshot))
        }

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.create_tap_screen_action_explanation),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(16.dp))

        if (state.screenshot != null) {
            ScreenshotCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(start = 16.dp, end = 16.dp)
                    .align(Alignment.CenterHorizontally),
                screenshot = state.screenshot,
                point = state.selectedPoint,
                onTap = onTapScreenshot
            )
        }
    }
}

@Composable
private fun getCoordinateErrorMessageText(error: CoordinateError): String {
    return when (error) {
        CoordinateError.NONE -> ""
        CoordinateError.EMPTY -> stringResource(R.string.create_tap_screen_action_empty_error)
        CoordinateError.NOT_INTEGER -> stringResource(R.string.create_tap_screen_action_not_an_integer_error)
    }
}

@Composable
private fun ScreenshotCanvas(
    modifier: Modifier,
    screenshot: ImageBitmap,
    point: Offset?,
    onTap: (Offset, IntSize) -> Unit
) {
    val lineColor = colorResource(R.color.coordinate_line)

    val customPainter = object : Painter() {
        override val intrinsicSize: Size
            get() = Size(screenshot.width.toFloat(), screenshot.height.toFloat())

        override fun DrawScope.onDraw() {
            drawImage(screenshot, dstSize = IntSize(size.width.toInt(), size.height.toInt()))

            if (point != null) {
                drawLine(
                    lineColor,
                    start = Offset(point.x, 0f),
                    end = Offset(point.x, size.height)
                )

                drawLine(
                    lineColor,
                    start = Offset(0f, point.y),
                    end = Offset(size.width, point.y)
                )
            }
        }
    }

    Image(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { offset ->
                onTap(offset, size)
            })
        },
        painter = customPainter,
        contentDescription = null
    )
}

@Preview(device = Devices.PIXEL_3)
@Composable
private fun Preview() {
    MaterialTheme {
        CreateTapScreenActionScreen(
            state = CreateTapScreenActionState(
                x = "",
                y = "456",
                xError = CoordinateError.EMPTY,
                yError = CoordinateError.NOT_INTEGER,
                screenshot = ImageBitmap.imageResource(R.drawable.ic_launcher_web),
                selectedPoint = Offset(100f, 100f),
                isDoneButtonEnabled = true,
                showIncorrectResolutionSnackBar = false,
                description = ""
            ),
        )
    }
}