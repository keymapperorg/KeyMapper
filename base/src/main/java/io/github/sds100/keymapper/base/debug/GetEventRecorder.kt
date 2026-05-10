package io.github.sds100.keymapper.base.debug

import android.content.Context
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.base.actions.ExecuteShellCommandUseCase
import io.github.sds100.keymapper.base.utils.ShareUtils
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.utils.handle
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.clipboard.ClipboardAdapter
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.files.IFile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface GetEventRecorder {
    val isRecording: Flow<Boolean>
    val deviceInfoOutput: Flow<String>
    val eventsOutput: Flow<String>

    suspend fun refreshDeviceInfo()
    fun recordEvents()
    fun stopRecording()
    fun copyOutput(output: String)
    suspend fun shareOutput(output: String)
}

@Singleton
class GetEventRecorderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val executeShellCommandUseCase: ExecuteShellCommandUseCase,
    private val preferenceRepository: PreferenceRepository,
    private val clipboardAdapter: ClipboardAdapter,
    private val fileAdapter: FileAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    private val resourceProvider: ResourceProvider,
) : GetEventRecorder {

    companion object {
        private const val MAX_COPY_OUTPUT_LENGTH = 150_000
    }

    private val recordingJobState: MutableStateFlow<Job?> = MutableStateFlow(null)

    override val isRecording: Flow<Boolean> = recordingJobState.map { it != null && it.isActive }

    override val deviceInfoOutput: Flow<String> = preferenceRepository
        .get(Keys.getEventDeviceInfoOutput)
        .map { it.orEmpty() }

    override val eventsOutput: Flow<String> = preferenceRepository
        .get(Keys.getEventEventsOutput)
        .map { it.orEmpty() }

    override suspend fun refreshDeviceInfo() {
        val output = executeShellCommandUseCase.execute(
            command = "getevent -il",
            executionMode = ShellExecutionMode.ADB,
            timeoutMillis = 5_000L,
        ).handle(
            onSuccess = { it.stdout },
            onError = { "Error: ${it.getFullMessage(resourceProvider)}" },
        )
        preferenceRepository.set(Keys.getEventDeviceInfoOutput, output)
    }

    override fun recordEvents() {
        recordingJobState.update { oldJob ->
            oldJob?.cancel()

            coroutineScope.launch {
                val output = executeShellCommandUseCase.execute(
                    command = "getevent -lt",
                    executionMode = ShellExecutionMode.ADB,
                    timeoutMillis = 60_000L,
                ).handle(
                    onSuccess = { it.stdout },
                    onError = { "" },
                )

                if (output.isNotEmpty()) {
                    preferenceRepository.set(Keys.getEventEventsOutput, output)
                }
            }
        }
    }

    override fun stopRecording() {
        coroutineScope.launch {
            executeShellCommandUseCase.execute(
                command = "pkill -x getevent || true",
                executionMode = ShellExecutionMode.ADB,
                timeoutMillis = 5_000L,
            )

            recordingJobState.update { oldJob ->
                oldJob?.join()
                oldJob?.cancel()
                null
            }
        }
    }

    override fun copyOutput(output: String) {
        clipboardAdapter.copy(
            "getevent output",
            output.takeLast(MAX_COPY_OUTPUT_LENGTH),
        )
    }

    override suspend fun shareOutput(output: String) {
        withContext(Dispatchers.IO) {
            val fileName = "getevent/key_mapper_getevent_${FileUtils.createFileDate()}.txt"
            val file: IFile = fileAdapter.getPrivateFile(fileName)
            file.createFile()
            file.outputStream()?.bufferedWriter()?.use { it.write(output) }

            val publicUri = fileAdapter.getPublicUriForPrivateFile(file).toUri()
            ShareUtils.shareFile(context, publicUri, buildConfigProvider.packageName)
        }
    }
}
