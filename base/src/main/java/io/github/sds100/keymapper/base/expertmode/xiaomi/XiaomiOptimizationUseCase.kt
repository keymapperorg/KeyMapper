package io.github.sds100.keymapper.base.expertmode.xiaomi

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.ExecuteShellCommandUseCase
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.models.ShellExecutionMode
import io.github.sds100.keymapper.common.models.isSuccess
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

interface XiaomiOptimizationUseCase {
    val isMiuiOptimizationDisabled: Flow<Boolean?>
    val isBatteryFixesApplied: Flow<Boolean?>
    suspend fun applyBatteryFixes(): Boolean
    suspend fun setMiuiOptimizationDisabled(disable: Boolean): Boolean
    fun openAutostartSettings(): Boolean
    fun openBatterySaverSettings(): Boolean
}

@OptIn(ExperimentalCoroutinesApi::class)
@ViewModelScoped
class XiaomiOptimizationUseCaseImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val executeShellCommandUseCase: ExecuteShellCommandUseCase,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val buildConfigProvider: BuildConfigProvider,
) : XiaomiOptimizationUseCase {

    companion object {
        private const val COMMAND_TIMEOUT_MS = 5000L
    }

    /**
     * Emits whenever the battery fix/MIUI optimization state may have changed so the two
     * flows below re-query the device, in addition to re-querying on (re)connection.
     */
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }

    override val isMiuiOptimizationDisabled: Flow<Boolean?> =
        combine(
            systemBridgeConnectionManager.connectionState,
            refreshTrigger,
        ) { connectionState, _ -> connectionState }
            .flatMapLatest { connectionState ->
                if (connectionState is SystemBridgeConnectionState.Connected) {
                    flow { emit(queryMiuiOptimizationDisabled()) }
                } else {
                    flowOf(null)
                }
            }

    override val isBatteryFixesApplied: Flow<Boolean?> =
        combine(
            systemBridgeConnectionManager.connectionState,
            refreshTrigger,
        ) { connectionState, _ -> connectionState }
            .flatMapLatest { connectionState ->
                if (connectionState is SystemBridgeConnectionState.Connected) {
                    flow { emit(queryBatteryFixesApplied()) }
                } else {
                    flowOf(null)
                }
            }

    override suspend fun applyBatteryFixes(): Boolean {
        val packageName = buildConfigProvider.packageName

        val success = runCommand("dumpsys deviceidle whitelist +$packageName") &&
            runCommand("cmd appops set $packageName RUN_IN_BACKGROUND allow") &&
            runCommand("cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow")

        refreshTrigger.tryEmit(Unit)

        return success
    }

    override suspend fun setMiuiOptimizationDisabled(disable: Boolean): Boolean {
        val value = if (disable) {
            0
        } else {
            1
        }
        val success = runCommand("settings put global miui_optimization $value")

        refreshTrigger.tryEmit(Unit)

        return success
    }

    override fun openAutostartSettings(): Boolean = safeLaunchActivity(
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
        ),
    )

    override fun openBatterySaverSettings(): Boolean = safeLaunchActivity(
        ComponentName(
            "com.miui.powerkeeper",
            "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
        ),
    ) {
        putExtra("package_name", buildConfigProvider.packageName)
        putExtra("package_label", ctx.getString(R.string.app_name))
    }

    private suspend fun queryBatteryFixesApplied(): Boolean {
        val packageName = buildConfigProvider.packageName

        val isWhitelisted = queryOutput("dumpsys deviceidle whitelist")
            ?.contains(packageName) == true
        val isRunInBackgroundAllowed =
            queryOutput("cmd appops get $packageName RUN_IN_BACKGROUND")
                ?.contains("allow") == true
        val isRunAnyInBackgroundAllowed =
            queryOutput("cmd appops get $packageName RUN_ANY_IN_BACKGROUND")
                ?.contains("allow") == true

        return isWhitelisted && isRunInBackgroundAllowed && isRunAnyInBackgroundAllowed
    }

    private suspend fun queryMiuiOptimizationDisabled(): Boolean =
        queryOutput("settings get global miui_optimization")?.trim() == "0"

    private suspend fun runCommand(command: String): Boolean {
        val result = executeShellCommandUseCase.execute(
            command,
            ShellExecutionMode.ADB,
            COMMAND_TIMEOUT_MS,
        )

        return result is Success && result.value.isSuccess()
    }

    private suspend fun queryOutput(command: String): String? {
        val result = executeShellCommandUseCase.execute(
            command,
            ShellExecutionMode.ADB,
            COMMAND_TIMEOUT_MS,
        )

        return if (result is Success && result.value.isSuccess()) {
            result.value.stdout
        } else {
            null
        }
    }

    /**
     * Some Xiaomi ROMs remove or rename these activities, so fall back to the app's own
     * details settings screen if the explicit component can't be launched.
     */
    private fun safeLaunchActivity(
        component: ComponentName,
        putExtras: Intent.() -> Unit = {},
    ): Boolean {
        val intent = Intent().apply {
            this.component = component
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtras()
        }

        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            openAppDetailsSettings()
        } catch (e: SecurityException) {
            openAppDetailsSettings()
        }
    }

    private fun openAppDetailsSettings(): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${buildConfigProvider.packageName}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            ctx.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
