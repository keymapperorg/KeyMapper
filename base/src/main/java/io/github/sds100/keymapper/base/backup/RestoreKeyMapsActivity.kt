package io.github.sds100.keymapper.base.backup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.MainActivity
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CustomDialogContent

class RestoreKeyMapsActivity : ComponentActivity() {

    private val viewModel by viewModels<RestoreKeyMapsViewModel> {
        RestoreKeyMapsViewModel.Factory(
            useCase = BackupRestoreMappingsUseCaseImpl(
                fileAdapter = ServiceLocator.fileAdapter(this),
                backupManager = ServiceLocator.backupManager(this),
            ),
            resourceProvider = ServiceLocator.resourceProvider(this),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme_DialogActivity)

        setFinishOnTouchOutside(true)

        importKeyMaps(intent)
        intent = null

        setContent {
            KeyMapperTheme {
                val state by viewModel.importExportState.collectAsStateWithLifecycle()

                val title = when (val state = state) {
                    ImportExportState.Idle -> stringResource(R.string.import_dialog_title_loading)
                    is ImportExportState.ConfirmImport -> pluralStringResource(
                        R.plurals.home_importing_dialog_title,
                        state.keyMapCount,
                        state.keyMapCount,
                    )

                    is ImportExportState.Error -> stringResource(R.string.import_dialog_title_error)
                    ImportExportState.FinishedImport -> stringResource(R.string.import_dialog_title_success)
                    ImportExportState.Importing -> stringResource(R.string.import_dialog_title_importing)
                    else -> ""
                }

                val text = when (val state = state) {
                    is ImportExportState.ConfirmImport -> stringResource(R.string.home_importing_dialog_text)
                    else -> null
                }

                val ctx = LocalContext.current

                CustomDialogContent(
                    title = title,
                    text = text,
                    confirmButton = {
                        if (state is ImportExportState.FinishedImport) {
                            TextButton(onClick = {
                                finish()

                                Intent(ctx, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(this)
                                }
                            }) {
                                Text(stringResource(R.string.import_dialog_button_launch_key_mapper))
                            }
                        } else if (state !is ImportExportState.Idle && state !is ImportExportState.ConfirmImport) {
                            TextButton(onClick = { finish() }) {
                                Text(stringResource(R.string.pos_done))
                            }
                        } else {
                            TextButton(
                                onClick = { viewModel.onConfirmImport(RestoreType.APPEND) },
                                enabled = state is ImportExportState.ConfirmImport,
                            ) {
                                Text(stringResource(R.string.home_importing_dialog_append))
                            }
                        }
                    },
                    dismissButton = {
                        if (state is ImportExportState.FinishedImport) {
                            TextButton(onClick = { finish() }) {
                                Text(stringResource(R.string.home_importing_dialog_dismiss))
                            }
                        } else if (state is ImportExportState.Idle || state is ImportExportState.ConfirmImport) {
                            TextButton(onClick = { finish() }) {
                                Text(stringResource(R.string.home_importing_dialog_cancel))
                            }

                            Spacer(Modifier.width(16.dp))

                            TextButton(
                                onClick = { viewModel.onConfirmImport(RestoreType.REPLACE) },
                                enabled = state is ImportExportState.ConfirmImport,
                            ) {
                                Text(stringResource(R.string.home_importing_dialog_replace))
                            }
                        }
                    },
                ) { }
            }
        }
    }

    private fun importKeyMaps(intent: Intent?) {
        intent ?: return

        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let {
                viewModel.onChooseImportFile(it.toString())

                // Do not want to import again on a configuration change so set it to null
                this.intent = null
            }
        }
    }
}
