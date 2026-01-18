package io.github.sds100.keymapper.base.system.inputmethod

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.SettingsUtils
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.otherwise
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

/**
 * This implementation of SwitchImeInterface communicates asynchronously with the accessibility
 * service. This is needed in cases where the accessibility service instance is not accessible
 * to the caller. A synchronous version is implemented in BaseAccessibilityService.
 */
@Singleton
class SwitchImeAsyncImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val serviceAdapter: AccessibilityServiceAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    private val permissionAdapter: PermissionAdapter,
    private val suAdapter: SuAdapter,
) : SwitchImeInterface {

    override fun enableIme(imeId: String): KMResult<Unit> {
        return enableImeWithoutUserInput(imeId).otherwise {
            try {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_NEW_TASK

                ctx.startActivity(intent)
                Success(Unit)
            } catch (_: Exception) {
                KMError.CantFindImeSettings
            }
        }
    }

    private fun enableImeWithoutUserInput(imeId: String): KMResult<Unit> {
        return inputMethodAdapter.getInfoById(imeId)
            .then { imeInfo ->
                // The accessibility service can only enable IMEs that have the same
                // package name as the accessibility service.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    imeInfo.packageName == buildConfigProvider.packageName
                ) {
                    serviceAdapter.sendAsync(
                        AccessibilityServiceEvent.EnableInputMethod(
                            imeInfo.id,
                        ),
                    )
                } else {
                    runBlocking { suAdapter.execute("ime enable $imeId").then { Success(Unit) } }
                }
            }
    }

    override fun switchIme(imeId: String): KMResult<Unit> {
        inputMethodAdapter.getInfoById(imeId).onSuccess {
            if (!it.isEnabled) {
                return SystemError.ImeDisabled(it)
            }
        }.onFailure {
            return it
        }

        // First try using the accessibility service, and if that fails then
        // try WRITE_SECURE_SETTINGS if possible. Otherwise return the accessibility service
        // error.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return serviceAdapter.sendAsync(AccessibilityServiceEvent.ChangeIme(imeId))
                .otherwise { error ->
                    if (permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
                        SettingsUtils.putSecureSetting(
                            ctx,
                            Settings.Secure.DEFAULT_INPUT_METHOD,
                            imeId,
                        )
                        Success(Unit)
                    } else {
                        error
                    }
                }
        }

        if (permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS)) {
            SettingsUtils.putSecureSetting(
                ctx,
                Settings.Secure.DEFAULT_INPUT_METHOD,
                imeId,
            )

            return Success(Unit)
        }

        return KMError.SwitchImeFailed
    }
}
